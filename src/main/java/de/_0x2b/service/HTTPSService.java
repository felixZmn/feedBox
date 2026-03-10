package de._0x2b.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class HTTPSService {
    private static final Logger logger = LoggerFactory.getLogger(HTTPSService.class);
    private final String userAgent;
    private final int timeout;

    HttpClient client;

    @Inject
    public HTTPSService(
            @ConfigProperty(name = "app.http.user-agent", defaultValue = "FeedBox/1.0.0") String userAgent,
            @ConfigProperty(name = "app.http.timeout", defaultValue = "10") int timeout) {
        this.userAgent = userAgent;
        this.timeout = timeout;
        client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(timeout)).build();
    }

    /**
     * Helper to perform http requests. Takes care about errors and logging
     *
     * @param uri URI to fetch
     * @return Response object or null if request is not successful
     */
    public Optional<HttpResponse<byte[]>> fetchAsBytes(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(timeout))
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                logger.error("Cannot fetch {}; status code {}", uri, response.statusCode());
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (IOException e) {
            logger.error("Could not fetch URI: {}", uri, e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Could not fetch URI: {}", uri, e);
            return Optional.empty();
        }
    }

    /**
     * Helper to perform http requests. Takes care about errors and logging
     *
     * @param uri URI to fetch
     * @return Response object or null if request is not successful
     */
    public Optional<HttpResponse<InputStream>> fetchUriAsStream(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(timeout))
                .build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                logger.error("Cannot fetch {}; status code {}", uri, response.statusCode());
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (IOException e) {
            logger.error("Could not fetch URI: {}", uri, e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Could not fetch URI: {}", uri, e);
            return Optional.empty();
        }
    }
}
