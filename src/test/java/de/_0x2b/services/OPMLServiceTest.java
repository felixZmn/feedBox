package de._0x2b.services;

import de._0x2b.models.Feed;
import de._0x2b.models.Folder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class OPMLServiceTest {
    private final static String opmlStart = """
            <?xml version="1.0" encoding="utf-8"?>
            <opml version="1.1">
              <head>
                <title>subscriptions</title>
              </head>
              <body>
            """;
    private final static String opmlEnd = """
              </body>
            </opml>
            """;

    private final static String feeds = """
                <outline type="rss" text="Deutsche Welle: DW.com Deutsch" xmlUrl="https://rss.dw.com/xml/rss-de-all" htmlUrl="https://rss.dw.com/xml/rss-de-all" />
                <outline type="rss" text="WIRED" xmlUrl="https://www.wired.com/feed/rss" htmlUrl="https://www.wired.com/feed/rss" />
            """;

    private final static String folder1 = """
                <outline text="folder1">
                  <outline type="rss" text="Deutsche Welle: DW.com Deutsch" xmlUrl="https://rss.dw.com/xml/rss-de-all" htmlUrl="https://rss.dw.com/xml/rss-de-all" />
                  <outline type="rss" text="WIRED" xmlUrl="https://www.wired.com/feed/rss" htmlUrl="https://www.wired.com/feed/rss" />
                </outline>
            """;

    private final static String folder2 = """
                <outline text="folder2">
                  <outline type="rss" text="https://danluu.com/atom.xml" xmlUrl="https://danluu.com/atom.xml" htmlUrl="https://danluu.com/atom.xml" />
                  <outline type="rss" text="IT_Fettchen" xmlUrl="https://it-fettchen.de/rss/" htmlUrl="https://it-fettchen.micro.blog/" />
                </outline>
            """;

    private final static String folderInFolder = """
                <outline text="outer">
                  <outline type="rss" text="Deutsche Welle: DW.com Deutsch" xmlUrl="https://rss.dw.com/xml/rss-de-all" htmlUrl="https://rss.dw.com/xml/rss-de-all" />
                  <outline text="inner">
                    <outline type="rss" text="https://danluu.com/atom.xml" xmlUrl="https://danluu.com/atom.xml" htmlUrl="https://danluu.com/atom.xml" />
                    <outline type="rss" text="IT_Fettchen" xmlUrl="https://it-fettchen.de/rss/" htmlUrl="https://it-fettchen.micro.blog/" />
                  </outline>
                  <outline type="rss" text="WIRED" xmlUrl="https://www.wired.com/feed/rss" htmlUrl="https://www.wired.com/feed/rss" />
                </outline>
            """;
    private OPMLService service;
    private FolderService folderService;
    private FeedService feedService;


    @BeforeEach
    void setUp() {
        folderService = mock(FolderService.class);
        feedService = mock(FeedService.class);

        service = new OPMLService(folderService, feedService);
    }

    @Test
    void importFeedTest() throws XMLStreamException {
        var firstFeed = new Feed(-1, 0, "Deutsche Welle: DW.com Deutsch", "https://rss.dw.com/xml/rss-de-all", "https://rss.dw.com/xml/rss-de-all");
        var secondFeed = new Feed(-1, 0, "WIRED", "https://www.wired.com/feed/rss", "https://www.wired.com/feed/rss");

        String xml = opmlStart + feeds + opmlEnd;
        InputStream stream = new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset()));

        service.importOPML(stream);

        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedService, atLeast(2)).create(feedCaptor.capture());
        List<Feed> capturedFeeds = feedCaptor.getAllValues();

        assertEquals(2, capturedFeeds.size());
        assertTrue(capturedFeeds.contains(firstFeed), "FeedService should have been called with firstFeed");
        assertTrue(capturedFeeds.contains(secondFeed), "FeedService should have been called with secondFeed");
    }

    @Test
    void importSingleFolderTest() throws XMLStreamException {
        var firstFeed = new Feed(-1, 1, "Deutsche Welle: DW.com Deutsch", "https://rss.dw.com/xml/rss-de-all", "https://rss.dw.com/xml/rss-de-all");
        var secondFeed = new Feed(-1, 1, "WIRED", "https://www.wired.com/feed/rss", "https://www.wired.com/feed/rss");
        var firstFolder = new Folder(-1, "folder1", null, "f-base");

        String xml = opmlStart + folder1 + opmlEnd;
        InputStream stream = new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset()));

        when(folderService.create(firstFolder)).thenReturn(1);

        service.importOPML(stream);

        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedService, atLeast(2)).create(feedCaptor.capture());
        List<Feed> capturedFeeds = feedCaptor.getAllValues();

        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        verify(folderService, atLeast(1)).create(folderCaptor.capture());
        List<Folder> capturedFolders = folderCaptor.getAllValues();

        assertEquals(1, capturedFolders.size());
        assertEquals(2, capturedFeeds.size());
        assertTrue(capturedFeeds.contains(firstFeed), "FeedService should have been called with firstFeed");
        assertTrue(capturedFeeds.contains(secondFeed), "FeedService should have been called with secondFeed");
        assertTrue(capturedFolders.contains(firstFolder), "FolderService should have been called with firstFolder");

    }

    @Test
    void importMultipleTest() throws XMLStreamException {
        var firstFeed = new Feed(-1, 1, "Deutsche Welle: DW.com Deutsch", "https://rss.dw.com/xml/rss-de-all", "https://rss.dw.com/xml/rss-de-all");
        var secondFeed = new Feed(-1, 1, "WIRED", "https://www.wired.com/feed/rss", "https://www.wired.com/feed/rss");
        var thirdFeed = new Feed(-1, 2, "https://danluu.com/atom.xml", "https://danluu.com/atom.xml", "https://danluu.com/atom.xml");
        var fourthFeed = new Feed(-1, 2, "IT_Fettchen", "https://it-fettchen.micro.blog/", "https://it-fettchen.de/rss/");
        var firstFolder = new Folder(-1, "folder1", null, "f-base");
        var secondFolder = new Folder(-1, "folder2", null, "f-base");

        String xml = opmlStart + folder1 + folder2 + opmlEnd;
        InputStream stream = new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset()));

        when(folderService.create(firstFolder)).thenReturn(1);
        when(folderService.create(secondFolder)).thenReturn(2);

        service.importOPML(stream);

        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedService, atLeast(4)).create(feedCaptor.capture());
        List<Feed> capturedFeeds = feedCaptor.getAllValues();

        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        verify(folderService, atLeast(2)).create(folderCaptor.capture());
        List<Folder> capturedFolders = folderCaptor.getAllValues();

        assertEquals(2, capturedFolders.size());
        assertEquals(4, capturedFeeds.size());
        assertTrue(capturedFeeds.contains(firstFeed), "FeedService should have been called with firstFeed");
        assertTrue(capturedFeeds.contains(secondFeed), "FeedService should have been called with secondFeed");
        assertTrue(capturedFeeds.contains(thirdFeed), "FeedService should have been called with thirdFeed");
        assertTrue(capturedFeeds.contains(fourthFeed), "FeedService should have been called with fourthFeed");
        assertTrue(capturedFolders.contains(firstFolder), "FolderService should have been called with firstFolder");
        assertTrue(capturedFolders.contains(secondFolder), "FolderService should have been called with secondFolder");
    }

    @Test
    void importNestedTest() throws XMLStreamException {
        var firstFeed = new Feed(-1, 1, "Deutsche Welle: DW.com Deutsch", "https://rss.dw.com/xml/rss-de-all", "https://rss.dw.com/xml/rss-de-all");
        var secondFeed = new Feed(-1, 1, "WIRED", "https://www.wired.com/feed/rss", "https://www.wired.com/feed/rss");
        var thirdFeed = new Feed(-1, 2, "https://danluu.com/atom.xml", "https://danluu.com/atom.xml", "https://danluu.com/atom.xml");
        var fourthFeed = new Feed(-1, 2, "IT_Fettchen", "https://it-fettchen.micro.blog/", "https://it-fettchen.de/rss/");
        var outer = new Folder(-1, "outer", null, "f-base");
        var inner = new Folder(-1, "inner", null, "f-base");

        String xml = opmlStart + folderInFolder + opmlEnd;
        InputStream stream = new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset()));

        when(folderService.create(outer)).thenReturn(1);
        when(folderService.create(inner)).thenReturn(2);

        service.importOPML(stream);

        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedService, atLeast(4)).create(feedCaptor.capture());
        List<Feed> capturedFeeds = feedCaptor.getAllValues();

        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        verify(folderService, atLeast(2)).create(folderCaptor.capture());
        List<Folder> capturedFolders = folderCaptor.getAllValues();

        assertEquals(2, capturedFolders.size());
        assertEquals(4, capturedFeeds.size());
        assertTrue(capturedFeeds.contains(firstFeed), "FeedService should have been called with firstFeed");
        assertTrue(capturedFeeds.contains(secondFeed), "FeedService should have been called with secondFeed");
        assertTrue(capturedFeeds.contains(thirdFeed), "FeedService should have been called with thirdFeed");
        assertTrue(capturedFeeds.contains(fourthFeed), "FeedService should have been called with fourthFeed");
        assertTrue(capturedFolders.contains(outer), "FolderService should have been called with firstFolder");
        assertTrue(capturedFolders.contains(inner), "FolderService should have been called with secondFolder");
    }
}
