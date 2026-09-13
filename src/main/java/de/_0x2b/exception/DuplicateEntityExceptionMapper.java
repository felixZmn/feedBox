package de._0x2b.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates {@link DuplicateEntityException} into a 409 Conflict with the
 * exception message so clients can show a user-friendly duplicate error.
 */
@Provider
public class DuplicateEntityExceptionMapper implements ExceptionMapper<DuplicateEntityException> {
    private static final Logger logger = LoggerFactory.getLogger(DuplicateEntityExceptionMapper.class);

    @Override
    public Response toResponse(DuplicateEntityException e) {
        logger.warn("Duplicate entity: {}", e.getMessage());
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.TEXT_PLAIN)
                .entity(e.getMessage())
                .build();
    }
}
