package de._0x2b.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Folder(
        int id,
        String name,
        @JsonIgnore List<Feed> feeds,
        String color) {

    public Folder withFeeds(List<Feed> feeds) {
        return new Folder(id, name, feeds, color);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public List<Feed> getFeeds() {
        return feeds;
    }

    public String getColor() {
        return color;
    }
}
