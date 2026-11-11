package de._0x2b.exception;

import de._0x2b.resource.dto.ApiError;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catch-all for unexpected failures. Logs the stack and returns a 500
 * {@link ApiError} with a generic message.
 * {@link WebApplicationException} keeps its status: client errors get an
 * {@link ApiError} carrying the exception message (thrown by our own
 * boundary validation); redirects and responses that already have a body
 * pass through unchanged. Implements {@code Throwable} so Errors do not
 * fall through unmapped.
 */
@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger logger = LoggerFactory.getLogger(UnhandledExceptionMapper.class);
    static final String INTERNAL_MESSAGE = "Something went wrong on the server";

    @Override
    public Response toResponse(Throwable e) {
        if (e instanceof WebApplicationException wae) {
            return toResponse(wae);
        }
        logger.error("Unhandled exception", e);
        return error(500, "INTERNAL", INTERNAL_MESSAGE);
    }

    private Response toResponse(WebApplicationException e) {
        Response original = e.getResponse();
        int status = original.getStatus();
        if (status < 400 || original.hasEntity()) {
            return original;
        }
        if (status >= 500) {
            logger.error("Server error {}", status, e);
            return error(status, "INTERNAL", INTERNAL_MESSAGE);
        }
        String message = e.getMessage() != null && !e.getMessage().isBlank()
                ? e.getMessage()
                : original.getStatusInfo().getReasonPhrase();
        return error(status, codeFor(status), message);
    }

    static String codeFor(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 409 -> "CONFLICT";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 429 -> "TOO_MANY_REQUESTS";
            default -> status >= 500 ? "INTERNAL" : "CLIENT_ERROR";
        };
    }

    private static Response error(int status, String code, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError(code, message))
                .build();
    }
}
