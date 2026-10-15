package de._0x2b.resource.dto;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import jakarta.ws.rs.BadRequestException;

/**
 * Keyset pagination cursor {@code (published, id)}. Both query params must be
 * present together or both absent — a partial cursor is a client error.
 */
public record PaginationCursor(Long id, Instant published) {

    public boolean isEmpty() {
        return id == null && published == null;
    }

    /**
     * @param paginationId   {@code pagination_id} query param (nullable)
     * @param paginationDate {@code pagination_date} ISO-8601 instant (nullable)
     * @throws BadRequestException when only one of the two is set, or the date
     *                             is not a valid Instant
     */
    public static PaginationCursor from(Long paginationId, String paginationDate) {
        boolean hasId = paginationId != null;
        boolean hasDate = paginationDate != null && !paginationDate.isBlank();

        if (!hasId && !hasDate) {
            return new PaginationCursor(null, null);
        }
        if (!hasId || !hasDate) {
            throw new BadRequestException(
                    "pagination_id and pagination_date must both be provided");
        }
        try {
            return new PaginationCursor(paginationId, Instant.parse(paginationDate.trim()));
        } catch (DateTimeParseException e) {
            throw new BadRequestException(
                    "pagination_date must be an ISO-8601 instant (e.g. 2026-01-01T10:00:00Z)");
        }
    }
}
