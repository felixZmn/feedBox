package de._0x2b.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class InvalidFeedExceptionMapper implements ExceptionMapper<InvalidFeedException> {
    private static final Logger logger = LoggerFactory.getLogger(InvalidFeedExceptionMapper.class);

    @Override
    public Response toResponse(InvalidFeedException e) {
        logger.warn("Invalid feed: {}", e.getMessage());
        return Response.status(422)
                .type(MediaType.TEXT_PLAIN)
                .entity(e.getMessage())
                .build();
    }
}
