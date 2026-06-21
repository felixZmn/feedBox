package de._0x2b.service;

import com.apptasticsoftware.rssreader.RssReader;
import de._0x2b.exception.NotFoundException;
import de._0x2b.model.Feed;
import de._0x2b.model.Icon;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.repository.FeedRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FeedService {
    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);

    @Inject
    HTTPSService httpsService;
    @Inject
    IconService iconService;
    @Inject
    FeedRepository feedRepository;
    @Inject
    ArticleRepository articleRepository;
    @Inject
    MediaRssParser mediaRssParser;
    @Inject
    ArticleMapper articleMapper;

    @ConfigProperty(name = "refresh.concurrency", defaultValue = "10")
    int refreshConcurrency = 10;

    /**
     * Store a new feed in the database
     *
     * @param feed
     * @return
     * @throws NotFoundException
     */
    @Transactional
    public int create(Feed feed) throws NotFoundException {
        logger.debug("create");
        feed = this.getFeedMetadata(feed);
        Icon icon = iconService.findIcon(feed);

        int feedId = create(feed, icon);
        return feedId;
    }

    public int create(Feed feed, Icon icon) {
        logger.debug("storeData");
        int feedId = feedRepository.create(feed);
        if (icon != null) {
            icon.setFeedId(feedId);
            iconService.create(icon);
        }
        return feedId;
    }

    /**
     * Update an existing feed
     *
     * @param feed
     * @return
     */
    @Transactional
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
     * @param id id of the feed to refresh
     */
    public void refresh(int id) {
        logger.debug("refresh");
        var feeds = feedRepository.findOne(id);
        refresh(feeds);
    }

    /**
     * Internal helper to refresh a list of feeds with bounded concurrency using
     * virtual threads.
     *
     * @param feeds
     */
    private void refresh(List<Feed> feeds) {
        logger.info("Refreshing Feeds...");
        Semaphore semaphore = new Semaphore(refreshConcurrency);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = feeds.stream()
                    .map(feed -> CompletableFuture.runAsync(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                parseFeed(feed);
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.warn("Feed refresh interrupted for {}", feed.getFeedUrl(), e);
                        }
                    }, executor))
                    .toList();

            for (var future : futures) {
                try {
                    future.join();
                } catch (Exception e) {
                    logger.error("Feed refresh failed", e);
                }
            }
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
            var channel = rssReader.read(feed.getFeedUrl().toString()).toList().getFirst().getChannel();
            feed.setUrl(URI.create(channel.getLink()));
            feed.setName(channel.getTitle());
        } catch (Exception e) {
            logger.error("Error querying feed [{}] \n {}\n", feed.getFeedUrl(), e.getMessage());
            throw new NotFoundException("Feed not reachable or not a valid RSS/Atom feed: " + feed.getFeedUrl());
        }
        return feed;
    }

    /**
     * Check if a URL is a valid feed or contains feed links
     *
     * @param url URL to check
     * @return List of valid feeds found
     */
    public List<Feed> checkFeedUrl(String url) {
        return checkFeedUrl(url, new JsoupProvider());
    }

    public List<Feed> checkFeedUrl(String url, JsoupProvider provider) {
        logger.debug("checkFeedUrl: {}", url);
        List<Feed> validFeeds = new ArrayList<>();

        try {
            Connection.Response queryResponse = provider.execute(url);

            String body = queryResponse.body();
            String contentType = queryResponse.contentType();

            if (isFeed(contentType, body)) {
                logger.debug("URL is a direct feed: {}", url);
                validFeeds.add(new Feed(-1, null, "", null, URI.create(url)));
            } else {
                Document doc = queryResponse.parse();

                Elements links = doc.select(
                        "link[rel=alternate][type=application/rss+xml], link[rel=alternate][type=application/atom+xml]");

                for (Element link : links) {
                    String feedUrl = extractFeedUrl(link);
                    String feedName = link.attr("title");
                    if (!feedUrl.isBlank()) {
                        validFeeds.add(new Feed(-1, null, feedName.isBlank() ? feedUrl : feedName, URI.create(url),
                                URI.create(feedUrl)));
                    }
                }
            }

        } catch (HttpStatusException e) {
            logger.warn("Remote server returned HTTP {} for URL: {}", e.getStatusCode(), url);
            validFeeds = Collections.emptyList();
        } catch (IOException e) {
            logger.error("Error fetching URL: {}", url, e);
            validFeeds = Collections.emptyList();
        } catch (IllegalArgumentException e) {
            logger.warn("Malformed or non-absolute URL provided: {}", url);
            validFeeds = Collections.emptyList();
        }

        return validFeeds;
    }

    /**
     * Extracts and normalises a feed URL from a &lt;link&gt; element.
     * Handles the legacy {@code feed://} and {@code feed:https://} URI schemes
     * used by some RSS readers, converting them to a usable {@code https://} URL.
     * Falls back to Jsoup's abs:href resolution for ordinary relative/absolute
     * URLs.
     *
     * @param link the &lt;link&gt; element to extract the URL from
     * @return a normalised, absolute URL string, or blank if one cannot be
     *         determined
     */
    private String extractFeedUrl(Element link) {
        String href = link.attr("href").trim();
        String lower = href.toLowerCase();
        // feed://example.com/rss.xml -> https://example.com/rss.xml
        if (lower.startsWith("feed://")) {
            href = "https://" + href.substring("feed://".length());
            // feed:https://example.com/rss.xml or feed:http://example.com/rss.xml
        } else if (lower.startsWith("feed:http://") || lower.startsWith("feed:https://")) {
            href = href.substring("feed:".length());
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        // Relative URL: let Jsoup resolve it against the document base URI
        return link.attr("abs:href");
    }

    /**
     * Helper to determine if content is likely an RSS/Atom feed based on headers or
     * raw content.
     *
     * @param contentType
     * @param body
     * @return
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

    /**
     * Parse a feed and store articles in the database (batch insert).
     * <p>
     * Refresh-health columns on the feed are updated in lockstep:
     * <ul>
     * <li>On success: {@code last_refreshed_at} is set, {@code last_error}
     * cleared.</li>
     * <li>On failure: {@code last_error} is set to a truncated message and
     * {@code last_refreshed_at} is left untouched (preserves the
     * "last seen working" timestamp).</li>
     * </ul>
     * A missing HTTP response (404, network error, etc.) is treated as a
     * "no new data" condition, not a failure - the timestamp is updated
     * and any previous error is cleared.
     *
     * @param feed
     */
    void parseFeed(Feed feed) { // package-private for direct testing
        var optional = httpsService.fetchUriAsStream(feed.getFeedUrl());
        if (optional.isEmpty() || optional.get().statusCode() != 200) {
            // The server did not give us a feed body. Could be a transient
            // outage - record the error but don't penalise a feed that
            // worked yesterday and is broken today.
            try {
                feedRepository.markRefreshError(feed.getId(),
                        "No successful HTTP response (status="
                                + (optional.isPresent() ? optional.get().statusCode() : "no-response")
                                + ")");
            } catch (SQLException e) {
                logger.warn("Failed to record refresh error for feed [{}]", feed.getFeedUrl(), e);
            }
            return;
        }

        var response = optional.get();
        var items = mediaRssParser.parse(response.body());

        var articles = new java.util.ArrayList<de._0x2b.model.Article>(items.size());
        int skipped = 0;
        for (var item : items) {
            try {
                articles.add(articleMapper.toArticle(feed, item));
            } catch (Exception e) {
                skipped++;
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping article in feed [{}]", feed.getFeedUrl(), e);
                }
            }
        }
        if (skipped > 0) {
            logger.warn("Skipped {} malformed article(s) in feed [{}]", skipped, feed.getFeedUrl());
        }

        try {
            articleRepository.create(articles);
            // Insert succeeded - mark the feed as healthy.
            feedRepository.markRefreshSuccess(feed.getId(), null);
        } catch (Exception e) {
            // The DataAccessException is the common case (the new
            // ArticleRepository.create throws it), but we also want to
            // catch anything thrown by the parser or the success-mark
            // path so a single bad feed doesn't kill the whole batch.
            logger.error("Refresh failed for feed [{}]", feed.getFeedUrl(), e);
            try {
                feedRepository.markRefreshError(feed.getId(), e.getMessage());
            } catch (SQLException markEx) {
                logger.warn("Failed to record refresh error for feed [{}]", feed.getFeedUrl(), markEx);
            }
        }
    }

    /**
     * Delete a feed and its articles
     *
     * @param feedId
     * @return
     */
    @Transactional
    public int delete(int feedId) {
        logger.debug("delete");
        try {
            articleRepository.deleteByFeed(feedId);
        } catch (SQLException e) {
            return -1;
        }
        try {
            return feedRepository.delete(feedId);
        } catch (SQLException e) {
            return -1;
        }
    }
}
