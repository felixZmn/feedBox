package de._0x2b.models;

public class Article {
    int id;
    int feedID;
    String feedName;
    String title;
    String description;
    String content;
    String link;
    String published;
    String authors;
    String imageUrl;
    String categories;

    public Article(int id, int feedID, String feedName, String title, String description, String content, String link,
            String published, String authors, String imageUrl, String categories) {
        this.id = id;
        this.feedID = feedID;
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

    public int getFeedID() {
        return feedID;
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
