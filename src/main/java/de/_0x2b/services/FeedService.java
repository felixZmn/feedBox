package de._0x2b.services;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.RssReader;

import de._0x2b.models.Article;
import de._0x2b.models.Feed;
import de._0x2b.repositories.ArticleRepository;
import de._0x2b.repositories.FeedRepository;

public class FeedService {
    private final FeedRepository feedRepository = new FeedRepository();
    private final ArticleRepository articleRepository = new ArticleRepository();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

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

        feeds.stream().parallel().forEach(f -> {
            parseFeed(f);
        });
    }

    /**
     * Refresh a single feed by its ID
     * 
     * @param id
     */
    public void refresh(int id) {
        var feed = feedRepository.findOne(id);
        parseFeed(feed.get(0));
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
            var channel = rssReader.read(feed.getFeedUrl()).toList().get(0).getChannel();
            feed.setUrl(channel.getLink());
            feed.setName(channel.getTitle());
        } catch (IOException e) {
            System.out.printf("Error querying feed [%s] \n %s\n", feed.getFeedUrl(), e.getMessage());
        }
        return feed;
    }

    private void parseFeed(Feed feed) {
        System.setProperty("jdk.xml.totalEntitySizeLimit", "500000");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "500000");

        RssReader rssReader = new RssReader();
        try {
            var items = rssReader.read(feed.getFeedUrl()).toList();
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
                    System.out.printf("Error refreshing feed [%s] \n %s\n", feed.getName(), e.getMessage());
                }
            }
            articleRepository.create(articles);
        } catch (IOException e) {
            System.out.printf("Error refreshing feed [%s] \n %s\n", feed.getName(), e.getMessage());
        }
    }

    public int deleteFeed(int feedId) {
        return feedRepository.delete(feedId);
    }
}
