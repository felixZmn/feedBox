package de._0x2b.http;

/**
 * Thrown when an outbound URL is rejected by the SSRF guard (scheme, port,
 * private/link-local/loopback destination, etc.).
 */
public class UnsafeUrlException extends RuntimeException {
    public UnsafeUrlException(String message) {
        super(message);
    }

    public UnsafeUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
