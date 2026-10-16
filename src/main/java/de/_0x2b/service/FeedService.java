package de._0x2b.service;

import com.apptasticsoftware.rssreader.RssReader;
import de._0x2b.exception.DataAccessException;
import de._0x2b.exception.InvalidFeedException;
import de._0x2b.exception.NotFoundException;
import de._0x2b.http.FetchResult;
import de._0x2b.http.OutboundHttpClient;
import de._0x2b.job.ProgressCallback;
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
     * Fetch metadata and favicon (network I/O), then persist in a short transaction.
     *
     * @return generated feed id
     * @throws InvalidFeedException when the feed URL is unreachable or invalid
     */
    public int create(Feed feed) {
        feed = this.getFeedMetadata(feed);
        Icon icon = iconService.findIcon(feed);
        return persistNewFeed(feed, icon);
    }

    @Transactional
    public int persistNewFeed(Feed feed, Icon icon) {
        int feedId = feedRepository.create(feed);
        if (icon != null) {
            iconService.create(icon.withFeedId(feedId));
        }
        return feedId;
    }

    /** Persist an already-resolved feed + optional icon (e.g. OPML import). */
    public int create(Feed feed, Icon icon) {
        return persistNewFeed(feed, icon);
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
        return feeds.stream()
                .map(feed -> feed.withUnreadCount(unreadCounts.getOrDefault(feed.getId(), 0)))
                .toList();
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
     * Advance this feed's read horizon to its current max article id.
     *
     * @param feedId feed primary key
     * @throws NotFoundException when the feed does not exist
     */
    public void markRead(int feedId) {
        try {
            int updated = articleRepository.markReadByFeed(feedId);
            if (updated == 0) {
                throw new NotFoundException("Feed not found: " + feedId);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark feed read: " + feedId, e);
        }
    }

    /**
     * Refresh all feeds in the database.
     */
    public void refresh() {
        refresh(ProgressCallback.NONE);
    }

    /**
     * Refresh all feeds, reporting progress to {@code progress}.
     */
    public void refresh(ProgressCallback progress) {
        var feeds = feedRepository.findAll();
        refresh(feeds, progress != null ? progress : ProgressCallback.NONE);
    }

    /**
     * Refresh a single feed by its ID.
     *
     * @param id id of the feed to refresh
     */
    public void refresh(int id) {
        var feeds = feedRepository.findOne(id);
        refresh(feeds, ProgressCallback.NONE);
    }

    /**
     * Internal helper to refresh a list of feeds with bounded concurrency using
     * virtual threads.
     */
    private void refresh(List<Feed> feeds, ProgressCallback progress) {
        logger.info("Refreshing Feeds...");
        progress.setTotal(feeds.size());
        Semaphore semaphore = new Semaphore(refreshConcurrency);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = feeds.stream()
                    .map(feed -> CompletableFuture.runAsync(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                if (parseFeed(feed)) {
                                    progress.itemSucceeded();
                                } else {
                                    progress.itemFailed();
                                }
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            progress.itemFailed();
                            logger.warn("Feed refresh interrupted for {}", feed.getFeedUrl(), e);
                        }
                    }, executor))
                    .toList();

            for (var future : futures) {
                try {
                    future.join();
                } catch (Exception e) {
                    logger.warn("Feed refresh failed: {}", e.toString());
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
        try {
            var fetched = outboundHttpClient.fetchFeed(feed.getFeedUrl());
            if (fetched.isEmpty()) {
                throw new InvalidFeedException(
                        "Feed not reachable or not a valid RSS/Atom feed: " + feed.getFeedUrl());
            }
            RssReader rssReader = new RssReader();
            rssReader.setUserAgent(outboundHttpClient.userAgent());
            var items = rssReader.read(fetched.get().bodyStream()).toList();
            if (!items.isEmpty()) {
                var channel = items.getFirst().getChannel();
                String link = channel.getLink();
                String title = channel.getTitle();
                URI siteUrl = (link != null && !link.isBlank())
                        ? URI.create(link)
                        : (feed.getUrl() != null ? feed.getUrl() : feed.getFeedUrl());
                String name = (title != null && !title.isBlank())
                        ? title
                        : (feed.getName() != null && !feed.getName().isBlank()
                                ? feed.getName()
                                : fallbackFeedName(feed.getFeedUrl()));
                feed = feed.withUrl(siteUrl).withName(name);
            } else {
                URI siteUrl = feed.getUrl() != null ? feed.getUrl() : feed.getFeedUrl();
                String name = (feed.getName() != null && !feed.getName().isBlank())
                        ? feed.getName()
                        : fallbackFeedName(feed.getFeedUrl());
                feed = feed.withUrl(siteUrl).withName(name);
            }
        } catch (InvalidFeedException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Error querying feed [{}]: {}", feed.getFeedUrl(), e.getMessage());
            throw new InvalidFeedException(
                    "Feed not reachable or not a valid RSS/Atom feed: " + feed.getFeedUrl(), e);
        }
        return feed;
    }

    private static String fallbackFeedName(URI feedUrl) {
        if (feedUrl == null) {
            return "Untitled feed";
        }
        String host = feedUrl.getHost();
        return host != null && !host.isBlank() ? host : feedUrl.toString();
    }

    /**
     * Check if a URL is a valid feed or contains feed links.
     * Fetches through {@link OutboundHttpClient} (SSRF guard + size limit).
     *
     * @param url URL to check
     * @return List of valid feeds found
     */
    public List<Feed> checkFeedUrl(String url) {
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
     * @return {@code true} when the feed was refreshed successfully
     */
    boolean parseFeed(Feed feed) { // package-private for direct testing
        var optional = outboundHttpClient.fetchFeed(feed.getFeedUrl());
        if (optional.isEmpty()) {
            // Unsafe URL, network error, non-2xx, or oversized body.
            try {
                feedRepository.markRefreshError(feed.getId(), "No successful HTTP response");
            } catch (SQLException e) {
                logger.warn("Failed to record refresh error for feed [{}]", feed.getFeedUrl(), e);
            }
            return false;
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
            persistRefreshSuccess(feed.getId(), articles);
            return true;
        } catch (Exception e) {
            logger.warn("Refresh failed for feed [{}]: {}", feed.getFeedUrl(), e.getMessage());
            try {
                feedRepository.markRefreshError(feed.getId(), "Could not store articles");
            } catch (SQLException markEx) {
                logger.warn("Failed to record refresh error for feed [{}]", feed.getFeedUrl(), markEx);
            }
            return false;
        }
    }

    @Transactional
    void persistRefreshSuccess(int feedId, List<de._0x2b.model.Article> articles) throws SQLException {
        articleRepository.create(articles);
        feedRepository.markRefreshSuccess(feedId, null);
    }

    /**
     * Delete a feed; articles and icons are removed via ON DELETE CASCADE.
     */
    @Transactional
    public void delete(int feedId) {
        int rows = feedRepository.delete(feedId);
        if (rows == 0) {
            throw new NotFoundException("Feed not found: " + feedId);
        }
    }
}

