package de._0x2b.service;

import com.apptasticsoftware.rssreader.module.mediarss.MediaRssItem;
import de._0x2b.model.Article;
import de._0x2b.model.Feed;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@ApplicationScoped
public class ArticleMapper {
    public Article toArticle(Feed feed, MediaRssItem item) {
        var title = item.getTitle().orElse("");
        var description = item.getDescription().orElse("");
        var content = item.getContent().orElse("");
        var link = item.getLink().orElse("");
        Instant published = item.getPubDateAsZonedDateTime()
                .map(zdt -> zdt.toInstant())
                .orElse(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
        var author = item.getAuthor().orElse("");

        var imageUrl = "";
        if (item.getMediaThumbnail().isPresent()) {
            imageUrl = item.getMediaThumbnail().get().getUrl();
        }
        if (imageUrl.isEmpty() && item.getEnclosure().isPresent()
                && item.getEnclosure().get().getType().startsWith("image/")) {
            imageUrl = item.getEnclosure().get().getUrl();
        }

        var categories = item.getCategories().toString();

        return new Article(
                -1, feed.getId(), feed.getName(), title, description, content, link,
                published, author, imageUrl, categories);
    }
}
