package de._0x2b.services;

import com.apptasticsoftware.rssreader.RssReader;
import com.apptasticsoftware.rssreader.module.mediarss.MediaRssItem;
import com.apptasticsoftware.rssreader.module.mediarss.MediaRssReader;
import de._0x2b.exceptions.NotFoundException;
import de._0x2b.models.Article;
import de._0x2b.models.Feed;
import de._0x2b.repositories.ArticleRepository;
import de._0x2b.repositories.FeedRepository;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;

public class FeedService {
    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);
    private final IconService iconService;
    private final FeedRepository feedRepository;
    private final ArticleRepository articleRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    public FeedService(IconService iconService, FeedRepository feedRepository, ArticleRepository articleRepository) {
        this.iconService = iconService;
        this.feedRepository = feedRepository;
        this.articleRepository = articleRepository;
    }

    public int create(Feed feed) throws NotFoundException {
        logger.debug("create");
        feed = this.getFeedMetadata(feed);
        feed.setId(feedRepository.create(feed));

        try {
            iconService.findIcon(feed);
        } catch (Exception e) {
            logger.warn("Could not find icon");
            // it can be the case that no icon is found or errors occour while searching for
            // one
            // as missing icons are a legimite case -> ignore
        }

        return feed.getId();
    }

    public int update(Feed feed) {
        logger.debug("update");
        return feedRepository.update(feed);
    }

    /**
     * Refresh all feeds in the database
     */
    public void refresh() {
        logger.debug("refresh");
        var feeds = feedRepository.findAll();
        refresh(feeds);
    }

    /**
     * Refresh a single feed by its ID
     *
     * @param id
     */
    public void refresh(int id) {
        logger.debug("refresh");
        var feeds = feedRepository.findOne(id);
        refresh(feeds);
    }

    private void refresh(List<Feed> feeds) {
        logger.info("Refreshing Feeds...");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            feeds.forEach(feed -> executor.submit(() -> parseFeed(feed)));
        }
        logger.info("Feeds refreshed!");
    }

    /**
     * Query a feed URL and return the filled feed object
     *
     * @param feed Feed object with feed_url filled
     * @return Feed object with name and url filled
     */
    public Feed getFeedMetadata(Feed feed) throws NotFoundException {
        logger.debug("getFeedMetadata");
        RssReader rssReader = new RssReader();
        try {
            var channel = rssReader.read(feed.getFeedURI().toString()).toList().getFirst().getChannel();
            feed.setURI(URI.create(channel.getLink()));
            feed.setName(channel.getTitle());
        } catch (Exception e) {
            logger.error("Error querying feed [{}] \n {}\n", feed.getFeedURI(), e.getMessage());
            throw new NotFoundException("Error creating feed " + feed.getFeedURI());
        }
        return feed;
    }

    public List<Feed> checkFeed(String url) {
        return checkFeed(url, new JsoupProvider());
    }

    protected List<Feed> checkFeed(String url, JsoupProvider provider) {
        logger.debug("checkFeed");
        List<Feed> validFeeds = new ArrayList<>();

        try {
            Connection.Response queryResponse = provider.execute(url);

            String body = queryResponse.body();
            String contentType = queryResponse.contentType();

            if (isFeed(contentType, body)) {
                logger.debug("URL is a direct feed: {}", url);
                validFeeds.add(new Feed(-1, -1, "", null, URI.create(url)));
            } else {
                Document doc = null;
                doc = queryResponse.parse();

                Elements links = doc.select(
                        "link[rel=alternate][type=application/rss+xml], link[rel=alternate][type=application/atom+xml]");

                for (Element link : links) {
                    String feedUrl = link.attr("abs:href");
                    String feedName = link.attr("title");
                    if (!feedUrl.isBlank()) {
                        validFeeds.add(new Feed(-1, -1, feedName.isBlank() ? feedUrl : feedName, URI.create(url),
                                URI.create(feedUrl)));
                    }
                }
            }

        } catch (IOException e) {
            logger.error("Error fetching URL: {}", url, e);
            validFeeds = Collections.emptyList();
        }

        return validFeeds;
    }

    /**
     * Helper to determine if content is likely an RSS/Atom feed based on headers or
     * raw content.
     */
    private boolean isFeed(String contentType, String body) {
        // 1. Check Content-Type header (fastest)
        if (contentType != null) {
            String type = contentType.toLowerCase();
            if (type.contains("application/rss+xml") || // Standard RSS
                    type.contains("application/atom+xml") || // Atom
                    type.contains("application/rdf+xml")) { // RSS 1.0
                return true;
            }
        }

        // 2. Check Content
        String startOfBody = body.length() > 500 ? body.substring(0, 500).toLowerCase() : body.toLowerCase();
        return startOfBody.contains("<rss") || // Standard RSS
                startOfBody.contains("<feed") || // Atom
                startOfBody.contains("<rdf:rdf"); // RSS 1.0
    }

    private void parseFeed(Feed feed) {
        logger.debug("parseFeed");
        MediaRssReader rssReader = new MediaRssReader();
        try {
            var items = rssReader.read(feed.getFeedURI().toString()).toList();
            List<Article> articles = new ArrayList<>();

            for (MediaRssItem item : items) {
                try {
                    var title = item.getTitle().orElse("");
                    var description = item.getDescription().orElse("");
                    var content = item.getContent().orElse("");
                    var link = item.getLink().orElse("");
                    var datetime = item.getPubDateZonedDateTime()
                            .map(zdt -> zdt.withZoneSameInstant(ZoneOffset.UTC).format(formatter))
                            .orElse("unknown pub date");
                    var author = item.getAuthor().orElse("");
                    var imageUrl = "";
                    if (item.getMediaThumbnail().isPresent()) {
                        imageUrl = item.getMediaThumbnail().get().getUrl();
                    }
                    if (imageUrl.equals("") && item.getEnclosure().isPresent()
                            && item.getEnclosure().get().getType().startsWith("image/")) {
                        imageUrl = item.getEnclosure().get().getUrl();
                    }

                    var categories = item.getCategories().toString();

                    Article article = new Article(-1, feed.getId(), feed.getName(), title, description, content, link,
                            datetime, author, imageUrl, categories);
                    articles.add(article);
                } catch (Exception e) {
                    logger.error("Error refreshing feed [{}] \n {}\n", feed.getName(), e.getMessage());
                }
            }
            articleRepository.create(articles);
        } catch (IOException e) {
            logger.error("Error refreshing feed [{}] \n {}\n", feed.getName(), e.getMessage());
        }
    }

    public int deleteFeed(int feedId) {
        logger.debug("deleteFeed");
        articleRepository.deleteByFeed(feedId);
        return feedRepository.delete(feedId);
    }
}
