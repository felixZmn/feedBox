package de._0x2b.model;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FolderTreeTest {

    @Test
    void from_groupsFeedsByFolderId() {
        Folder tech = new Folder(10, "Tech", null, "f-base");
        Folder news = new Folder(20, "News", null, "f-base");

        Feed hn = new Feed(1, 10, "HN",
                URI.create("https://news.ycombinator.com/"),
                URI.create("https://news.ycombinator.com/rss"));
        Feed bbc = new Feed(2, 20, "BBC",
                URI.create("https://bbc.com/"),
                URI.create("https://bbc.com/rss"));

        FolderTree tree = FolderTree.from(List.of(tech, news), List.of(hn, bbc));

        assertEquals(2, tree.getFolders().size());
        assertEquals(1, tree.getFolders().get(0).getFeeds().size());
        assertEquals("HN", tree.getFolders().get(0).getFeeds().get(0).getName());
        assertEquals(1, tree.getFolders().get(1).getFeeds().size());
        assertEquals("BBC", tree.getFolders().get(1).getFeeds().get(0).getName());
        assertTrue(tree.getUnfiledFeeds().isEmpty());
    }

    @Test
    void from_putsNullFolderIdFeedsInUnfiled() {
        Feed unfiled = new Feed(1, null, "Standalone",
                URI.create("https://example.com"),
                URI.create("https://example.com/rss"));

        FolderTree tree = FolderTree.from(List.of(), List.of(unfiled));

        assertTrue(tree.getFolders().isEmpty());
        assertEquals(1, tree.getUnfiledFeeds().size());
        assertEquals("Standalone", tree.getUnfiledFeeds().get(0).getName());
    }

    @Test
    void from_putsOrphanFeedsInUnfiled() {
        Folder tech = new Folder(10, "Tech", null, "f-base");
        Feed orphan = new Feed(1, 99, "Orphan",
                URI.create("https://orphan.example"),
                URI.create("https://orphan.example/rss"));

        FolderTree tree = FolderTree.from(List.of(tech), List.of(orphan));

        assertTrue(tree.getFolders().get(0).getFeeds().isEmpty());
        assertEquals(1, tree.getUnfiledFeeds().size());
        assertEquals("Orphan", tree.getUnfiledFeeds().get(0).getName());
    }
}
