package de._0x2b.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Folder {
    int id;
    String name;
    List<Feed> feeds;
    String color;

    @JsonCreator
    public Folder(
            @JsonProperty("id") int id,
            @JsonProperty("name") String name,
            @JsonProperty("feeds") List<Feed> feeds,
            @JsonProperty("color") String color) {
        this.id = id;
        this.name = name;
        this.feeds = feeds;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Feed> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<Feed> feeds) {
        this.feeds = feeds;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

}
