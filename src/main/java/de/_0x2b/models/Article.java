package de._0x2b.models;

import java.util.Objects;

public class Article {
    int id;
    int feedId;
    String feedName;
    String title;
    String description;
    String content;
    String link;
    String published;
    String authors;
    String imageUrl;
    String categories;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Article article = (Article) o;
        return id == article.id && feedId == article.feedId && Objects.equals(feedName, article.feedName) && Objects.equals(title, article.title) && Objects.equals(description, article.description) && Objects.equals(content, article.content) && Objects.equals(link, article.link) && Objects.equals(published, article.published) && Objects.equals(authors, article.authors) && Objects.equals(imageUrl, article.imageUrl) && Objects.equals(categories, article.categories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, feedId, feedName, title, description, content, link, published, authors, imageUrl, categories);
    }

    public Article(int id, int feedId, String feedName, String title, String description, String content, String link,
                   String published, String authors, String imageUrl, String categories) {
        this.id = id;
        this.feedId = feedId;
        this.feedName = feedName;
        this.title = title;
        this.description = description;
        this.content = content;
        this.link = link;
        this.published = published;
        this.authors = authors;
        this.imageUrl = imageUrl;
        this.categories = categories;
    }

    public int getId() {
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

    public String getPublished() {
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
}
