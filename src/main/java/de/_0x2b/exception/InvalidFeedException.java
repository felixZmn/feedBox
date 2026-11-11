package de._0x2b.exception;

/**
 * The supplied feed URL is unreachable or not a valid RSS/Atom feed.
 * Mapped to HTTP 422. {@link #getCode()} tells the client why.
 */
public class InvalidFeedException extends RuntimeException {
    public static final String DEFAULT_CODE = "INVALID_FEED";

    private final String code;

    public InvalidFeedException(String message) {
        this(DEFAULT_CODE, message);
    }

    public InvalidFeedException(String message, Throwable cause) {
        this(DEFAULT_CODE, message, cause);
    }

    public InvalidFeedException(String code, String message) {
        super(message);
        this.code = code != null && !code.isBlank() ? code : DEFAULT_CODE;
    }

    public InvalidFeedException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code != null && !code.isBlank() ? code : DEFAULT_CODE;
    }

    public String getCode() {
        return code;
    }
}
