package de._0x2b.exception;

import de._0x2b.resource.dto.ApiError;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    private static final Logger logger = LoggerFactory.getLogger(NotFoundExceptionMapper.class);

    @Override
    public Response toResponse(NotFoundException e) {
        logger.debug("Not found: {}", e.getMessage());
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError("NOT_FOUND", e.getMessage()))
                .build();
    }
}
