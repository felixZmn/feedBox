package de._0x2b.resource.dto;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PaginationCursorTest {

    @Test
    void from_bothAbsent_returnsEmpty() {
        PaginationCursor cursor = PaginationCursor.from(null, null);
        assertTrue(cursor.isEmpty());
        assertNull(cursor.id());
        assertNull(cursor.published());
    }

    @Test
    void from_blankDate_treatedAsAbsent() {
        assertTrue(PaginationCursor.from(null, "  ").isEmpty());
    }

    @Test
    void from_onlyId_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> PaginationCursor.from(1L, null));
        assertThrows(BadRequestException.class, () -> PaginationCursor.from(1L, ""));
    }

    @Test
    void from_onlyDate_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> PaginationCursor.from(null, "2026-01-01T10:00:00Z"));
    }

    @Test
    void from_bothPresent_parsesInstant() {
        PaginationCursor cursor = PaginationCursor.from(42L, "2026-01-01T10:00:00Z");
        assertFalse(cursor.isEmpty());
        assertEquals(42L, cursor.id());
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), cursor.published());
    }

    @Test
    void from_invalidDate_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> PaginationCursor.from(1L, "not-a-date"));
    }
}
