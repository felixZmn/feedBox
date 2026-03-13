package de._0x2b.service;

import com.apptasticsoftware.rssreader.Enclosure;
import com.apptasticsoftware.rssreader.module.mediarss.MediaRssItem;
import com.apptasticsoftware.rssreader.module.mediarss.MediaThumbnail;
import de._0x2b.model.Article;
import de._0x2b.model.Feed;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArticleMapperTest {

    private final ArticleMapper sut = new ArticleMapper();

    @Test
    void toArticle_mapsBasicFields_andFormatsDateInUtc() {
        Feed feed = new Feed(10, 1, "FeedName", URI.create("https://example.com"), URI.create("https://example.com/rss"));

        MediaRssItem item = mock(MediaRssItem.class);
        when(item.getTitle()).thenReturn(Optional.of("Title"));
        when(item.getDescription()).thenReturn(Optional.of("Desc"));
        when(item.getContent()).thenReturn(Optional.of("Content"));
        when(item.getLink()).thenReturn(Optional.of("https://example.com/a"));
        when(item.getAuthor()).thenReturn(Optional.of("Author"));

        // 2020-01-01T01:00+01:00 == 2020-01-01T00:00Z
        ZonedDateTime zdt = ZonedDateTime.of(2020, 1, 1, 1, 0, 0, 0, ZoneId.of("Europe/Berlin"));
        when(item.getPubDateAsZonedDateTime()).thenReturn(Optional.of(zdt));

        when(item.getMediaThumbnail()).thenReturn(Optional.empty());
        when(item.getEnclosure()).thenReturn(Optional.empty());

        when(item.getCategories()).thenReturn(List.of("cat1", "cat2"));

        Article a = sut.toArticle(feed, item);

        assertEquals(feed.getId(), a.getFeedId());
        assertEquals(feed.getName(), a.getFeedName());
        assertEquals("Title", a.getTitle());
        assertEquals("Desc", a.getDescription());
        assertEquals("Content", a.getContent());
        assertEquals("https://example.com/a", a.getLink());
        assertEquals("Author", a.getAuthors());
        assertEquals("2020-01-01 00:00:00 UTC", a.getPublished());

        // exact Set#toString order can vary; just assert it contains values
        assertNotNull(a.getCategories());
        assertTrue(a.getCategories().contains("cat1"));
        assertTrue(a.getCategories().contains("cat2"));
    }

    @Test
    void toArticle_prefersMediaThumbnail_overEnclosureImage() {
        Feed feed = new Feed(10, 1, "FeedName", URI.create("https://example.com"), URI.create("https://example.com/rss"));

        MediaRssItem item = mock(MediaRssItem.class);
        when(item.getTitle()).thenReturn(Optional.of("Title"));
        when(item.getDescription()).thenReturn(Optional.of(""));
        when(item.getContent()).thenReturn(Optional.of(""));
        when(item.getLink()).thenReturn(Optional.of(""));
        when(item.getAuthor()).thenReturn(Optional.of(""));
        when(item.getPubDateAsZonedDateTime()).thenReturn(Optional.empty());
        when(item.getCategories()).thenReturn(List.of());

        MediaThumbnail thumb = mock(MediaThumbnail.class);
        when(thumb.getUrl()).thenReturn("https://cdn.example/thumb.jpg");
        when(item.getMediaThumbnail()).thenReturn(Optional.of(thumb));

        Enclosure enclosure = mock(Enclosure.class);
        when(enclosure.getType()).thenReturn("image/jpeg");
        when(enclosure.getUrl()).thenReturn("https://cdn.example/enclosure.jpg");
        when(item.getEnclosure()).thenReturn(Optional.of(enclosure));

        Article a = sut.toArticle(feed, item);

        assertEquals("https://cdn.example/thumb.jpg", a.getImageUrl());
    }

    @Test
    void toArticle_usesEnclosureImage_whenNoThumbnailAndEnclosureIsImage() {
        Feed feed = new Feed(10, 1, "FeedName", URI.create("https://example.com"), URI.create("https://example.com/rss"));

        MediaRssItem item = mock(MediaRssItem.class);
        when(item.getTitle()).thenReturn(Optional.of("Title"));
        when(item.getDescription()).thenReturn(Optional.of(""));
        when(item.getContent()).thenReturn(Optional.of(""));
        when(item.getLink()).thenReturn(Optional.of(""));
        when(item.getAuthor()).thenReturn(Optional.of(""));
        when(item.getPubDateAsZonedDateTime()).thenReturn(Optional.empty());
        when(item.getCategories()).thenReturn(List.of());

        when(item.getMediaThumbnail()).thenReturn(Optional.empty());

        Enclosure enclosure = mock(Enclosure.class);
        when(enclosure.getType()).thenReturn("image/png");
        when(enclosure.getUrl()).thenReturn("https://cdn.example/enclosure.png");
        when(item.getEnclosure()).thenReturn(Optional.of(enclosure));

        Article a = sut.toArticle(feed, item);

        assertEquals("https://cdn.example/enclosure.png", a.getImageUrl());
    }
}