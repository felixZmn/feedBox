package de._0x2b.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArticleResourceTest {

    private static final int MAX_SEARCH_LENGTH = 100;

    @Test
    void normalizeSearch_nullBecomesEmpty() {
        assertEquals("", ArticleResource.normalizeSearch(null));
    }

    @Test
    void normalizeSearch_trimsSurroundingWhitespace() {
        assertEquals("postgres", ArticleResource.normalizeSearch("  postgres  "));
    }

    @Test
    void normalizeSearch_whitespaceOnlyBecomesEmpty() {
        assertEquals("", ArticleResource.normalizeSearch("   \t "));
    }

    @Test
    void normalizeSearch_keepsInnerWhitespaceForTokenSplitting() {
        assertEquals("postgres index", ArticleResource.normalizeSearch(" postgres index "));
    }

    @Test
    void normalizeSearch_leavesQueryAtLimitIntact() {
        String atLimit = "a".repeat(MAX_SEARCH_LENGTH);

        assertEquals(atLimit, ArticleResource.normalizeSearch(atLimit));
    }

    @Test
    void normalizeSearch_capsOverlongQuery() {
        String tooLong = "a".repeat(MAX_SEARCH_LENGTH + 20);

        String result = ArticleResource.normalizeSearch(tooLong);

        assertEquals(MAX_SEARCH_LENGTH, result.length());
        assertEquals("a".repeat(MAX_SEARCH_LENGTH), result);
    }

    @Test
    void normalizeSearch_trimsBeforeApplyingCap() {
        String padded = "  " + "a".repeat(MAX_SEARCH_LENGTH) + "  ";

        assertEquals("a".repeat(MAX_SEARCH_LENGTH), ArticleResource.normalizeSearch(padded));
    }
}
