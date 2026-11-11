package de._0x2b.model;

import java.net.URI;
import java.time.Instant;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Feed(
        int id,
        Integer folderId,
        String name,
        URI url,
        URI feedUrl,
        Instant lastRefreshedAt,
        String lastError,
        int unreadCount,
        long latestArticleId) {

    public Feed(int id, Integer folderId, String name, URI url, URI feedUrl) {
        this(id, folderId, name, url, feedUrl, null, null, 0, 0);
    }

    public Feed withId(int id) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount,
                latestArticleId);
    }

    public Feed withFolderId(Integer folderId) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount,
                latestArticleId);
    }

    public Feed withName(String name) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount,
                latestArticleId);
    }

    public Feed withUrl(URI url) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount,
                latestArticleId);
    }

    public Feed withFeedUrl(URI feedUrl) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount,
                latestArticleId);
    }

    public Feed withLastRefreshedAt(Instant lastRefreshedAt) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount,
                latestArticleId);
    }

    public Feed withLastError(String lastError) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount,
                latestArticleId);
    }

    public Feed withUnreadCount(int unreadCount) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount,
                latestArticleId);
    }

    public Feed withLatestArticleId(long latestArticleId) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount,
                latestArticleId);
    }

    public int getId() {
        return id;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public String getName() {
        return name;
    }

    public URI getUrl() {
        return url;
    }

    public URI getFeedUrl() {
        return feedUrl;
    }

    public Instant getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public long getLatestArticleId() {
        return latestArticleId;
    }
}
