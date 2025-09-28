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
import java.util.List;
import java.util.Stack;

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

    public void importOPML(InputStream stream) throws XMLStreamException {
        logger.debug("importOPML");
        XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(stream);

        Stack<OutlineContext> contextStack = new Stack<>();
        contextStack.push(new OutlineContext("root", 0));

        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement() && "outline".equals(event.asStartElement().getName().getLocalPart())) {
                var workingEvent = event.asStartElement();

                var type = workingEvent.getAttributeByName(ATTR_TYPE);
                if (type == null) {
                    // folder
                    int folderId;
                    var name = workingEvent.getAttributeByName(ATTR_TEXT).getValue();
                    try {
                        folderId = folderService.create(new Folder(-1, name, null, "f-base"));
                    } catch (DuplicateEntityException e){
                        // ignore & query id
                        folderId = folderService.findByName(name).getFirst().getId();
                    }
                    contextStack.push(new OutlineContext("folder", folderId));
                } else if ("rss".equals(type.getValue())) {
                    // rss feed
                    var name = workingEvent.getAttributeByName(ATTR_TEXT).getValue();
                    var xmlUrl = workingEvent.getAttributeByName(ATTR_XMLURL).getValue();
                    var htmlUrl = workingEvent.getAttributeByName(ATTR_HTMLURL).getValue();
                    try {
                        feedService.create(new Feed(-1, contextStack.peek().folderId(), name, htmlUrl, xmlUrl));
                    } catch (DuplicateEntityException e){
                        // ignore
                    }
                    contextStack.push(new OutlineContext("feed", null)); // feeds don't open new folders
                } else {
                    // not implemented type
                    logger.info("Found not implemented feed type {}", type.getValue());
                    contextStack.push(new OutlineContext("feed", null));
                }
            } else if (event.isEndElement() && "outline".equals(event.asEndElement().getName().getLocalPart())) {
                contextStack.pop();
            }
        }
    }

    public String exportOpml(){
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

    private Element createFolderElement(Document doc, Folder folder){
        var el = doc.createElement("outline");
        el.setAttribute("text", folder.getName());
        return el;
    }

    private Element createFeedElement(Document doc, Feed feed){
        var el = doc.createElement("outline");
        el.setAttribute("text", feed.getName());
        el.setAttribute("type", "rss");
        el.setAttribute("xmlUrl", feed.getFeedUrl());
        el.setAttribute("htmlUrl", feed.getUrl());
        return el;
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
}
