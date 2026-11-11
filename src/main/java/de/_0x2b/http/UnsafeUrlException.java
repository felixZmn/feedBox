package de._0x2b.http;

/**
 * Thrown when an outbound URL is rejected by the SSRF guard (scheme, port,
 * private/link-local/loopback destination, etc.).
 */
public class UnsafeUrlException extends RuntimeException {

    public enum Reason {
        /** Not an absolute http(s) URL with a host. */
        INVALID_URL,
        /** Port or destination address is not allowed. */
        BLOCKED,
        /** The host name does not resolve. */
        UNRESOLVED
    }

    private final Reason reason;

    public UnsafeUrlException(String message) {
        this(Reason.BLOCKED, message);
    }

    public UnsafeUrlException(String message, Throwable cause) {
        this(Reason.BLOCKED, message, cause);
    }

    public UnsafeUrlException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public UnsafeUrlException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
