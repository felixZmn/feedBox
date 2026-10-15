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
        int unreadCount) {

    public Feed(int id, Integer folderId, String name, URI url, URI feedUrl) {
        this(id, folderId, name, url, feedUrl, null, null, 0);
    }

    public Feed withFolderId(Integer folderId) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount);
    }

    public Feed withName(String name) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount);
    }

    public Feed withUrl(URI url) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount);
    }

    public Feed withFeedUrl(URI feedUrl) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount);
    }

    public Feed withLastRefreshedAt(Instant lastRefreshedAt) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount);
    }

    public Feed withLastError(String lastError) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount);
    }

    public Feed withUnreadCount(int unreadCount) {
        return new Feed(id, folderId, name, url, feedUrl, lastRefreshedAt, lastError, unreadCount);
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
}
