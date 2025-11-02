package de._0x2b.services;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;
import de._0x2b.exceptions.NotFoundException;
import de._0x2b.models.Article;
import de._0x2b.models.Feed;
import de._0x2b.repositories.ArticleRepository;
import de._0x2b.repositories.FeedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

        iconService.findIcon(feed);

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
        } catch (IOException e) {
            logger.error("Error querying feed [{}] \n {}\n", feed.getFeedURI(), e.getMessage());
            // ToDo: Exception?
        }
        return feed;
    }

    private void parseFeed(Feed feed) {
        logger.debug("parseFeed");
        RssReader rssReader = new RssReader();
        try {
            var items = rssReader.read(feed.getFeedURI().toString()).toList();
            List<Article> articles = new ArrayList<>();

            for (Item item : items) {
                try {
                    var title = item.getTitle().orElse("");
                    var description = item.getDescription().orElse("");
                    var content = item.getContent().orElse("");
                    var link = item.getLink().orElse("");
                    var datetime = item.getPubDateZonedDateTime()
                            .map(zdt -> zdt.withZoneSameInstant(ZoneOffset.UTC).format(formatter))
                            .orElse("unknown pub date");
                    var author = item.getAuthor().orElse("");
                    var categories = item.getCategories().toString();

                    Article article = new Article(-1, feed.getId(), feed.getName(),
                            title, description,
                            content, link,
                            datetime,
                            author, "",
                            categories);
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
