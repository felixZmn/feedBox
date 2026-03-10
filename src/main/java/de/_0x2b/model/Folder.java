package de._0x2b.model;

import java.util.List;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Folder {
    int id;
    String name;
    List<Feed> feeds;
    String color;

    public Folder() {
    }

    public Folder(int id, String name, List<Feed> feeds, String color) {
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Folder folder = (Folder) o;
        return id == folder.id && Objects.equals(name, folder.name) && Objects.equals(feeds, folder.feeds)
                && Objects.equals(color, folder.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, feeds, color);
    }
}
