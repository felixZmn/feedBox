package de._0x2b.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.BadRequestException;

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
    void resolveListView_blankIsAll() {
        assertEquals(ArticleResource.ListView.ALL, ArticleResource.resolveListView(null, null, null));
        assertEquals(ArticleResource.ListView.ALL, ArticleResource.resolveListView(null, null, "  "));
    }

    @Test
    void resolveListView_acceptsUnreadAndSaved() {
        assertEquals(ArticleResource.ListView.UNREAD, ArticleResource.resolveListView(null, null, "unread"));
        assertEquals(ArticleResource.ListView.SAVED, ArticleResource.resolveListView(null, null, "saved"));
    }

    @Test
    void resolveListView_rejectsUnknownView() {
        assertThrows(BadRequestException.class, () -> ArticleResource.resolveListView(null, null, "later"));
    }

    @Test
    void resolveListView_rejectsViewCombinedWithFolderOrFeed() {
        assertThrows(BadRequestException.class, () -> ArticleResource.resolveListView(1, null, "unread"));
        assertThrows(BadRequestException.class, () -> ArticleResource.resolveListView(null, 2, "saved"));
    }

    @Test
    void resolveListView_rejectsFolderAndFeedTogether() {
        assertThrows(BadRequestException.class, () -> ArticleResource.resolveListView(1, 2, null));
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
