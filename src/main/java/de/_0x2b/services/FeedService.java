package de._0x2b.services;

import com.apptasticsoftware.rssreader.RssReader;
import de._0x2b.models.Article;
import de._0x2b.models.Feed;
import de._0x2b.repositories.ArticleRepository;
import de._0x2b.repositories.FeedRepository;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeedService {
    private final FeedRepository feedRepository;
    private final ArticleRepository articleRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    public FeedService(FeedRepository feedRepository, ArticleRepository articleRepository) {
        this.feedRepository = feedRepository;
        this.articleRepository = articleRepository;
    }

    private static InputStream fetchFeed(Feed feed, HttpClient client) {
        try {
            var request = HttpRequest.newBuilder().uri(URI.create(feed.getFeedUrl())).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("Failed: " + feed.getFeedUrl() + " -> " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("[" + feed.getFeedUrl() + "]");
            e.printStackTrace();
        }
        return null; // ToDo: care
    }


    private static FeedType detectFeedType(XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            var event = reader.peek();
            if (event.isStartElement()) {
                var start = event.asStartElement();
                QName qName = start.getName();
                String local = qName.getLocalPart();
                String nsUri = qName.getNamespaceURI();

                if ("rss".equalsIgnoreCase(local)) {
                    return FeedType.RSS;
                }
                if ("feed".equalsIgnoreCase(local) && "http://www.w3.org/2005/Atom".equalsIgnoreCase(nsUri)) {
                    return FeedType.ATOM;
                }
                return FeedType.UNKNOWN;
            }
            reader.nextEvent(); // advance if not StartElement
        }
        return FeedType.UNKNOWN;
    }

    public int create(Feed feed) {
        return feedRepository.create(feed);
    }

    public int update(Feed feed) {
        return feedRepository.update(feed);
    }

    /**
     * Refresh all feeds in the database
     */
    public void refresh() {
        var feeds = feedRepository.findAll();
        //var feeds = feedRepository.findOne(3);
        refresh(feeds);
    }

    /**
     * Refresh a single feed by its ID
     *
     * @param id feed id
     */
    public void refresh(int id) {
        var feeds = feedRepository.findOne(id);
        refresh(feeds);
    }

    private void refresh(List<Feed> feeds) {
        try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()) {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                feeds.parallelStream().forEach(feed -> executor.submit(() -> {
                    try {
                        var result = fetchFeed(feed, client);
                        var articles = parseFeed(feed, result);
                        articleRepository.create(articles);
                    } catch (IOException | XMLStreamException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
        }
    }


    /**
     * Query a feed URL and return the filled feed object
     *
     * @param feed Feed object with feed_url filled
     * @return Feed object with name and url filled
     */
    public Feed query(Feed feed) {
        RssReader rssReader = new RssReader();
        try {
            var channel = rssReader.read(feed.getFeedUrl()).toList().getFirst().getChannel();
            feed.setUrl(channel.getLink());
            feed.setName(channel.getTitle());
        } catch (IOException e) {
            System.out.printf("Error querying feed [%s] \n %s\n", feed.getFeedUrl(), e.getMessage());
        }
        return feed;
    }

    public int deleteFeed(int feedId) {
        return feedRepository.delete(feedId);
    }


    private List<Article> parseFeed(Feed feed, InputStream stream) throws IOException, XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty("jdk.xml.maxGeneralEntitySizeLimit", 1_000_000);
        factory.setProperty("jdk.xml.totalEntitySizeLimit", 5_000_000);
        factory.setProperty("jdk.xml.entityExpansionLimit", 10_000);

        try (stream) {
            XMLEventReader reader = factory.createXMLEventReader(stream);

            var foo =  switch (detectFeedType(reader)) {
                case RSS -> parseRSSFeed(feed, reader);
                case ATOM -> new ArrayList<Article>();
                case UNKNOWN -> new ArrayList<Article>();
            };
            return foo;
        } catch (Exception e){
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static List<Article> parseRSSFeed(Feed feed, XMLEventReader reader) throws XMLStreamException {
        List<Article> articles = new ArrayList<>();
        Article article = null;
        Element currentElement = Element.UNKNOWN;
        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                currentElement = Element.fromQName(startElement.getName());

                if (currentElement == Element.ITEM) {
                    article = new Article();
                    article.setFeedId(feed.getId());
                    article.setFeedName(feed.getName());
                } else if (currentElement == Element.CONTENT) {
                    // image
                    var urlAttr = startElement.getAttributeByName(new QName("url"));
                    if (urlAttr != null) article.setImageUrl(urlAttr.getValue());
                }
            } else if (event.isCharacters()) {
                var chars = event.asCharacters();
                if (!chars.isWhiteSpace() && article != null) {
                    String text = chars.getData();
                    switch (currentElement) {
                        case TITLE:
                            article.setTitle(text);
                            break;
                        case CATEGORY:
                            article.addCategory(text);
                            break;
                        case CREATOR:
                            article.setAuthors(text);
                            break;
                        case PUBDATE:
                            article.setPublished(text);
                            break;
                        case LINK:
                            article.setLink(text);
                            break;
                        case DESCRIPTION:
                            article.setDescription(text);
                            break;
                        case ENCODED:
                            article.setContent(text);
                            break;
                        // case CONTENT: article.setImageUrl(text); break; // content is actually <media:content type="image/png"...
                    }
                }
            } else if (event.isEndElement()) {
                var end = Element.fromQName(event.asEndElement().getName());

                if (end == Element.ITEM && article != null) {
                    articles.add(article);
                    article = null;
                }
                currentElement = Element.UNKNOWN;
            }
        }
        return articles;
    }

    enum FeedType {RSS, ATOM, UNKNOWN}

    enum Element {
        ITEM, TITLE, CATEGORY, CREATOR, PUBDATE, LINK, GUID, DESCRIPTION, ENCODED, CONTENT, UNKNOWN;

        static Element fromQName(QName qName) {
            String local = qName.getLocalPart();
            return switch (local) {
                case "item" -> ITEM;
                case "title" -> TITLE;
                case "category" -> CATEGORY;
                case "creator" -> CREATOR;
                case "pubDate" -> PUBDATE;
                case "link" -> LINK;
                case "guid" -> GUID;
                case "description" -> DESCRIPTION;
                case "encoded" -> ENCODED;
                case "content" -> CONTENT;
                default -> UNKNOWN;
            };
        }
    }
}
