package de._0x2b.http;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-local DNS pin used with {@link PinnedInetAddressResolverProvider} so
 * {@link java.net.http.HttpClient} reconnects to the same addresses
 * {@link UrlSafety} already validated — closing the DNS-rebinding TOCTOU window
 * without rewriting the request URI (SNI / cert checks keep the hostname).
 */
public final class PinnedDns {

    private static final ThreadLocal<Map<String, List<InetAddress>>> PIN = new ThreadLocal<>();

    private PinnedDns() {
    }

    static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        return host.toLowerCase(Locale.ROOT);
    }

    /**
     * Pin {@code host} to {@code addresses} for the current thread until
     * {@link #clear()} or {@link #unpin(String)}.
     */
    public static void pin(String host, List<InetAddress> addresses) {
        if (host == null || host.isBlank() || addresses == null || addresses.isEmpty()) {
            return;
        }
        Map<String, List<InetAddress>> map = PIN.get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            PIN.set(map);
        }
        map.put(normalizeHost(host), List.copyOf(addresses));
    }

    public static void unpin(String host) {
        Map<String, List<InetAddress>> map = PIN.get();
        if (map != null) {
            map.remove(normalizeHost(host));
            if (map.isEmpty()) {
                PIN.remove();
            }
        }
    }

    public static void clear() {
        PIN.remove();
    }

    /** @return pinned addresses for {@code host}, or {@code null} when unset */
    static List<InetAddress> lookup(String host) {
        Map<String, List<InetAddress>> map = PIN.get();
        if (map == null) {
            return null;
        }
        return map.get(normalizeHost(host));
    }
}
