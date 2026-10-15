package de._0x2b.service;

import de._0x2b.exception.DuplicateEntityException;
import de._0x2b.job.ProgressCallback;
import de._0x2b.model.Feed;
import de._0x2b.model.Folder;
import de._0x2b.model.FolderTree;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

record OutlineContext(String type, Integer folderId) {
}

record PendingFeed(String name, URI htmlUrl, URI xmlUrl, Integer folderId) {
}

@ApplicationScoped
public class OPMLService {
    private static final Logger logger = LoggerFactory.getLogger(OPMLService.class);
    private static final QName ATTR_TYPE = new QName("type");
    private static final QName ATTR_TEXT = new QName("text");
    private static final QName ATTR_XMLURL = new QName("xmlUrl");
    private static final QName ATTR_HTMLURL = new QName("htmlUrl");

    @Inject
    FolderService folderService;
    @Inject
    FeedService feedService;

    @ConfigProperty(name = "import.concurrency", defaultValue = "5")
    int importConcurrency = 5;

    /**
     * Convert a Document to a formatted String
     */
    public static String documentToString(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        StringWriter stringWriter = new StringWriter();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    /**
     * Import OPML from InputStream (blocking, no progress).
     */
    public void importOPML(InputStream stream) throws XMLStreamException {
        importOPML(stream, ProgressCallback.NONE);
    }

    /**
     * Import OPML with bounded concurrency and optional progress reporting.
     * Folders are created synchronously while parsing; feeds are created afterward
     * with at most {@code import.concurrency} parallel creates.
     */
    public void importOPML(InputStream stream, ProgressCallback progress) throws XMLStreamException {
        logger.debug("importOPML");
        ProgressCallback callback = progress != null ? progress : ProgressCallback.NONE;

        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLEventReader reader = xmlFactory.createXMLEventReader(stream);

        List<PendingFeed> pendingFeeds = new ArrayList<>();
        try {
            Deque<OutlineContext> contextStack = new ArrayDeque<>();
            contextStack.push(new OutlineContext("body", null));

            while (reader.hasNext()) {
                var event = reader.nextEvent();

                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    if (!"outline".equals(startElement.getName().getLocalPart())) {
                        continue;
                    }
                    handleStartOutline(startElement, contextStack, pendingFeeds);

                } else if (event.isEndElement()) {
                    var endElement = event.asEndElement();
                    if (!"outline".equals(endElement.getName().getLocalPart())) {
                        continue;
                    }
                    handleEndOutline(contextStack);
                }
            }
        } finally {
            reader.close();
        }

        createFeedsBounded(pendingFeeds, callback);
    }

    private void createFeedsBounded(List<PendingFeed> pendingFeeds, ProgressCallback progress) {
        progress.setTotal(pendingFeeds.size());
        if (pendingFeeds.isEmpty()) {
            return;
        }

        Semaphore semaphore = new Semaphore(Math.max(1, importConcurrency));
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = pendingFeeds.stream()
                    .map(pending -> CompletableFuture.runAsync(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                feedService.create(new Feed(
                                        -1,
                                        pending.folderId(),
                                        pending.name(),
                                        pending.htmlUrl(),
                                        pending.xmlUrl()));
                                progress.itemSucceeded();
                            } catch (DuplicateEntityException e) {
                                logger.debug("Feed '{}' already exists; ignoring duplicate.", pending.name());
                                progress.itemSucceeded();
                            } catch (Exception ex) {
                                logger.error("Exception creating feed '{}': {}", pending.name(), ex.getMessage(), ex);
                                progress.itemFailed();
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            progress.itemFailed();
                            logger.warn("OPML feed create interrupted for {}", pending.name(), e);
                        }
                    }, executor))
                    .toList();

            for (var future : futures) {
                try {
                    future.join();
                } catch (Exception e) {
                    logger.error("OPML feed create task failed", e);
                }
            }
        }
    }

    private void handleStartOutline(StartElement startElement, Deque<OutlineContext> contextStack,
            List<PendingFeed> pendingFeeds) {
        var typeAttr = startElement.getAttributeByName(ATTR_TYPE);

        if (typeAttr == null) {
            handleFolderOutline(startElement, contextStack);
        } else if ("rss".equals(typeAttr.getValue())) {
            handleFeedOutline(startElement, contextStack, pendingFeeds);
        } else {
            logger.info("Unsupported outline type '{}'; skipping.", typeAttr.getValue());
            contextStack.push(new OutlineContext("unknown", null));
        }
    }

    private void handleFolderOutline(StartElement startElement, Deque<OutlineContext> contextStack) {
        var textAttr = startElement.getAttributeByName(ATTR_TEXT);
        if (textAttr == null) {
            logger.warn("Folder outline missing 'text' attribute; skipping.");
            contextStack.push(new OutlineContext("invalid-folder", null));
            return;
        }

        String name = textAttr.getValue();
        int folderId = getOrCreateFolder(name);
        if (folderId < 0) {
            logger.error("Could not create or find folder '{}'; children will be placed in parent context.", name);
            contextStack.push(new OutlineContext("invalid-folder", null));
            return;
        }

        contextStack.push(new OutlineContext("folder", folderId));
    }

    private void handleFeedOutline(StartElement startElement, Deque<OutlineContext> contextStack,
            List<PendingFeed> pendingFeeds) {
        var textAttr = startElement.getAttributeByName(ATTR_TEXT);
        var xmlUrlAttr = startElement.getAttributeByName(ATTR_XMLURL);
        var htmlUrlAttr = startElement.getAttributeByName(ATTR_HTMLURL);

        if (textAttr == null || xmlUrlAttr == null || htmlUrlAttr == null) {
            logger.warn("RSS feed outline missing 'text', 'xmlUrl', or 'htmlUrl'; skipping.");
            contextStack.push(new OutlineContext("invalid-feed", null));
            return;
        }

        URI xmlUrl = parseUri(xmlUrlAttr.getValue());
        URI htmlUrl = parseUri(htmlUrlAttr.getValue());

        if (xmlUrl == null || htmlUrl == null) {
            logger.warn("RSS feed outline has malformed URL; skipping.");
            contextStack.push(new OutlineContext("invalid-feed", null));
            return;
        }

        String name = textAttr.getValue();
        Integer parentFolderId = contextStack.peek().folderId();
        pendingFeeds.add(new PendingFeed(name, htmlUrl, xmlUrl, parentFolderId));
        contextStack.push(new OutlineContext("feed", null));
    }

    private void handleEndOutline(Deque<OutlineContext> contextStack) {
        if (contextStack.size() <= 1) {
            return;
        }
        contextStack.pop();
    }

    /**
     * Creates a folder if it doesn't exist, or returns the ID of the existing one.
     *
     * @return folder ID, or -1 on unrecoverable failure
     */
    private int getOrCreateFolder(String name) {
        try {
            return folderService.create(new Folder(-1, name, null, "f-base"));
        } catch (DuplicateEntityException e) {
            logger.debug("Folder '{}' already exists; querying existing ID.", name);
            return folderService.findByName(name).stream()
                    .findFirst()
                    .map(Folder::getId)
                    .orElseGet(() -> {
                        logger.error("Folder '{}' not found after DuplicateEntityException.", name);
                        return -1;
                    });
        }
    }

    private URI parseUri(String value) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException e) {
            logger.warn("Malformed URI '{}': {}", value, e.getMessage());
            return null;
        }
    }

    public String exportOpml() {
        List<Folder> folders = folderService.findAll();
        List<Feed> feeds = feedService.findAll();
        FolderTree folderTree = FolderTree.from(folders, feeds);
        try {
            Document doc = createOPML(folderTree);
            return documentToString(doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Document createOPML(FolderTree folderTree) throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document doc = docBuilder.newDocument();
        Element opml = doc.createElement("opml");
        opml.setAttribute("version", "2.0");
        doc.appendChild(opml);

        Element head = doc.createElement("head");
        Element title = doc.createElement("title");
        title.appendChild(doc.createTextNode("My Feeds"));
        head.appendChild(title);
        opml.appendChild(head);

        Element body = doc.createElement("body");
        opml.appendChild(body);

        for (Feed feed : folderTree.getUnfiledFeeds()) {
            body.appendChild(createFeedElement(doc, feed));
        }

        for (Folder folder : folderTree.getFolders()) {
            Element folderOutline = createFolderElement(doc, folder);
            body.appendChild(folderOutline);
            for (Feed feed : folder.getFeeds()) {
                folderOutline.appendChild(createFeedElement(doc, feed));
            }
        }

        return doc;
    }

    private Element createFolderElement(Document doc, Folder folder) {
        var el = doc.createElement("outline");
        el.setAttribute("text", folder.getName());
        return el;
    }

    private Element createFeedElement(Document doc, Feed feed) {
        var el = doc.createElement("outline");
        el.setAttribute("text", feed.getName());
        el.setAttribute("type", "rss");
        el.setAttribute("xmlUrl", feed.getFeedUrl().toString());
        el.setAttribute("htmlUrl", feed.getUrl().toString());
        return el;
    }
}
