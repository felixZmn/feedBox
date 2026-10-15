package de._0x2b.exception;

/**
 * The supplied feed URL is unreachable or not a valid RSS/Atom feed.
 * Mapped to HTTP 422.
 */
public class InvalidFeedException extends RuntimeException {
    public InvalidFeedException(String message) {
        super(message);
    }

    public InvalidFeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
