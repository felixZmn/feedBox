package de._0x2b.service;

import de._0x2b.exception.DuplicateEntityException;
import de._0x2b.model.Feed;
import de._0x2b.model.Folder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

record OutlineContext(String type, Integer folderId) {
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

    /**
     * Convert a Document to a formatted String
     * 
     * @param document
     * @return
     * @throws TransformerException
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
     * Import OPML from InputStream
     *
     * @param stream
     * @throws XMLStreamException
     * @throws InterruptedException
     */
    public void importOPML(InputStream stream) throws XMLStreamException, InterruptedException {
        logger.debug("importOPML");

        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLEventReader reader = xmlFactory.createXMLEventReader(stream);

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Deque<OutlineContext> contextStack = new ArrayDeque<>();
            contextStack.push(new OutlineContext("root", 0));

            while (reader.hasNext()) {
                var event = reader.nextEvent();

                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    if (!"outline".equals(startElement.getName().getLocalPart())) {
                        continue;
                    }
                    handleStartOutline(startElement, contextStack, pool);

                } else if (event.isEndElement()) {
                    var endElement = event.asEndElement();
                    if (!"outline".equals(endElement.getName().getLocalPart())) {
                        continue;
                    }
                    handleEndOutline(contextStack);
                }
            }

            pool.shutdown();
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Timeout waiting for feed creation tasks; cancelling remaining tasks.");
                pool.shutdownNow();
                // Restore interrupt status so callers can react
                Thread.currentThread().interrupt();
            }

        } catch (XMLStreamException | InterruptedException e) {
            logger.error("Exception during OPML import: {}", e.getMessage(), e);
            throw e;
        } finally {
            reader.close();
        }
    }

    private void handleStartOutline(StartElement startElement, Deque<OutlineContext> contextStack,
            ExecutorService pool) {
        var typeAttr = startElement.getAttributeByName(ATTR_TYPE);

        if (typeAttr == null) {
            handleFolderOutline(startElement, contextStack);
        } else if ("rss".equals(typeAttr.getValue())) {
            handleFeedOutline(startElement, contextStack, pool);
        } else {
            logger.info("Unsupported outline type '{}'; skipping.", typeAttr.getValue());
            // Still push so the matching end element pop is balanced
            contextStack.push(new OutlineContext("unknown", null));
        }
    }

    private void handleFolderOutline(StartElement startElement, Deque<OutlineContext> contextStack) {
        var textAttr = startElement.getAttributeByName(ATTR_TEXT);
        if (textAttr == null) {
            logger.warn("Folder outline missing 'text' attribute; skipping.");
            // Push placeholder so end-element pop stays balanced
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
            ExecutorService pool) {
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

        pool.submit(() -> {
            try {
                feedService.create(new Feed(-1, parentFolderId, name, htmlUrl, xmlUrl));
            } catch (DuplicateEntityException e) {
                logger.debug("Feed '{}' already exists; ignoring duplicate.", name);
            } catch (Exception ex) {
                logger.error("Exception creating feed '{}': {}", name, ex.getMessage(), ex);
            }
        });

        // Push so the matching end-element pop is balanced
        contextStack.push(new OutlineContext("feed", null));
    }

    private void handleEndOutline(Deque<OutlineContext> contextStack) {
        if (contextStack.size() <= 1) {
            // Never pop the root sentinel
            logger.warn("End outline encountered but only root context remains; ignoring.");
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

    /**
     * Safely parses a URI, returning null instead of throwing on malformed input.
     */
    private URI parseUri(String value) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException e) {
            logger.warn("Malformed URI '{}': {}", value, e.getMessage());
            return null;
        }
    }

    /**
     * Export OPML as String, for example for download
     * 
     * @return
     */
    public String exportOpml() {
        var result = folderService.findAll();
        Document doc = null;
        try {
            doc = createOPML(result);
            return documentToString(doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create OPML Document from folders and feeds
     * 
     * @param folders list of folders to include in the OPML
     * @return Document object representing the OPML
     * @throws ParserConfigurationException
     */
    public Document createOPML(List<Folder> folders) throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // root elements
        Document doc = docBuilder.newDocument();
        Element opml = doc.createElement("opml");
        opml.setAttribute("version", "2.0");
        doc.appendChild(opml);

        // head
        Element head = doc.createElement("head");
        Element title = doc.createElement("title");
        title.appendChild(doc.createTextNode("My Feeds"));
        head.appendChild(title);
        opml.appendChild(head);

        // body
        Element body = doc.createElement("body");
        opml.appendChild(body);

        for (Folder folder : folders) {
            if (folder.getId() == 0) {
                // Add all feeds directly under <body> without folder element
                for (Feed feed : folder.getFeeds()) {
                    Element feedOutline = createFeedElement(doc, feed);
                    body.appendChild(feedOutline);
                }
            } else {
                // Normal case: add folder as outline with feeds inside
                Element folderOutline = createFolderElement(doc, folder);
                body.appendChild(folderOutline);
                for (Feed feed : folder.getFeeds()) {
                    Element feedOutline = createFeedElement(doc, feed);
                    folderOutline.appendChild(feedOutline);
                }
            }
        }

        return doc;
    }

    /**
     * Create folder outline element
     * 
     * @param doc
     * @param folder
     * @return
     */
    private Element createFolderElement(Document doc, Folder folder) {
        var el = doc.createElement("outline");
        el.setAttribute("text", folder.getName());
        return el;
    }

    /**
     * Create feed outline element
     * 
     * @param doc
     * @param feed
     * @return
     */
    private Element createFeedElement(Document doc, Feed feed) {
        var el = doc.createElement("outline");
        el.setAttribute("text", feed.getName());
        el.setAttribute("type", "rss");
        el.setAttribute("xmlUrl", feed.getFeedUrl().toString());
        el.setAttribute("htmlUrl", feed.getUrl().toString());
        return el;
    }
}
