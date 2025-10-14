package de._0x2b.services;

import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.models.Feed;
import de._0x2b.models.Folder;
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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

record OutlineContext(String type, Integer folderId) {
}

public class OPMLService {
    private static final Logger logger = LoggerFactory.getLogger(OPMLService.class);
    private final QName ATTR_TYPE = new QName("type");
    private final QName ATTR_TEXT = new QName("text");
    private final QName ATTR_XMLURL = new QName("xmlUrl");
    private final QName ATTR_HTMLURL = new QName("htmlUrl");

    private final FolderService folderService;
    private final FeedService feedService;

    public OPMLService(FolderService folderService, FeedService feedService) {
        this.folderService = folderService;
        this.feedService = feedService;
    }

    public static String documentToString(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        StringWriter stringWriter = new StringWriter();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    public void importOPML(InputStream stream) throws XMLStreamException, InterruptedException {
        logger.debug("importOPML");

        XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(stream);

        try (ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)) {
            List<Future<?>> futures = new ArrayList<>();
            Stack<OutlineContext> contextStack = new Stack<>();
            contextStack.push(new OutlineContext("root", 0));

            while (reader.hasNext()) {
                var event = reader.nextEvent();

                if (event.isStartElement() && "outline".equals(event.asStartElement().getName().getLocalPart())) {
                    var startElement = event.asStartElement();
                    var typeAttr = startElement.getAttributeByName(ATTR_TYPE);

                    if (typeAttr == null) {
                        // Folder
                        var textAttr = startElement.getAttributeByName(ATTR_TEXT);
                        if (textAttr == null) {
                            logger.warn("Folder outline missing 'text' attribute; skipping this outline.");
                            continue;
                        }
                        var name = textAttr.getValue();
                        int folderId;

                        try {
                            folderId = folderService.create(new Folder(-1, name, null, "f-base"));
                        } catch (DuplicateEntityException e) {
                            logger.debug("Folder '{}' already exists; querying existing ID.", name);
                            var optFolder = folderService.findByName(name).stream().findFirst();
                            if (optFolder.isPresent()) {
                                folderId = optFolder.get().getId();
                            } else {
                                logger.error("Folder '{}' not found after DuplicateEntityException; skipping.", name);
                                continue;
                            }
                        }
                        contextStack.push(new OutlineContext("folder", folderId));
                    } else if ("rss".equals(typeAttr.getValue())) {
                        // rss feed
                        var textAttr = startElement.getAttributeByName(ATTR_TEXT);
                        var xmlUrlAttr = startElement.getAttributeByName(ATTR_XMLURL);
                        var htmlUrlAttr = startElement.getAttributeByName(ATTR_HTMLURL);

                        if (textAttr == null || xmlUrlAttr == null || htmlUrlAttr == null) {
                            logger.warn("RSS feed outline missing one of 'text', 'xmlUrl' or 'htmlUrl'; skipping.");
                            continue;
                        }

                        String name = textAttr.getValue();
                        URI xmlUrl = URI.create(xmlUrlAttr.getValue());
                        URI htmlUrl = URI.create(htmlUrlAttr.getValue());
                        Integer parentFolderId = contextStack.peek().folderId();

                        futures.add(pool.submit(() -> {
                            try {
                                feedService.create(new Feed(-1, parentFolderId, name, htmlUrl, xmlUrl));
                            } catch (DuplicateEntityException e) {
                                logger.debug("Feed '{}' already exists; ignoring duplicate.", name);
                            } catch (Exception ex) {
                                logger.error("Exception creating feed '{}': {}", name, ex.getMessage(), ex);
                            }
                        }));
                        // Feeds do NOT open new folder context, so no push.
                        contextStack.push(new OutlineContext("feed", null)); // feeds don't open new folders
                    } else {
                        logger.info("Found not implemented feed type '{}'; skipping.", typeAttr.getValue());
                        // No stack push for unimplemented types either
                        contextStack.push(new OutlineContext("feed", null));
                    }
                } else if (event.isEndElement() && "outline".equals(event.asEndElement().getName().getLocalPart())) {
                    if (!contextStack.isEmpty()) {
                        contextStack.pop();
                    } else {
                        logger.warn("Outline end element encountered but context stack was empty.");
                    }
                }
            }

            pool.shutdown();
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Timeout waiting for feed creation tasks, cancelling remaining tasks");
                pool.shutdownNow();
            }
        } catch (XMLStreamException | InterruptedException e) {
            logger.error("Exception during OPML import: {}", e.getMessage(), e);
            throw e;
        } finally {
            reader.close();
        }
    }

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

    private Element createFolderElement(Document doc, Folder folder) {
        var el = doc.createElement("outline");
        el.setAttribute("text", folder.getName());
        return el;
    }

    private Element createFeedElement(Document doc, Feed feed) {
        var el = doc.createElement("outline");
        el.setAttribute("text", feed.getName());
        el.setAttribute("type", "rss");
        el.setAttribute("xmlUrl", feed.getFeedURI().toString());
        el.setAttribute("htmlUrl", feed.getURI().toString());
        return el;
    }
}
