package de._0x2b.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    List<String> categories;

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

    public Article(){
        this(-1, -1, "", "","","","","","","","");
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
        this.categories = new ArrayList<>();
        this.categories.addAll(Arrays.asList(categories.split(",")));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFeedId() {
        return feedId;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getPublished() {
        return published;
    }

    public void setPublished(String published) {
        this.published = published;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategories() {
        return String.join(",", categories);
    }

    public void addCategory(String category) {
        if(this.categories == null){
            this.categories = new ArrayList<>();
        }
        this.categories.add(category);
    }
}
