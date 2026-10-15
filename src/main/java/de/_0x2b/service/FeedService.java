package de._0x2b.service;

import com.apptasticsoftware.rssreader.RssReader;
import de._0x2b.exception.DataAccessException;
import de._0x2b.exception.InvalidFeedException;
import de._0x2b.exception.NotFoundException;
import de._0x2b.http.FetchResult;
import de._0x2b.http.OutboundHttpClient;
import de._0x2b.model.Feed;
import de._0x2b.model.Icon;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.repository.FeedRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    OutboundHttpClient outboundHttpClient;
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
     * @return generated feed id
     * @throws InvalidFeedException when the feed URL is unreachable or invalid
     */
    @Transactional
    public int create(Feed feed) {
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
     * @return the feed id
     * @throws NotFoundException when the feed does not exist
     */
    @Transactional
    public int update(Feed feed) {
        logger.debug("update");
        int rows = feedRepository.update(feed);
        if (rows == 0) {
            throw new NotFoundException("Feed not found: " + feed.getId());
        }
        return feed.getId();
    }

    /**
     * Get all feeds in the database, with per-feed unread article counts.
     *
     * @return all feeds
     */
    public List<Feed> findAll() {
        List<Feed> feeds = feedRepository.findAll();
        var unreadCounts = articleRepository.countUnreadByFeed();
        for (Feed feed : feeds) {
            feed.setUnreadCount(unreadCounts.getOrDefault(feed.getId(), 0));
        }
        return feeds;
    }

    /**
     * @param id feed primary key
     * @return the feed
     * @throws NotFoundException when missing
     */
    public Feed findById(int id) {
        List<Feed> feeds = feedRepository.findOne(id);
        if (feeds.isEmpty()) {
            throw new NotFoundException("Feed not found: " + id);
        }
        return feeds.getFirst();
    }

    /**
     * Mark every article in a feed as read.
     *
     * @param feedId feed primary key
     * @throws NotFoundException when the feed does not exist
     */
    public void markRead(int feedId) {
        logger.debug("markRead feed {}", feedId);
        if (feedRepository.findOne(feedId).isEmpty()) {
            throw new NotFoundException("Feed not found: " + feedId);
        }
        try {
            articleRepository.markReadByFeed(feedId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark feed read: " + feedId, e);
        }
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
     * @throws InvalidFeedException when the URL is unreachable or not a feed
     */
    public Feed getFeedMetadata(Feed feed) {
        logger.debug("getFeedMetadata");
        try {
            var fetched = outboundHttpClient.fetchFeed(feed.getFeedUrl());
            if (fetched.isEmpty()) {
                throw new InvalidFeedException(
                        "Feed not reachable or not a valid RSS/Atom feed: " + feed.getFeedUrl());
            }
            RssReader rssReader = new RssReader();
            rssReader.setUserAgent(outboundHttpClient.userAgent());
            var channel = rssReader.read(fetched.get().bodyStream()).toList().getFirst().getChannel();
            feed.setUrl(URI.create(channel.getLink()));
            feed.setName(channel.getTitle());
        } catch (InvalidFeedException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error querying feed [{}]: {}", feed.getFeedUrl(), e.getMessage());
            throw new InvalidFeedException(
                    "Feed not reachable or not a valid RSS/Atom feed: " + feed.getFeedUrl(), e);
        }
        return feed;
    }

    /**
     * Check if a URL is a valid feed or contains feed links.
     * Fetches through {@link OutboundHttpClient} (SSRF guard + size limit).
     *
     * @param url URL to check
     * @return List of valid feeds found
     */
    public List<Feed> checkFeedUrl(String url) {
        logger.debug("checkFeedUrl: {}", url);
        List<Feed> validFeeds = new ArrayList<>();

        URI requestUri;
        try {
            requestUri = URI.create(url);
        } catch (IllegalArgumentException e) {
            logger.warn("Malformed or non-absolute URL provided: {}", url);
            return Collections.emptyList();
        }

        var fetched = outboundHttpClient.fetchFeed(requestUri);
        if (fetched.isEmpty()) {
            return Collections.emptyList();
        }
        FetchResult result = fetched.get();
        String body = new String(result.body(), charsetFromContentType(result.contentType()));
        String contentType = result.contentType();
        String baseUri = result.finalUri().toString();

        if (isFeed(contentType, body)) {
            logger.debug("URL is a direct feed: {}", url);
            validFeeds.add(new Feed(-1, null, "", null, result.finalUri()));
        } else {
            Document doc = Jsoup.parse(body, baseUri);

            Elements links = doc.select(
                    "link[rel=alternate][type=application/rss+xml], link[rel=alternate][type=application/atom+xml]");

            for (Element link : links) {
                String feedUrl = extractFeedUrl(link);
                String feedName = link.attr("title");
                if (!feedUrl.isBlank()) {
                    validFeeds.add(new Feed(-1, null, feedName.isBlank() ? feedUrl : feedName,
                            result.finalUri(), URI.create(feedUrl)));
                }
            }
        }

        return validFeeds;
    }

    private static Charset charsetFromContentType(String contentType) {
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.regionMatches(true, 0, "charset=", 0, 8)) {
                String name = part.substring(8).trim().replace("\"", "");
                try {
                    return Charset.forName(name);
                } catch (Exception e) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
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
        var optional = outboundHttpClient.fetchFeed(feed.getFeedUrl());
        if (optional.isEmpty()) {
            // Unsafe URL, network error, non-2xx, or oversized body.
            try {
                feedRepository.markRefreshError(feed.getId(), "No successful HTTP response");
            } catch (SQLException e) {
                logger.warn("Failed to record refresh error for feed [{}]", feed.getFeedUrl(), e);
            }
            return;
        }

        var response = optional.get();
        var items = mediaRssParser.parse(response.bodyStream());

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
     * @throws NotFoundException when the feed does not exist
     */
    @Transactional
    public void delete(int feedId) {
        logger.debug("delete");
        try {
            articleRepository.deleteByFeed(feedId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete articles for feed: " + feedId, e);
        }
        int rows = feedRepository.delete(feedId);
        if (rows == 0) {
            throw new NotFoundException("Feed not found: " + feedId);
        }
    }
}

