package de._0x2b.http;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * SSRF guard for server-initiated HTTP(S) fetches. Validates scheme, port, and
 * resolved destination addresses before connect and on every redirect hop.
 */
public final class UrlSafety {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<Integer> ALLOWED_PORTS = Set.of(80, 443);

    private UrlSafety() {
    }

    /**
     * @throws UnsafeUrlException when the URI must not be fetched by the server
     */
    public static void requireSafeOutbound(URI uri) {
        if (uri == null) {
            throw new UnsafeUrlException("URL is required");
        }
        if (!uri.isAbsolute()) {
            throw new UnsafeUrlException("URL must be absolute: " + uri);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new UnsafeUrlException("Only http and https URLs are allowed: " + uri);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeUrlException("URL host is required: " + uri);
        }

        int port = uri.getPort();
        if (port != -1 && !ALLOWED_PORTS.contains(port)) {
            throw new UnsafeUrlException("Port not allowed (use 80 or 443): " + uri);
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new UnsafeUrlException("Cannot resolve host: " + host, e);
        }
        if (addresses.length == 0) {
            throw new UnsafeUrlException("Cannot resolve host: " + host);
        }

        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new UnsafeUrlException(
                        "Refusing to fetch private or local address for host " + host + ": " + address.getHostAddress());
            }
        }
    }

    /**
     * @return true when {@link #requireSafeOutbound(URI)} would succeed
     */
    public static boolean isSafeOutbound(URI uri) {
        try {
            requireSafeOutbound(uri);
            return true;
        } catch (UnsafeUrlException e) {
            return false;
        }
    }

    static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        if (isUniqueLocalIpv6(address)) {
            return true;
        }
        if (isIpv4ZeroNetwork(address)) {
            return true;
        }
        return false;
    }

    /** IPv6 ULA {@code fc00::/7}. */
    private static boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        return (bytes[0] & 0xfe) == 0xfc;
    }

    /** IPv4 {@code 0.0.0.0/8}. */
    private static boolean isIpv4ZeroNetwork(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            return false;
        }
        return address.getAddress()[0] == 0;
    }
}
