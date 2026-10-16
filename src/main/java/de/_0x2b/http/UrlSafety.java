package de._0x2b.http;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SSRF guard for server-initiated HTTP(S) fetches. Validates scheme, port, and
 * resolved destination addresses before connect and on every redirect hop.
 */
public final class UrlSafety {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    /** Default when callers do not pass an explicit allow-list. */
    public static final Set<Integer> DEFAULT_ALLOWED_PORTS = Set.of(80, 443);

    private UrlSafety() {
    }

    /**
     * @throws UnsafeUrlException when the URI must not be fetched by the server
     */
    public static void requireSafeOutbound(URI uri) {
        requireSafeOutbound(uri, DEFAULT_ALLOWED_PORTS);
    }

    /**
     * @param allowedPorts explicit ports; empty/null falls back to
     *                     {@link #DEFAULT_ALLOWED_PORTS}
     * @throws UnsafeUrlException when the URI must not be fetched by the server
     */
    public static void requireSafeOutbound(URI uri, Set<Integer> allowedPorts) {
        resolveValidated(uri, allowedPorts);
    }

    /**
     * Resolve and validate {@code uri}. Returns the addresses that must be used
     * for the subsequent connect (pin via {@link PinnedDns}).
     *
     * @throws UnsafeUrlException when the URI must not be fetched by the server
     */
    public static List<InetAddress> resolveValidated(URI uri) {
        return resolveValidated(uri, DEFAULT_ALLOWED_PORTS);
    }

    /**
     * @param allowedPorts explicit ports; empty/null falls back to
     *                     {@link #DEFAULT_ALLOWED_PORTS}
     */
    public static List<InetAddress> resolveValidated(URI uri, Set<Integer> allowedPorts) {
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

        Set<Integer> ports = (allowedPorts == null || allowedPorts.isEmpty())
                ? DEFAULT_ALLOWED_PORTS
                : allowedPorts;
        int port = uri.getPort();
        if (port != -1 && !ports.contains(port)) {
            throw new UnsafeUrlException("Port not allowed: " + uri);
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

        List<InetAddress> safe = new ArrayList<>(addresses.length);
        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new UnsafeUrlException(
                        "Refusing to fetch private or local address for host " + host + ": "
                                + address.getHostAddress());
            }
            safe.add(address);
        }
        return List.copyOf(safe);
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
        if (isCarrierGradeNat(address)) {
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

    /** IPv4 CGNAT {@code 100.64.0.0/10}. */
    private static boolean isCarrierGradeNat(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        return first == 100 && second >= 64 && second <= 127;
    }
}
