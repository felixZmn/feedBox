package de._0x2b.exception;

import de._0x2b.resource.dto.ApiError;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionMappersTest {

    @Test
    void duplicate_is409WithDuplicateCode() {
        Response response = new DuplicateEntityExceptionMapper()
                .toResponse(new DuplicateEntityException("Feed with this URL already exists"));

        assertError(response, 409, "DUPLICATE", "Feed with this URL already exists");
    }

    @Test
    void invalidFeed_defaultsToInvalidFeedCode() {
        Response response = new InvalidFeedExceptionMapper()
                .toResponse(new InvalidFeedException("nope"));

        assertError(response, 422, "INVALID_FEED", "nope");
    }

    @Test
    void invalidFeed_keepsSpecificCode() {
        Response response = new InvalidFeedExceptionMapper()
                .toResponse(new InvalidFeedException("NOT_A_FEED", "No feed found"));

        assertError(response, 422, "NOT_A_FEED", "No feed found");
    }

    @Test
    void invalidFeed_blankCodeFallsBackToDefault() {
        assertEquals("INVALID_FEED", new InvalidFeedException(" ", "x").getCode());
    }

    @Test
    void notFound_is404WithNotFoundCode() {
        Response response = new NotFoundExceptionMapper()
                .toResponse(new NotFoundException("Feed not found: 3"));

        assertError(response, 404, "NOT_FOUND", "Feed not found: 3");
    }

    @Test
    void dataAccess_is500AndHidesSqlDetail() {
        Response response = new DataAccessExceptionMapper()
                .toResponse(new DataAccessException("SELECT * FROM feed failed", new SQLException("boom")));

        ApiError error = assertError(response, 500, "DATA_ACCESS", null);
        assertFalse(error.message().contains("SELECT"));
    }

    @Test
    void unhandled_is500WithGenericMessage() {
        Response response = new UnhandledExceptionMapper()
                .toResponse(new IllegalStateException("secret internals"));

        ApiError error = assertError(response, 500, "INTERNAL", UnhandledExceptionMapper.INTERNAL_MESSAGE);
        assertFalse(error.message().contains("secret"));
    }

    @Test
    void unhandled_badRequestKeepsStatusAndMessage() {
        Response response = new UnhandledExceptionMapper()
                .toResponse(new BadRequestException("url query parameter is required"));

        assertError(response, 400, "BAD_REQUEST", "url query parameter is required");
    }

    @Test
    void unhandled_payloadTooLargeKeepsStatus() {
        Response response = new UnhandledExceptionMapper()
                .toResponse(new WebApplicationException("OPML payload too large (max 5 MiB)", 413));

        assertError(response, 413, "PAYLOAD_TOO_LARGE", "OPML payload too large (max 5 MiB)");
    }

    @Test
    void unhandled_serverWebApplicationExceptionHidesMessage() {
        Response response = new UnhandledExceptionMapper()
                .toResponse(new WebApplicationException("upstream detail", 503));

        assertError(response, 503, "INTERNAL", UnhandledExceptionMapper.INTERNAL_MESSAGE);
    }

    @Test
    void unhandled_redirectPassesThrough() {
        Response redirect = Response.status(302).header("Location", "/start").build();

        Response response = new UnhandledExceptionMapper()
                .toResponse(new WebApplicationException(redirect));

        assertSame(redirect, response);
    }

    private static ApiError assertError(Response response, int status, String code, String message) {
        assertEquals(status, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        ApiError error = assertInstanceOf(ApiError.class, response.getEntity());
        assertEquals(code, error.code());
        if (message != null) {
            assertEquals(message, error.message());
        }
        return error;
    }
}
