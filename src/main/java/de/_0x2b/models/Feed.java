package de._0x2b.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Feed {
    int id;
    int folderId;
    String name;
    String url;
    String feedUrl;

    @JsonCreator
    public Feed(
            @JsonProperty("id") int id,
            @JsonProperty("folderId") int folderId,
            @JsonProperty("name") String name,
            @JsonProperty("url") String url,
            @JsonProperty("feedUrl") String feedUrl) {
        this.id = id;
        this.folderId = folderId;
        this.name = name;
        this.url = url;
        this.feedUrl = feedUrl;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

}
