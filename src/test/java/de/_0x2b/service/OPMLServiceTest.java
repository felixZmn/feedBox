package de._0x2b.service;

import de._0x2b.exception.DuplicateEntityException;
import de._0x2b.model.Feed;
import de._0x2b.model.Folder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OPMLServiceTest {

    @Mock
    FolderService folderService;
    @Mock
    FeedService feedService;

    @InjectMocks
    OPMLService sut;

    @Test
    void importOPML_createsFoldersAndFeeds() throws Exception {
        String opml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <opml version="2.0">
                  <body>
                    <outline text="Tech">
                      <outline type="rss" text="HN"
                               xmlUrl="https://news.ycombinator.com/rss"
                               htmlUrl="https://news.ycombinator.com/"/>
                    </outline>
                  </body>
                </opml>
                """;

        // folder created successfully -> id 123
        when(folderService.create(any(Folder.class))).thenReturn(123);

        sut.importOPML(new ByteArrayInputStream(opml.getBytes(StandardCharsets.UTF_8)));

        // Verify folder creation
        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        verify(folderService, times(1)).create(folderCaptor.capture());
        assertEquals("Tech", folderCaptor.getValue().getName());

        // Verify feed creation (async): parentFolderId should be 123
        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedService, timeout(1000).times(1)).create(feedCaptor.capture());

        Feed created = feedCaptor.getValue();
        assertEquals(Integer.valueOf(123), created.getFolderId());
        assertEquals("HN", created.getName());
        assertEquals("https://news.ycombinator.com/", created.getUrl().toString());
        assertEquals("https://news.ycombinator.com/rss", created.getFeedUrl().toString());
    }

    @Test
    void importOPML_whenFolderAlreadyExists_usesFindByNameId() throws Exception {
        String opml = """
                <opml version="2.0">
                  <body>
                    <outline text="Tech">
                      <outline type="rss" text="HN"
                               xmlUrl="https://news.ycombinator.com/rss"
                               htmlUrl="https://news.ycombinator.com/"/>
                    </outline>
                  </body>
                </opml>
                """;

        when(folderService.create(any(Folder.class))).thenThrow(new DuplicateEntityException("dup"));
        when(folderService.findByName("Tech")).thenReturn(List.of(new Folder(55, "Tech", null, "f-base")));

        sut.importOPML(new ByteArrayInputStream(opml.getBytes(StandardCharsets.UTF_8)));

        ArgumentCaptor<Feed> feedCaptor = ArgumentCaptor.forClass(Feed.class);
        verify(feedService, timeout(1000)).create(feedCaptor.capture());
        assertEquals(Integer.valueOf(55), feedCaptor.getValue().getFolderId());
    }

    @Test
    void importOPML_whenDuplicateFeedCreationOccurs_isIgnored() throws Exception {
        String opml = """
                <opml version="2.0">
                  <body>
                    <outline text="Tech">
                      <outline type="rss" text="HN"
                               xmlUrl="https://news.ycombinator.com/rss"
                               htmlUrl="https://news.ycombinator.com/"/>
                    </outline>
                  </body>
                </opml>
                """;

        when(folderService.create(any(Folder.class))).thenReturn(1);
        doThrow(new DuplicateEntityException("dup")).when(feedService).create(any(Feed.class));

        // should not throw
        sut.importOPML(new ByteArrayInputStream(opml.getBytes(StandardCharsets.UTF_8)));

        verify(feedService, timeout(1000)).create(any(Feed.class));
    }

    @Test
    void importOPML_skipsFeedOutlineWithMalformedUrls() throws Exception {
        String opml = """
                <opml version="2.0">
                  <body>
                    <outline text="Tech">
                      <outline type="rss" text="Bad"
                               xmlUrl="ht!tp://bad"
                               htmlUrl="https://ok.example/"/>
                    </outline>
                  </body>
                </opml>
                """;

        when(folderService.create(any(Folder.class))).thenReturn(1);

        sut.importOPML(new ByteArrayInputStream(opml.getBytes(StandardCharsets.UTF_8)));

        // feed should be skipped, so never created
        verify(feedService, after(300).never()).create(any());
    }

    @Test
    void importOPML_skipsFeedOutlineMissingRequiredAttributes() throws Exception {
        String opml = """
                <opml version="2.0">
                  <body>
                    <outline text="Tech">
                      <outline type="rss" text="MissingXmlUrl"
                               htmlUrl="https://example.com/"/>
                    </outline>
                  </body>
                </opml>
                """;

        when(folderService.create(any(Folder.class))).thenReturn(1);

        sut.importOPML(new ByteArrayInputStream(opml.getBytes(StandardCharsets.UTF_8)));

        verify(feedService, after(300).never()).create(any());
    }

    @Test
    void exportOpml_returnsXmlString() {
        Folder root = new Folder(0, "root", null, "f-base");
        root.setFeeds(List.of(
                new Feed(1, 0, "A", java.net.URI.create("https://a.example"), java.net.URI.create("https://a.example/rss"))
        ));

        when(folderService.findAll()).thenReturn(List.of(root));

        String xml = sut.exportOpml();

        assertNotNull(xml);
        assertTrue(xml.contains("<opml"));
        assertTrue(xml.contains("xmlUrl=\"https://a.example/rss\""));
        assertTrue(xml.contains("htmlUrl=\"https://a.example\""));
    }

    @Test
    void createOPML_buildsBodyWithRootFeedsDirectlyAndFoldersNested() throws Exception {
        Folder root = new Folder(0, "root", null, "f-base");
        root.setFeeds(List.of(
                new Feed(1, 0, "RootFeed",
                        java.net.URI.create("https://root.example"),
                        java.net.URI.create("https://root.example/rss"))
        ));

        Folder tech = new Folder(10, "Tech", null, "f-base");
        tech.setFeeds(List.of(
                new Feed(2, 10, "HN",
                        java.net.URI.create("https://news.ycombinator.com/"),
                        java.net.URI.create("https://news.ycombinator.com/rss"))
        ));

        Document doc = sut.createOPML(List.of(root, tech));

        Element opml = doc.getDocumentElement();
        assertEquals("opml", opml.getTagName());

        NodeList bodies = opml.getElementsByTagName("body");
        assertEquals(1, bodies.getLength());

        Element body = (Element) bodies.item(0);

        // body should contain:
        // - 1 feed outline directly (RootFeed)
        // - 1 folder outline (Tech) containing its feed
        NodeList outlines = body.getElementsByTagName("outline");

        // There will be 3 outlines total in the subtree: RootFeed, Tech(folder), HN(feed inside Tech)
        assertEquals(3, outlines.getLength());

        // Assert root feed is present somewhere and is type=rss
        assertTrue(containsFeedOutline(doc, "RootFeed", "https://root.example/rss", "https://root.example"));

        // Assert folder outline exists
        assertTrue(containsFolderOutline(doc, "Tech"));

        // Assert nested feed exists
        assertTrue(containsFeedOutline(doc, "HN", "https://news.ycombinator.com/rss", "https://news.ycombinator.com/"));
    }

    @Test
    void documentToString_outputsIndentedXml() throws Exception {
        Folder root = new Folder(0, "root", null, "f-base");
        root.setFeeds(List.of());

        Document doc = sut.createOPML(List.of(root));

        String s = OPMLService.documentToString(doc);

        assertNotNull(s);
        assertTrue(s.contains("<?xml") || s.contains("<opml"));
        assertTrue(s.contains("\n"), "Should be formatted with newlines/indentation");
    }

    private static boolean containsFolderOutline(Document doc, String folderName) {
        NodeList outlines = doc.getElementsByTagName("outline");
        for (int i = 0; i < outlines.getLength(); i++) {
            Element el = (Element) outlines.item(i);
            if (folderName.equals(el.getAttribute("text")) && !el.hasAttribute("type")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsFeedOutline(Document doc, String name, String xmlUrl, String htmlUrl) {
        NodeList outlines = doc.getElementsByTagName("outline");
        for (int i = 0; i < outlines.getLength(); i++) {
            Element el = (Element) outlines.item(i);
            if (!"rss".equals(el.getAttribute("type"))) continue;
            if (name.equals(el.getAttribute("text"))
                    && xmlUrl.equals(el.getAttribute("xmlUrl"))
                    && htmlUrl.equals(el.getAttribute("htmlUrl"))) {
                return true;
            }
        }
        return false;
    }
}