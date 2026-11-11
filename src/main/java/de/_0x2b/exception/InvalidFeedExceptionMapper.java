package de._0x2b.exception;

import de._0x2b.resource.dto.ApiError;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates {@link InvalidFeedException} into a 422 with the exception's
 * code (e.g. {@code NOT_A_FEED}, {@code UNREACHABLE}).
 */
@Provider
public class InvalidFeedExceptionMapper implements ExceptionMapper<InvalidFeedException> {
    private static final Logger logger = LoggerFactory.getLogger(InvalidFeedExceptionMapper.class);

    @Override
    public Response toResponse(InvalidFeedException e) {
        logger.warn("Invalid feed ({}): {}", e.getCode(), e.getMessage());
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError(e.getCode(), e.getMessage()))
                .build();
    }
}
