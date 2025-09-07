package de._0x2b.services;

import de._0x2b.models.Feed;
import de._0x2b.models.Folder;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.util.Stack;

record OutlineContext(String type, Integer folderId) {
}

public class OPMLService {
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
                    var name = workingEvent.getAttributeByName(ATTR_TEXT).getValue();
                    var folderId = folderService.create(new Folder(-1, name, null, "f-base"));
                    contextStack.push(new OutlineContext("folder", folderId));
                } else if ("rss".equals(type.getValue())) {
                    // rss feed
                    var name = workingEvent.getAttributeByName(ATTR_TEXT).getValue();
                    var xmlUrl = workingEvent.getAttributeByName(ATTR_XMLURL).getValue();
                    var htmlUrl = workingEvent.getAttributeByName(ATTR_HTMLURL).getValue();
                    feedService.create(new Feed(-1, contextStack.peek().folderId(), name, htmlUrl, xmlUrl));
                    contextStack.push(new OutlineContext("feed", null)); // feeds don't open new folders
                } else {
                    // not implemented type
                    System.out.println("Found not implemented feed type " + type.getValue());
                    contextStack.push(new OutlineContext("feed", null));

                }
            } else if (event.isEndElement() && "outline".equals(event.asEndElement().getName().getLocalPart())) {
                contextStack.pop();
            }
        }
    }
}
