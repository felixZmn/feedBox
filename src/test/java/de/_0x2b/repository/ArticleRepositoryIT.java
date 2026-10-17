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

    @Test
    void markAllRead_horizonHidesExisting_andKeepsLaterInsertUnread() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "it-horizon-all-" + suffix, null, "f-base"));
        int feedId = feedRepository.create(new Feed(-1, folderId, "it-horizon-all-feed-" + suffix,
                URI.create("https://horizon-all.example/" + suffix),
                URI.create("https://horizon-all.example/" + suffix + "/rss")));

        Instant published = Instant.parse("2026-03-01T00:00:00Z");
        articleRepository.create(List.of(
                new Article(-1, feedId, "f", "Old", "", "", "https://horizon-all.example/old",
                        "https://horizon-all.example/old", published, "", "", "[]")));
        long oldId = articleRepository.findByFeed(feedId, "").getFirst().id();
        assertFalse(articleRepository.findById(oldId).isRead());
        assertEquals(1, articleRepository.countUnreadByFeed().getOrDefault(feedId, 0));

        assertEquals(1, articleRepository.markAllRead());
        assertTrue(articleRepository.findById(oldId).isRead(), "existing id must be effectively read");
        assertEquals(0, articleRepository.countUnreadByFeed().getOrDefault(feedId, 0));

        Instant newerPub = Instant.parse("2020-01-01T00:00:00Z");
        articleRepository.create(List.of(
                new Article(-1, feedId, "f", "Backfill", "", "", "https://horizon-all.example/new",
                        "https://horizon-all.example/new", newerPub, "", "", "[]")));
        ArticleSummary backfill = articleRepository.findByFeed(feedId, "").stream()
                .filter(a -> "Backfill".equals(a.title()))
                .findFirst()
                .orElseThrow();
        assertFalse(backfill.read(), "new id with old published must stay unread");
        assertEquals(1, articleRepository.countUnreadByFeed().getOrDefault(feedId, 0));
    }

    @Test
    void markReadByFeed_doesNotHideOtherFeed() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "it-horizon-feed-" + suffix, null, "f-base"));
        int feedA = feedRepository.create(new Feed(-1, folderId, "feed-a-" + suffix,
                URI.create("https://horizon-feed.example/a/" + suffix),
                URI.create("https://horizon-feed.example/a/" + suffix + "/rss")));
        int feedB = feedRepository.create(new Feed(-1, folderId, "feed-b-" + suffix,
                URI.create("https://horizon-feed.example/b/" + suffix),
                URI.create("https://horizon-feed.example/b/" + suffix + "/rss")));

        Instant published = Instant.parse("2026-03-02T00:00:00Z");
        articleRepository.create(List.of(
                new Article(-1, feedA, "a", "A1", "", "", "https://horizon-feed.example/a1",
                        "https://horizon-feed.example/a1", published, "", "", "[]"),
                new Article(-1, feedB, "b", "B1", "", "", "https://horizon-feed.example/b1",
                        "https://horizon-feed.example/b1", published, "", "", "[]")));

        assertEquals(1, articleRepository.markReadByFeed(feedA));
        assertTrue(articleRepository.findByFeed(feedA, "").getFirst().read());
        assertFalse(articleRepository.findByFeed(feedB, "").getFirst().read());
        assertEquals(0, articleRepository.countUnreadByFeed().getOrDefault(feedA, 0));
        assertEquals(1, articleRepository.countUnreadByFeed().getOrDefault(feedB, 0));
    }

    @Test
    void markReadByFolder_coversOnlyThatFolder() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderKeep = folderRepository.create(new Folder(-1, "it-horizon-keep-" + suffix, null, "f-base"));
        int folderMark = folderRepository.create(new Folder(-1, "it-horizon-mark-" + suffix, null, "f-base"));
        int feedKeep = feedRepository.create(new Feed(-1, folderKeep, "keep-" + suffix,
                URI.create("https://horizon-folder.example/keep/" + suffix),
                URI.create("https://horizon-folder.example/keep/" + suffix + "/rss")));
        int feedMark = feedRepository.create(new Feed(-1, folderMark, "mark-" + suffix,
                URI.create("https://horizon-folder.example/mark/" + suffix),
                URI.create("https://horizon-folder.example/mark/" + suffix + "/rss")));

        Instant published = Instant.parse("2026-03-03T00:00:00Z");
        articleRepository.create(List.of(
                new Article(-1, feedKeep, "k", "Keep", "", "", "https://horizon-folder.example/k",
                        "https://horizon-folder.example/k", published, "", "", "[]"),
                new Article(-1, feedMark, "m", "Mark", "", "", "https://horizon-folder.example/m",
                        "https://horizon-folder.example/m", published, "", "", "[]")));

        assertEquals(1, articleRepository.markReadByFolder(folderMark));
        assertTrue(articleRepository.findByFeed(feedMark, "").getFirst().read());
        assertFalse(articleRepository.findByFeed(feedKeep, "").getFirst().read());
    }

    @Test
    void save_staysSavedAfterMarkRead_andListsNewestFirst() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "it-saved-" + suffix, null, "f-base"));
        int feedId = feedRepository.create(new Feed(-1, folderId, "it-saved-feed-" + suffix,
                URI.create("https://saved.example/" + suffix),
                URI.create("https://saved.example/" + suffix + "/rss")));

        Instant older = Instant.parse("2026-04-01T00:00:00Z");
        Instant newer = Instant.parse("2026-05-01T00:00:00Z");
        articleRepository.create(List.of(
                new Article(-1, feedId, "f", "Older saved", "", "", "https://saved.example/old",
                        "https://saved.example/old", older, "", "", "[]"),
                new Article(-1, feedId, "f", "Newer saved", "", "", "https://saved.example/new",
                        "https://saved.example/new", newer, "", "", "[]"),
                new Article(-1, feedId, "f", "Not saved", "", "", "https://saved.example/no",
                        "https://saved.example/no", newer, "", "", "[]")));
        long olderId = idByTitle(feedId, "Older saved");
        long newerId = idByTitle(feedId, "Newer saved");
        long unsavedId = idByTitle(feedId, "Not saved");
        int before = articleRepository.countSaved();

        assertEquals(1, articleRepository.save(olderId));
        assertEquals(0, articleRepository.save(olderId), "already saved is a no-op");
        assertEquals(1, articleRepository.save(newerId));
        assertEquals(before + 2, articleRepository.countSaved());

        assertEquals(1, articleRepository.markRead(olderId));
        articleRepository.markAllRead();

        List<Long> savedIds = articleRepository.findSaved("").stream()
                .filter(a -> a.feedId() == feedId)
                .map(ArticleSummary::id)
                .toList();
        assertEquals(List.of(newerId, olderId), savedIds);
        ArticleSummary newerSaved = articleRepository.findSaved("").stream()
                .filter(a -> a.id() == newerId)
                .findFirst()
                .orElseThrow();
        assertTrue(newerSaved.saved());
        assertTrue(newerSaved.read(), "horizon mark-read must not clear saved");
        assertTrue(articleRepository.findById(olderId).isSaved());

        assertEquals(1, articleRepository.unsave(newerId));
        assertEquals(0, articleRepository.unsave(newerId));
        assertEquals(before + 1, articleRepository.countSaved());
        assertFalse(articleRepository.findSaved("").stream().anyMatch(a -> a.id() == newerId));
        assertFalse(articleRepository.findSaved("").stream().anyMatch(a -> a.id() == unsavedId));
    }

    @Test
    void findUnread_omitsColumnReadAndHorizonRead_newestFirst() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "it-unread-view-" + suffix, null, "f-base"));
        int feedId = feedRepository.create(new Feed(-1, folderId, "it-unread-view-feed-" + suffix,
                URI.create("https://unread-view.example/" + suffix),
                URI.create("https://unread-view.example/" + suffix + "/rss")));

        Instant older = Instant.parse("2026-06-01T00:00:00Z");
        Instant newer = Instant.parse("2026-07-01T00:00:00Z");
        articleRepository.create(List.of(
                new Article(-1, feedId, "f", "Stay unread", "", "", "https://unread-view.example/stay",
                        "https://unread-view.example/stay", newer, "", "", "[]"),
                new Article(-1, feedId, "f", "Column read", "", "", "https://unread-view.example/col",
                        "https://unread-view.example/col", older, "", "", "[]")));
        long stayId = idByTitle(feedId, "Stay unread");
        long columnId = idByTitle(feedId, "Column read");
        assertEquals(1, articleRepository.markRead(columnId));

        List<Long> unreadIds = articleRepository.findUnread("").stream()
                .filter(a -> a.feedId() == feedId)
                .map(ArticleSummary::id)
                .toList();
        assertEquals(List.of(stayId), unreadIds);
        assertFalse(articleRepository.findUnread("").stream().anyMatch(a -> a.id() == columnId));

        articleRepository.markAllRead();
        assertFalse(articleRepository.findUnread("").stream().anyMatch(a -> a.feedId() == feedId));

        articleRepository.create(List.of(
                new Article(-1, feedId, "f", "After horizon", "", "", "https://unread-view.example/after",
                        "https://unread-view.example/after", older, "", "", "[]")));
        long afterId = idByTitle(feedId, "After horizon");
        List<Long> afterHorizon = articleRepository.findUnread("").stream()
                .filter(a -> a.feedId() == feedId)
                .map(ArticleSummary::id)
                .toList();
        assertEquals(List.of(afterId), afterHorizon);
        assertFalse(articleRepository.findById(afterId).isRead());
    }

    private long idByTitle(int feedId, String title) {
        return articleRepository.findByFeed(feedId, "").stream()
                .filter(a -> title.equals(a.title()))
                .findFirst()
                .orElseThrow()
                .id();
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
