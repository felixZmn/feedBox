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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static de._0x2b.services.IconService.getCharsetFromContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class IconServiceTest {

    private OPMLService service;
    private FolderService folderService;
    private FeedService feedService;


    @BeforeEach
    void setUp() {
        folderService = mock(FolderService.class);
        feedService = mock(FeedService.class);

        service = new OPMLService(folderService, feedService);
    }

//    @Test
//    void importFeedTest() throws XMLStreamException, InterruptedException {
//        var firstFeed = new Feed(-1, 0, "Deutsche Welle: DW.com Deutsch", URI.create("https://rss.dw.com/xml/rss-de-all"), URI.create("https://rss.dw.com/xml/rss-de-all"));
//        var secondFeed = new Feed(-1, 0, "WIRED", URI.create("https://www.wired.com/feed/rss"), URI.create("https://www.wired.com/feed/rss"));
//
//        String xml = opmlStart + feeds + opmlEnd;
//        InputStream stream = new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset()));
//
//        service.importOPML(stream);
//
//        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
//        verify(feedService, atLeast(2)).create(feedCaptor.capture());
//        List<Feed> capturedFeeds = feedCaptor.getAllValues();
//
//        assertEquals(2, capturedFeeds.size());
//        assertTrue(capturedFeeds.contains(firstFeed), "FeedService should have been called with firstFeed");
//        assertTrue(capturedFeeds.contains(secondFeed), "FeedService should have been called with secondFeed");
//    }

    @Test
    void getCharsetFromContentTypeTest(){
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType(null));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("\"text/html; charset=UTF-8\""));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("text/javascript; charset=utf-8"));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("multipart/form-data; boundary=ExampleBoundaryString"));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("multipart/form-data; boundary=ExampleBoundaryString; charset=utf-8"));
        assertEquals(StandardCharsets.US_ASCII, getCharsetFromContentType("Content-Type: application/xml; charset=US-ASCII"));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("Content-Type: text/plain; charset=LATIN-DUMMY"));
    }



//    @Test
//    void importNestedTest() throws XMLStreamException, InterruptedException {
//        var firstFeed = new Feed(-1, 1, "Deutsche Welle: DW.com Deutsch", URI.create("https://rss.dw.com/xml/rss-de-all"), URI.create("https://rss.dw.com/xml/rss-de-all"));
//        var secondFeed = new Feed(-1, 1, "WIRED", URI.create("https://www.wired.com/feed/rss"), URI.create("https://www.wired.com/feed/rss"));
//        var thirdFeed = new Feed(-1, 2, "https://danluu.com/atom.xml", URI.create("https://danluu.com/atom.xml"), URI.create("https://danluu.com/atom.xml"));
//        var fourthFeed = new Feed(-1, 2, "IT_Fettchen", URI.create("https://it-fettchen.micro.blog/"), URI.create("https://it-fettchen.de/rss/"));
//        var outer = new Folder(-1, "outer", null, "f-base");
//        var inner = new Folder(-1, "inner", null, "f-base");
//
//        String xml = opmlStart + folderInFolder + opmlEnd;
//        InputStream stream = new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset()));
//
//        when(folderService.create(outer)).thenReturn(1);
//        when(folderService.create(inner)).thenReturn(2);
//
//        service.importOPML(stream);
//
//        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
//        verify(feedService, atLeast(4)).create(feedCaptor.capture());
//        List<Feed> capturedFeeds = feedCaptor.getAllValues();
//
//        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
//        verify(folderService, atLeast(2)).create(folderCaptor.capture());
//        List<Folder> capturedFolders = folderCaptor.getAllValues();
//
//        assertEquals(2, capturedFolders.size());
//        assertEquals(4, capturedFeeds.size());
//        assertTrue(capturedFeeds.contains(firstFeed), "FeedService should have been called with firstFeed");
//        assertTrue(capturedFeeds.contains(secondFeed), "FeedService should have been called with secondFeed");
//        assertTrue(capturedFeeds.contains(thirdFeed), "FeedService should have been called with thirdFeed");
//        assertTrue(capturedFeeds.contains(fourthFeed), "FeedService should have been called with fourthFeed");
//        assertTrue(capturedFolders.contains(outer), "FolderService should have been called with firstFolder");
//        assertTrue(capturedFolders.contains(inner), "FolderService should have been called with secondFolder");
//    }
}
