package de._0x2b.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catch-all for unexpected failures. Logs the stack and returns an empty 500.
 * {@link WebApplicationException} is left to the JAX-RS runtime (it already
 * carries the correct status). Implements {@code Throwable} so Errors do not
 * fall through unmapped.
 */
@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger logger = LoggerFactory.getLogger(UnhandledExceptionMapper.class);

    @Override
    public Response toResponse(Throwable e) {
        if (e instanceof WebApplicationException wae) {
            return wae.getResponse();
        }
        logger.error("Unhandled exception", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
}
