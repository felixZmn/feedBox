package de._0x2b.services;

import de._0x2b.models.Feed;
import de._0x2b.models.Folder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
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

    private final static String opmlExport = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <opml version="2.0">
              <head>
                <title>My Feeds</title>
              </head>
              <body>
                <outline htmlUrl="https://www.dw.com/de" text="Deutsche Welle: DW.com Deutsch" type="rss" xmlUrl="https://feed.dw.com/de"/>
                <outline text="tech">
                  <outline htmlUrl="https://arstechnica.com" text="Arstechnica" type="rss" xmlUrl="https://feed.arstechnica.com"/>
                  <outline htmlUrl="https://hackaday.com" text="Hackaday" type="rss" xmlUrl="https://feed.hackaday.com"/>
                </outline>
                <outline text="sports">
                  <outline htmlUrl="https://www.theguardian.com/sport" text="NFL | The Guardian" type="rss" xmlUrl="https://feed.theguardian.com/sport"/>
                  <outline htmlUrl="https://www.espn.com/nfl" text="www.espn.com - NFL" type="rss" xmlUrl="https://feed.espn.com/nfl"/>
                </outline>
              </body>
            </opml>
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
        var firstFeed = new Feed(-1, 0, "Deutsche Welle: DW.com Deutsch", URI.create("https://rss.dw.com/xml/rss-de-all"), URI.create("https://rss.dw.com/xml/rss-de-all"));
        var secondFeed = new Feed(-1, 0, "WIRED", URI.create("https://www.wired.com/feed/rss"), URI.create("https://www.wired.com/feed/rss"));

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
        var firstFeed = new Feed(-1, 1, "Deutsche Welle: DW.com Deutsch", URI.create("https://rss.dw.com/xml/rss-de-all"), URI.create("https://rss.dw.com/xml/rss-de-all"));
        var secondFeed = new Feed(-1, 1, "WIRED", URI.create("https://www.wired.com/feed/rss"), URI.create("https://www.wired.com/feed/rss"));
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
        var firstFeed = new Feed(-1, 1, "Deutsche Welle: DW.com Deutsch", URI.create("https://rss.dw.com/xml/rss-de-all"), URI.create("https://rss.dw.com/xml/rss-de-all"));
        var secondFeed = new Feed(-1, 1, "WIRED", URI.create("https://www.wired.com/feed/rss"), URI.create("https://www.wired.com/feed/rss"));
        var thirdFeed = new Feed(-1, 2, "https://danluu.com/atom.xml", URI.create("https://danluu.com/atom.xml"), URI.create("https://danluu.com/atom.xml"));
        var fourthFeed = new Feed(-1, 2, "IT_Fettchen", URI.create("https://it-fettchen.micro.blog/"), URI.create("https://it-fettchen.de/rss/"));
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
        var firstFeed = new Feed(-1, 1, "Deutsche Welle: DW.com Deutsch", URI.create("https://rss.dw.com/xml/rss-de-all"), URI.create("https://rss.dw.com/xml/rss-de-all"));
        var secondFeed = new Feed(-1, 1, "WIRED", URI.create("https://www.wired.com/feed/rss"), URI.create("https://www.wired.com/feed/rss"));
        var thirdFeed = new Feed(-1, 2, "https://danluu.com/atom.xml", URI.create("https://danluu.com/atom.xml"), URI.create("https://danluu.com/atom.xml"));
        var fourthFeed = new Feed(-1, 2, "IT_Fettchen", URI.create("https://it-fettchen.micro.blog/"), URI.create("https://it-fettchen.de/rss/"));
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

    @Test
    void exportFeedTest() throws ParserConfigurationException, TransformerException {
        List<Feed> techFeeds = new ArrayList<>();
        List<Feed> sportsFeeds = new ArrayList<>();
        List<Feed> rootFeeds = new ArrayList<>();
        List<Folder> folders = new ArrayList<>();

        rootFeeds.add(new Feed(1, 0, "Deutsche Welle: DW.com Deutsch", URI.create("https://www.dw.com/de"), URI.create("https://feed.dw.com/de")));
        techFeeds.add(new Feed(2, 1, "Arstechnica", URI.create("https://arstechnica.com"), URI.create("https://feed.arstechnica.com")));
        techFeeds.add(new Feed(3, 1, "Hackaday", URI.create("https://hackaday.com"), URI.create("https://feed.hackaday.com")));
        sportsFeeds.add(new Feed(4, 2, "NFL | The Guardian", URI.create("https://www.theguardian.com/sport"), URI.create("https://feed.theguardian.com/sport")));
        sportsFeeds.add(new Feed(5, 2, "www.espn.com - NFL", URI.create("https://www.espn.com/nfl"), URI.create("https://feed.espn.com/nfl")));

        folders.add(new Folder(0, "", rootFeeds, "f-base"));
        folders.add(new Folder(1, "tech", techFeeds, "f-base"));
        folders.add(new Folder(2, "sports", sportsFeeds, "f-base"));

        assertEquals(opmlExport, OPMLService.documentToString(service.createOPML(folders)));
    }
}
