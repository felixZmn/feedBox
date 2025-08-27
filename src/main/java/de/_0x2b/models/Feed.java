package de._0x2b.models;

public class Feed {
    int id;
    int folderId;
    String name;
    String url;
    String feedUrl;

    public Feed(int id, int folderId, String name, String url, String feedUrl) {
        this.id = id;
        this.folderId = folderId;
        this.name = name;
        this.url = url;
        this.feedUrl = feedUrl;
    }

    public int getId() {
        return id;
    }

    public int getFolderId() {
        return folderId;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

}
