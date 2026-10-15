package de._0x2b.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import de._0x2b.model.Article;
import de._0x2b.model.Feed;
import de._0x2b.model.Folder;
import de._0x2b.resource.dto.ArticleSummary;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ArticleRepositoryIT {

    @Inject
    ArticleRepository articleRepository;
    @Inject
    FeedRepository feedRepository;
    @Inject
    FolderRepository folderRepository;
    @Inject
    DataSource dataSource;

    @Test
    void create_upsertsOnFeedGuid_andListsWithoutBodies() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "it-folder-" + suffix, null, "f-base"));
        int feedId = feedRepository.create(new Feed(-1, folderId, "it-feed-" + suffix,
                URI.create("https://example.com/" + suffix),
                URI.create("https://example.com/" + suffix + "/rss")));

        Instant published = Instant.parse("2026-01-15T12:00:00Z");
        Article first = new Article(-1, feedId, "it-feed-" + suffix, "Title", "desc", "<p>body-1</p>",
                "https://example.com/a", "https://example.com/a", published, "Author", "", "[]");
        articleRepository.create(List.of(first));

        Article updated = new Article(-1, feedId, "it-feed-" + suffix, "Title updated", "desc2", "<p>body-2</p>",
                "https://example.com/a", "https://example.com/a", published, "Author", "", "[]");
        articleRepository.create(List.of(updated));

        List<ArticleSummary> page = articleRepository.findByFeed(feedId, "");
        assertEquals(1, page.size(), "ON CONFLICT (feed_id, guid) should upsert, not duplicate");
        ArticleSummary summary = page.getFirst();
        assertEquals("Title updated", summary.title());
        assertEquals(published, summary.published());
        assertFalse(summary.read());

        Article detail = articleRepository.findById(summary.id());
        assertNotNull(detail);
        assertEquals("<p>body-2</p>", detail.getContent());
        assertEquals("https://example.com/a", detail.getGuid());
    }

    @Test
    void create_allowsNullPublished_andExcludesFromList() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "it-nullpub-" + suffix, null, "f-base"));
        int feedId = feedRepository.create(new Feed(-1, folderId, "it-nullpub-feed-" + suffix,
                URI.create("https://example.org/" + suffix),
                URI.create("https://example.org/" + suffix + "/feed")));

        Article undated = new Article(-1, feedId, "n", "No date", "", "", "https://example.org/x",
                "https://example.org/x", null, "", "", "[]");
        articleRepository.create(List.of(undated));

        assertTrue(articleRepository.findByFeed(feedId, "").isEmpty(),
                "NULL published articles must be excluded from the list query");

        long id = latestArticleId(feedId);
        Article detail = articleRepository.findById(id);
        assertNotNull(detail);
        assertNull(detail.getPublished());
    }

    @Test
    void markRead_isIdempotent_andExistsDistinguishesMissing() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "it-read-" + suffix, null, "f-base"));
        int feedId = feedRepository.create(new Feed(-1, folderId, "it-read-feed-" + suffix,
                URI.create("https://read.example/" + suffix),
                URI.create("https://read.example/" + suffix + "/rss")));

        Instant published = Instant.parse("2026-02-01T00:00:00Z");
        articleRepository.create(List.of(new Article(-1, feedId, "f", "R", "", "", "https://read.example/1",
                "https://read.example/1", published, "", "", "[]")));
        long id = articleRepository.findByFeed(feedId, "").getFirst().id();

        assertEquals(1, articleRepository.markRead(id));
        assertEquals(0, articleRepository.markRead(id), "already-read is a no-op");
        assertTrue(articleRepository.exists(id));
        assertFalse(articleRepository.exists(Long.MAX_VALUE - 7));
    }

    private long latestArticleId(int feedId) throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT id FROM article WHERE feed_id = ? ORDER BY id DESC LIMIT 1")) {
            stmt.setInt(1, feedId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                return rs.getLong(1);
            }
        }
    }
}
