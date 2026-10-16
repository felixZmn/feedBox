package de._0x2b.http;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single outbound HTTP stack for feed discovery, refresh, and favicon fetch.
 * Enforces SSRF checks, DNS pinning (no rebinding TOCTOU), no protocol
 * downgrade on redirects, and response size caps.
 */
@ApplicationScoped
public class OutboundHttpClient {
    private static final Logger logger = LoggerFactory.getLogger(OutboundHttpClient.class);
    private static final Set<Integer> REDIRECT_CODES = Set.of(301, 302, 303, 307, 308);
    /** Cap for draining redirect / error bodies (avoid OOM on huge Location hops). */
    private static final long DRAIN_MAX_BYTES = 64 * 1024;

    private final String userAgent;
    private final int timeoutSeconds;
    private final int maxRedirects;
    private final long feedMaxBytes;
    private final long iconMaxBytes;
    private final Set<Integer> allowedPorts;
    final HttpClient client;

    @Inject
    public OutboundHttpClient(
            @ConfigProperty(name = "app.http.user-agent", defaultValue = "FeedBox/${quarkus.application.version}") String userAgent,
            @ConfigProperty(name = "app.http.timeout", defaultValue = "30") int timeoutSeconds,
            @ConfigProperty(name = "app.http.max-redirects", defaultValue = "5") int maxRedirects,
            @ConfigProperty(name = "app.http.max-bytes.feed", defaultValue = "10485760") long feedMaxBytes,
            @ConfigProperty(name = "app.http.max-bytes.icon", defaultValue = "2097152") long iconMaxBytes,
            @ConfigProperty(name = "app.http.allowed-ports", defaultValue = "80,443") String allowedPortsCsv) {
        this(userAgent, timeoutSeconds, maxRedirects, feedMaxBytes, iconMaxBytes, parsePorts(allowedPortsCsv),
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                        .build());
    }

    /** Package-private for tests that inject a mock {@link HttpClient}. */
    OutboundHttpClient(String userAgent, int timeoutSeconds, int maxRedirects, long feedMaxBytes,
            long iconMaxBytes, HttpClient client) {
        this(userAgent, timeoutSeconds, maxRedirects, feedMaxBytes, iconMaxBytes,
                UrlSafety.DEFAULT_ALLOWED_PORTS, client);
    }

    OutboundHttpClient(String userAgent, int timeoutSeconds, int maxRedirects, long feedMaxBytes,
            long iconMaxBytes, Set<Integer> allowedPorts, HttpClient client) {
        this.userAgent = userAgent;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRedirects = maxRedirects;
        this.feedMaxBytes = feedMaxBytes;
        this.iconMaxBytes = iconMaxBytes;
        this.allowedPorts = Set.copyOf(allowedPorts);
        this.client = client;
    }

    static Set<Integer> parsePorts(String csv) {
        if (csv == null || csv.isBlank()) {
            return UrlSafety.DEFAULT_ALLOWED_PORTS;
        }
        Set<Integer> ports = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toCollection(HashSet::new));
        return ports.isEmpty() ? UrlSafety.DEFAULT_ALLOWED_PORTS : Set.copyOf(ports);
    }

    public Optional<FetchResult> fetchFeed(URI uri) {
        return fetch(uri, feedMaxBytes);
    }

    public Optional<FetchResult> fetchIcon(URI uri) {
        return fetch(uri, iconMaxBytes);
    }

    /**
     * GET {@code uri} with SSRF validation, DNS pinning, manual redirect
     * following, and a hard body size limit. Returns empty on network errors,
     * non-2xx finals, oversized bodies, or unsafe URLs/redirects.
     */
    public Optional<FetchResult> fetch(URI uri, long maxBytes) {
        URI current;
        try {
            current = uri.toURL().toURI(); // normalise
        } catch (Exception e) {
            logger.warn("Malformed outbound URL: {}", uri);
            return Optional.empty();
        }

        try {
            for (int hop = 0; hop <= maxRedirects; hop++) {
                List<InetAddress> addresses = UrlSafety.resolveValidated(current, allowedPorts);
                String host = current.getHost();
                PinnedDns.pin(host, addresses);
                try {
                    HttpRequest request = HttpRequest.newBuilder(current)
                            .GET()
                            .header("User-Agent", userAgent)
                            .timeout(Duration.ofSeconds(timeoutSeconds))
                            .build();

                    HttpResponse<InputStream> response = client.send(request,
                            HttpResponse.BodyHandlers.ofInputStream());
                    int status = response.statusCode();

                    if (REDIRECT_CODES.contains(status)) {
                        Optional<String> location = response.headers().firstValue("location");
                        drainQuietly(response.body(), DRAIN_MAX_BYTES);
                        if (location.isEmpty() || location.get().isBlank()) {
                            logger.warn("Redirect without Location from {}", current);
                            return Optional.empty();
                        }
                        URI next = current.resolve(location.get().trim());
                        if (isHttpsToHttpDowngrade(current, next)) {
                            logger.warn("Refusing https→http redirect: {} → {}", current, next);
                            return Optional.empty();
                        }
                        current = next;
                        continue;
                    }

                    if (status < 200 || status >= 300) {
                        drainQuietly(response.body(), DRAIN_MAX_BYTES);
                        logger.warn("Cannot fetch {}; status code {}", current, status);
                        return Optional.empty();
                    }

                    byte[] body = readLimited(response.body(), maxBytes);
                    String contentType = response.headers().firstValue("content-type").orElse(null);
                    return Optional.of(new FetchResult(uri, current, status, contentType, body));
                } finally {
                    PinnedDns.unpin(host);
                }
            }
            logger.warn("Too many redirects fetching {}", uri);
            return Optional.empty();
        } catch (UnsafeUrlException e) {
            logger.warn("Blocked unsafe outbound URL {}: {}", uri, e.getMessage());
            return Optional.empty();
        } catch (ResponseTooLargeException e) {
            logger.warn("Response too large (limit {} bytes) for {}", maxBytes, uri);
            return Optional.empty();
        } catch (IOException e) {
            logger.debug("Could not fetch URI {}: {}", uri, e.toString());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted fetching URI: {}", uri);
            return Optional.empty();
        } finally {
            PinnedDns.clear();
        }
    }

    public long feedMaxBytes() {
        return feedMaxBytes;
    }

    public long iconMaxBytes() {
        return iconMaxBytes;
    }

    public String userAgent() {
        return userAgent;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public Set<Integer> allowedPorts() {
        return allowedPorts;
    }

    private static boolean isHttpsToHttpDowngrade(URI from, URI to) {
        String fromScheme = from.getScheme() != null ? from.getScheme().toLowerCase(Locale.ROOT) : "";
        String toScheme = to.getScheme() != null ? to.getScheme().toLowerCase(Locale.ROOT) : "";
        return "https".equals(fromScheme) && "http".equals(toScheme);
    }

    private static byte[] readLimited(InputStream in, long maxBytes) throws IOException {
        try (InputStream stream = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = stream.read(buf)) >= 0) {
                total += n;
                if (total > maxBytes) {
                    throw new ResponseTooLargeException(maxBytes);
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    /** Best-effort drain of redirect / error bodies with a hard size cap. */
    static void drainQuietly(InputStream in, long maxBytes) {
        if (in == null) {
            return;
        }
        try (InputStream stream = in) {
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = stream.read(buf)) >= 0) {
                total += n;
                if (total > maxBytes) {
                    break;
                }
            }
        } catch (IOException ignored) {
            // best-effort
        }
    }

    static final class ResponseTooLargeException extends IOException {
        ResponseTooLargeException(long maxBytes) {
            super("Response exceeds " + maxBytes + " bytes");
        }
    }
}
