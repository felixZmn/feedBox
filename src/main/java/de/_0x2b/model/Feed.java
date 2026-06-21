package de._0x2b.model;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Feed {
    int id;
    Integer folderId;
    String name;
    URI url;
    URI feedUrl;
    Icon icon;
    Instant lastRefreshedAt;
    String lastError;

    public Feed() {
    }

    public Feed(int id, Integer folderId, String name, URI url, URI feedUrl) {
        this.id = id;
        this.folderId = folderId;
        this.name = name;
        this.url = url;
        this.feedUrl = feedUrl;
    }

    public Feed(int id, Integer folderId, String name, URI url, URI feedUrl, Icon icon) {
        this.id = id;
        this.folderId = folderId;
        this.name = name;
        this.url = url;
        this.feedUrl = feedUrl;
        this.icon = icon;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Feed feed = (Feed) o;
        return id == feed.id && Objects.equals(folderId, feed.folderId) && Objects.equals(name, feed.name)
                && Objects.equals(url, feed.url) && Objects.equals(feedUrl, feed.feedUrl)
                && Objects.equals(icon, feed.icon)
                && Objects.equals(lastRefreshedAt, feed.lastRefreshedAt)
                && Objects.equals(lastError, feed.lastError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, folderId, name, url, feedUrl, icon, lastRefreshedAt, lastError);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public URI getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(URI feedUrl) {
        this.feedUrl = feedUrl;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public Instant getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public void setLastRefreshedAt(Instant lastRefreshedAt) {
        this.lastRefreshedAt = lastRefreshedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
