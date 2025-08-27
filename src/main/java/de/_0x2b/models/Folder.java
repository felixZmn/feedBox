package de._0x2b.models;

import java.util.List;

public class Folder {
    int id;
    String name;
    List<Feed> feeds;
    String color;

    public Folder(int id, String name, List<Feed> feeds, String color) {
        this.id = id;
        this.name = name;
        this.feeds = feeds;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Feed> getFeeds() {
        return feeds;
    }

    public String getColor() {
        return color;
    }

}
