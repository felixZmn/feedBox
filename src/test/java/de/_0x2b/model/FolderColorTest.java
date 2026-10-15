package de._0x2b.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FolderColorTest {

    @Test
    void from_knownValues() {
        assertEquals(FolderColor.F_BASE, FolderColor.from("f-base"));
        assertEquals(FolderColor.F_RED, FolderColor.from("f-red"));
        assertEquals(FolderColor.F_VIOLET, FolderColor.from("f-violet"));
    }

    @Test
    void from_nullOrBlank_defaultsToBase() {
        assertEquals(FolderColor.F_BASE, FolderColor.from(null));
        assertEquals(FolderColor.F_BASE, FolderColor.from(""));
        assertEquals(FolderColor.F_BASE, FolderColor.from("  "));
    }

    @Test
    void from_unknown_throws() {
        assertThrows(IllegalArgumentException.class, () -> FolderColor.from("f-hacker"));
        assertThrows(IllegalArgumentException.class, () -> FolderColor.from("red"));
    }

    @Test
    void isAllowed() {
        assertTrue(FolderColor.isAllowed("f-base"));
        assertFalse(FolderColor.isAllowed("f-hacker"));
        assertFalse(FolderColor.isAllowed(null));
    }

    @Test
    void cssClass_roundTripsViaJsonValue() {
        assertEquals("f-green", FolderColor.F_GREEN.cssClass());
    }
}
