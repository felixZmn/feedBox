package de._0x2b.model;

import java.time.Instant;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Article(
        long id,
        int feedId,
        String feedName,
        String title,
        String description,
        String content,
        String link,
        String guid,
        Instant published,
        String authors,
        String imageUrl,
        String categories,
        boolean read,
        boolean saved) {

    /** Convenience when guid defaults to link. */
    public Article(long id, int feedId, String feedName, String title, String description, String content, String link,
            Instant published, String authors, String imageUrl, String categories) {
        this(id, feedId, feedName, title, description, content, link, link, published, authors, imageUrl, categories,
                false, false);
    }

    public Article(long id, int feedId, String feedName, String title, String description, String content, String link,
            String guid, Instant published, String authors, String imageUrl, String categories) {
        this(id, feedId, feedName, title, description, content, link, guid, published, authors, imageUrl, categories,
                false, false);
    }

    public long getId() {
        return id;
    }

    public int getFeedId() {
        return feedId;
    }

    public String getFeedName() {
        return feedName;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }

    public String getLink() {
        return link;
    }

    public String getGuid() {
        return guid;
    }

    public Instant getPublished() {
        return published;
    }

    public String getAuthors() {
        return authors;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCategories() {
        return categories;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isSaved() {
        return saved;
    }
}
