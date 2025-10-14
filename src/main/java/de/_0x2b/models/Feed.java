package de._0x2b.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.net.URI;
import java.util.Objects;

public class Feed {
    int id;
    int folderId;
    String name;
    @JsonSerialize(using = ToStringSerializer.class)
    URI url;
    @JsonSerialize(using = ToStringSerializer.class)
    URI feedUrl;
    Icon icon;

    @JsonCreator
    public Feed(@JsonProperty("id") int id, @JsonProperty("folderId") int folderId, @JsonProperty("name") String name, @JsonProperty("url") URI url, @JsonProperty("feedUrl") URI feedUrl) {
        this.id = id;
        this.folderId = folderId;
        this.name = name;
        this.url = url;
        this.feedUrl = feedUrl;
    }

    public Feed(int id, int folderId, String name, URI url, URI feedUrl, Icon icon) {
        this.id = id;
        this.folderId = folderId;
        this.name = name;
        this.url = url;
        this.feedUrl = feedUrl;
        this.icon = icon;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Feed feed = (Feed) o;
        return id == feed.id && folderId == feed.folderId && Objects.equals(name, feed.name) && Objects.equals(url, feed.url) && Objects.equals(feedUrl, feed.feedUrl) && Objects.equals(icon, feed.icon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, folderId, name, url, feedUrl, icon);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getURI() {
        return url;
    }

    public void setURI(URI url) {
        this.url = url;
    }

    public URI getFeedURI() {
        return feedUrl;
    }

    public void setFeedURI(URI feedUrl) {
        this.feedUrl = feedUrl;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

}
