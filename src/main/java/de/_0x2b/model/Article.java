package de._0x2b.model;

import java.time.Instant;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Article {
    long id;
    int feedId;
    String feedName;
    String title;
    String description;
    String content;
    String link;
    String guid;
    Instant published;
    String authors;
    String imageUrl;
    String categories;
    boolean read;

    public Article() {
    }

    public Article(long id, int feedId, String feedName, String title, String description, String content, String link,
            Instant published, String authors, String imageUrl, String categories) {
        this(id, feedId, feedName, title, description, content, link, link, published, authors, imageUrl, categories,
                false);
    }

    public Article(long id, int feedId, String feedName, String title, String description, String content, String link,
            String guid, Instant published, String authors, String imageUrl, String categories) {
        this(id, feedId, feedName, title, description, content, link, guid, published, authors, imageUrl, categories,
                false);
    }

    public Article(long id, int feedId, String feedName, String title, String description, String content, String link,
            Instant published, String authors, String imageUrl, String categories, boolean read) {
        this(id, feedId, feedName, title, description, content, link, link, published, authors, imageUrl, categories,
                read);
    }

    public Article(long id, int feedId, String feedName, String title, String description, String content, String link,
            String guid, Instant published, String authors, String imageUrl, String categories, boolean read) {
        this.id = id;
        this.feedId = feedId;
        this.feedName = feedName;
        this.title = title;
        this.description = description;
        this.content = content;
        this.link = link;
        this.guid = guid;
        this.published = published;
        this.authors = authors;
        this.imageUrl = imageUrl;
        this.categories = categories;
        this.read = read;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Article article = (Article) o;
        return id == article.id && feedId == article.feedId && read == article.read
                && Objects.equals(feedName, article.feedName)
                && Objects.equals(title, article.title) && Objects.equals(description, article.description)
                && Objects.equals(content, article.content) && Objects.equals(link, article.link)
                && Objects.equals(guid, article.guid)
                && Objects.equals(published, article.published) && Objects.equals(authors, article.authors)
                && Objects.equals(imageUrl, article.imageUrl) && Objects.equals(categories, article.categories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, feedId, feedName, title, description, content, link, guid, published, authors,
                imageUrl, categories, read);
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

    public void setRead(boolean read) {
        this.read = read;
    }
}
