package de._0x2b.http;

/**
 * Why an outbound GET produced no usable body. {@link #message()} is short,
 * user-facing text (stored as a feed's {@code last_error}); it never carries
 * resolved addresses or exception dumps.
 *
 * @param kind       failure category
 * @param statusCode final HTTP status for {@link Kind#HTTP_STATUS}, else 0
 * @param message    readable reason, e.g. "HTTP 404" or "Timed out"
 */
public record FetchFailure(Kind kind, int statusCode, String message) implements FetchOutcome {

    public enum Kind {
        /** Not an absolute http(s) URL. */
        INVALID_URL,
        /** Port, private destination, or https-to-http redirect refused. */
        BLOCKED,
        /** DNS, connection, or redirect failure. */
        UNREACHABLE,
        TIMEOUT,
        /** Final response was not 2xx. */
        HTTP_STATUS,
        /** Body exceeds the configured size cap. */
        TOO_LARGE
    }

    public static FetchFailure invalidUrl() {
        return new FetchFailure(Kind.INVALID_URL, 0, "Invalid URL");
    }

    public static FetchFailure blocked(String message) {
        return new FetchFailure(Kind.BLOCKED, 0, message);
    }

    public static FetchFailure unreachable(String message) {
        return new FetchFailure(Kind.UNREACHABLE, 0, message);
    }

    public static FetchFailure timeout() {
        return new FetchFailure(Kind.TIMEOUT, 0, "Timed out");
    }

    public static FetchFailure httpStatus(int statusCode) {
        return new FetchFailure(Kind.HTTP_STATUS, statusCode, "HTTP " + statusCode);
    }

    public static FetchFailure tooLarge() {
        return new FetchFailure(Kind.TOO_LARGE, 0, "Response too large");
    }
}
