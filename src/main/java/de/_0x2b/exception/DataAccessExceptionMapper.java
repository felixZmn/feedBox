package de._0x2b.exception;

import de._0x2b.resource.dto.ApiError;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates {@link DataAccessException} into a 500 Internal Server Error
 * with code {@code DATA_ACCESS} and a generic message. The wrapped
 * {@link java.sql.SQLException} is logged with the stack trace, never sent.
 */
@Provider
public class DataAccessExceptionMapper implements ExceptionMapper<DataAccessException> {
    private static final Logger logger = LoggerFactory.getLogger(DataAccessExceptionMapper.class);

    @Override
    public Response toResponse(DataAccessException e) {
        logger.error("Database access error: {}", e.getMessage(), e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError("DATA_ACCESS", "The database could not complete the request"))
                .build();
    }
}
