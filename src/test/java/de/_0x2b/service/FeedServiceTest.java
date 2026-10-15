package de._0x2b.service;

import com.apptasticsoftware.rssreader.module.mediarss.MediaRssItem;
import de._0x2b.exception.DataAccessException;
import de._0x2b.exception.NotFoundException;
import de._0x2b.http.FetchResult;
import de._0x2b.http.OutboundHttpClient;
import de._0x2b.model.Article;
import de._0x2b.model.Feed;
import de._0x2b.model.Icon;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.repository.FeedRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    OutboundHttpClient outboundHttpClient;
    @Mock
    IconService iconService;
    @Mock
    FeedRepository feedRepository;
    @Mock
    ArticleRepository articleRepository;
    @Mock
    MediaRssParser mediaRssParser;
    @Mock
    ArticleMapper articleMapper;

    @InjectMocks
    FeedService sut;

    // -----------------------
    // create(feed, icon)
    // -----------------------

    @Test
    void create_withIcon_storesFeedAndIconAndReturnsFeedId() {
        Feed feed = new Feed(-1, -1, "name", URI.create("https://site.example"),
                URI.create("https://site.example/rss"));
        Icon icon = new Icon(); // adapt if your Icon needs ctor args

        when(feedRepository.create(feed)).thenReturn(42);

        int id = sut.create(feed, icon);

        assertEquals(42, id);
        verify(feedRepository).create(feed);
        assertEquals(42, icon.getFeedId(), "Icon.feedId must be set to returned feed id");
        verify(iconService).create(icon);
    }

    @Test
    void create_withNullIcon_onlyStoresFeed() {
        Feed feed = new Feed(-1, -1, "name", URI.create("https://site.example"),
                URI.create("https://site.example/rss"));

        when(feedRepository.create(feed)).thenReturn(7);

        int id = sut.create(feed, null);

        assertEquals(7, id);
        verify(feedRepository).create(feed);
        verify(iconService, never()).create(any());
    }

    @Test
    void create_callsMetadataAndIconLookup_andStores() throws Exception {
        // Spy to stub getFeedMetadata without hitting real RssReader
        FeedService spy = spy(sut);

        Feed input = new Feed(-1, -1, "", null, URI.create("https://example.com/rss"));
        Feed enriched = new Feed(-1, -1, "Feed Title", URI.create("https://example.com"),
                URI.create("https://example.com/rss"));
        Icon icon = new Icon();

        doReturn(enriched).when(spy).getFeedMetadata(input);
        when(iconService.findIcon(enriched)).thenReturn(icon);
        when(feedRepository.create(enriched)).thenReturn(99);

        int id = spy.create(input);

        assertEquals(99, id);
        verify(spy).getFeedMetadata(input);
        verify(iconService).findIcon(enriched);
        verify(feedRepository).create(enriched);
        verify(iconService).create(icon);
        assertEquals(99, icon.getFeedId());
    }

    @Test
    void update_whenFound_returnsId() {
        Feed feed = new Feed(1, 1, "n", URI.create("https://site.example"), URI.create("https://site.example/rss"));
        when(feedRepository.update(feed)).thenReturn(1);

        int updated = sut.update(feed);

        assertEquals(1, updated);
        verify(feedRepository).update(feed);
    }

    @Test
    void update_whenMissing_throwsNotFound() {
        Feed feed = new Feed(1, 1, "n", URI.create("https://site.example"), URI.create("https://site.example/rss"));
        when(feedRepository.update(feed)).thenReturn(0);

        assertThrows(NotFoundException.class, () -> sut.update(feed));
        verify(feedRepository).update(feed);
    }

    @Test
    void findAll_attachesUnreadCountsFromArticleRepository() {
        Feed withUnread = new Feed(1, 1, "n", URI.create("https://site.example"),
                URI.create("https://site.example/rss"));
        Feed withoutUnread = new Feed(2, 1, "m", URI.create("https://other.example"),
                URI.create("https://other.example/rss"));
        when(feedRepository.findAll()).thenReturn(List.of(withUnread, withoutUnread));
        when(articleRepository.countUnreadByFeed()).thenReturn(java.util.Map.of(1, 5));

        List<Feed> result = sut.findAll();

        assertSame(withUnread, result.get(0));
        assertSame(withoutUnread, result.get(1));
        assertEquals(5, withUnread.getUnreadCount());
        assertEquals(0, withoutUnread.getUnreadCount());
        verify(feedRepository).findAll();
        verify(articleRepository).countUnreadByFeed();
    }

    @Test
    void checkFeedUrl_whenContentTypeIsRss_returnsDirectFeed() {
        String url = "https://example.com/rss.xml";
        URI uri = URI.create(url);
        when(outboundHttpClient.fetchFeed(uri)).thenReturn(Optional.of(new FetchResult(
                uri, uri, 200, "application/rss+xml; charset=utf-8",
                "<rss version=\"2.0\"></rss>".getBytes(StandardCharsets.UTF_8))));

        List<Feed> feeds = sut.checkFeedUrl(url);

        assertEquals(1, feeds.size());
        assertEquals(uri, feeds.getFirst().getFeedUrl());
    }

    @Test
    void checkFeedUrl_whenHtmlContainsAlternateLinks_extractsAndNormalizesFeedUrls() {
        String url = "https://example.com";
        URI uri = URI.create(url);
        String html = """
                <html><head>
                  <link rel="alternate" type="application/rss+xml" title="My RSS" href="/rss.xml"/>
                  <link rel="alternate" type="application/atom+xml" title="" href="feed://example.com/atom.xml"/>
                  <link rel="alternate" type="application/rss+xml" title="FeedHTTPS" href="feed:https://example.com/x.rss"/>
                </head></html>
                """;
        when(outboundHttpClient.fetchFeed(uri)).thenReturn(Optional.of(new FetchResult(
                uri, uri, 200, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8))));

        List<Feed> feeds = sut.checkFeedUrl(url);

        assertEquals(3, feeds.size());

        assertEquals("My RSS", feeds.get(0).getName());
        assertEquals(uri, feeds.get(0).getUrl());
        assertEquals(URI.create("https://example.com/rss.xml"), feeds.get(0).getFeedUrl());

        assertEquals("https://example.com/atom.xml", feeds.get(1).getName(), "blank title falls back to feedUrl");
        assertEquals(URI.create("https://example.com/atom.xml"), feeds.get(1).getFeedUrl());

        assertEquals("FeedHTTPS", feeds.get(2).getName());
        assertEquals(URI.create("https://example.com/x.rss"), feeds.get(2).getFeedUrl());
    }

    @Test
    void checkFeedUrl_whenFetchFails_returnsEmptyList() {
        URI uri = URI.create("https://example.com");
        when(outboundHttpClient.fetchFeed(uri)).thenReturn(Optional.empty());

        assertTrue(sut.checkFeedUrl(uri.toString()).isEmpty());
    }

    @Test
    void checkFeedUrl_whenMalformedUrl_returnsEmptyList() {
        // Relative / opaque URIs are rejected by the outbound client.
        when(outboundHttpClient.fetchFeed(any())).thenReturn(Optional.empty());

        assertTrue(sut.checkFeedUrl("notaurl").isEmpty());
    }

    @Test
    void refresh_allFeeds_whenFetchEmpty_doesNotStoreArticles() throws SQLException {
        Feed feed = new Feed(1, 1, "n", URI.create("https://example.com"), URI.create("https://example.com/rss"));

        when(feedRepository.findAll()).thenReturn(List.of(feed));
        when(outboundHttpClient.fetchFeed(feed.getFeedUrl())).thenReturn(Optional.empty());

        sut.refresh();

        verify(articleRepository, never()).create(anyList());
        verify(feedRepository).markRefreshError(eq(1), anyString());
    }

    @Test
    void parseFeed_whenFetchEmpty_doesNothing_andMarksError() throws SQLException {
        Feed feed = new Feed(1, 1, "Feed", URI.create("https://example.com"), URI.create("https://example.com/rss"));
        when(outboundHttpClient.fetchFeed(feed.getFeedUrl())).thenReturn(Optional.empty());

        sut.parseFeed(feed);

        verifyNoInteractions(mediaRssParser, articleMapper, articleRepository);
        verify(feedRepository).markRefreshError(eq(1), anyString());
    }

    @Test
    void parseFeed_whenStatus200_parsesMapsAndStoresArticles_andMarksSuccess() throws SQLException {
        Feed feed = new Feed(5, 1, "MyFeed", URI.create("https://example.com"), URI.create("https://example.com/rss"));
        byte[] body = "<rss/>".getBytes(StandardCharsets.UTF_8);
        FetchResult result = new FetchResult(feed.getFeedUrl(), feed.getFeedUrl(), 200, "application/rss+xml", body);
        when(outboundHttpClient.fetchFeed(feed.getFeedUrl())).thenReturn(Optional.of(result));

        MediaRssItem item1 = mock(MediaRssItem.class);
        MediaRssItem item2 = mock(MediaRssItem.class);
        when(mediaRssParser.parse(any())).thenReturn(List.of(item1, item2));

        Article a1 = new Article(-1, feed.getId(), feed.getName(),
                "t1", "d1", "c1", "l1", Instant.parse("2020-01-01T00:00:00Z"),
                "auth1", "img1", "cats1");
        Article a2 = new Article(-1, feed.getId(), feed.getName(),
                "t2", "d2", "c2", "l2", null,
                "auth2", "", "cats2");

        when(articleMapper.toArticle(feed, item1)).thenReturn(a1);
        when(articleMapper.toArticle(feed, item2)).thenReturn(a2);

        sut.parseFeed(feed);

        ArgumentCaptor<List<Article>> captor = ArgumentCaptor.forClass(List.class);
        verify(articleRepository).create(captor.capture());

        List<Article> stored = captor.getValue();
        assertEquals(2, stored.size());
        assertSame(a1, stored.get(0));
        assertSame(a2, stored.get(1));

        verify(articleMapper).toArticle(feed, item1);
        verify(articleMapper).toArticle(feed, item2);
        verify(feedRepository).markRefreshSuccess(eq(feed.getId()), isNull());
        verify(feedRepository, never()).markRefreshError(anyInt(), anyString());
    }

    @Test
    void parseFeed_whenMapperThrowsForOneItem_continuesWithOthers() throws SQLException {
        Feed feed = new Feed(5, 1, "MyFeed", URI.create("https://example.com"), URI.create("https://example.com/rss"));
        FetchResult result = new FetchResult(feed.getFeedUrl(), feed.getFeedUrl(), 200, "application/rss+xml",
                "<rss/>".getBytes(StandardCharsets.UTF_8));
        when(outboundHttpClient.fetchFeed(feed.getFeedUrl())).thenReturn(Optional.of(result));

        MediaRssItem bad = mock(MediaRssItem.class);
        MediaRssItem good = mock(MediaRssItem.class);
        when(mediaRssParser.parse(any())).thenReturn(List.of(bad, good));

        when(articleMapper.toArticle(feed, bad)).thenThrow(new RuntimeException("boom"));
        Article aGood = new Article(-1, feed.getId(), feed.getName(),
                "t", "d", "c", "l", null, "", "", "");
        when(articleMapper.toArticle(feed, good)).thenReturn(aGood);

        sut.parseFeed(feed);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Article>> captor = ArgumentCaptor.forClass(List.class);
        verify(articleRepository).create(captor.capture());

        List<Article> stored = captor.getValue();
        assertEquals(1, stored.size());
        assertSame(aGood, stored.getFirst());
        verify(feedRepository).markRefreshSuccess(eq(feed.getId()), isNull());
    }

    @Test
    void parseFeed_whenArticleRepositoryCreateThrows_marksError() throws SQLException {
        Feed feed = new Feed(5, 1, "MyFeed", URI.create("https://example.com"), URI.create("https://example.com/rss"));
        FetchResult result = new FetchResult(feed.getFeedUrl(), feed.getFeedUrl(), 200, "application/rss+xml",
                "<rss/>".getBytes(StandardCharsets.UTF_8));
        when(outboundHttpClient.fetchFeed(feed.getFeedUrl())).thenReturn(Optional.of(result));

        MediaRssItem item = mock(MediaRssItem.class);
        when(mediaRssParser.parse(any())).thenReturn(List.of(item));
        when(articleMapper.toArticle(feed, item))
                .thenReturn(new Article(-1, feed.getId(), feed.getName(), "t", "d", "c", "l", null, "", "", ""));

        doThrow(new de._0x2b.exception.DataAccessException("DB down", new SQLException("down")))
                .when(articleRepository).create(anyList());

        sut.parseFeed(feed);

        verify(feedRepository).markRefreshError(eq(feed.getId()), contains("DB down"));
        verify(feedRepository, never()).markRefreshSuccess(anyInt(), any());
    }

    @Test
    void delete_whenAllDeletesSucceed_completes() throws Exception {
        int feedId = 10;

        doNothing().when(articleRepository).deleteByFeed(feedId);
        when(feedRepository.delete(feedId)).thenReturn(1);

        sut.delete(feedId);

        verify(articleRepository).deleteByFeed(feedId);
        verify(feedRepository).delete(feedId);
    }

    @Test
    void delete_whenArticleDeleteThrowsSQLException_throwsDataAccessAndDoesNotDeleteFeed()
            throws Exception {
        int feedId = 10;

        doThrow(new SQLException("db")).when(articleRepository).deleteByFeed(feedId);

        assertThrows(DataAccessException.class, () -> sut.delete(feedId));
        verify(feedRepository, never()).delete(anyInt());
    }

    @Test
    void delete_whenFeedMissing_throwsNotFound() throws Exception {
        int feedId = 10;

        doNothing().when(articleRepository).deleteByFeed(feedId);
        when(feedRepository.delete(feedId)).thenReturn(0);

        assertThrows(NotFoundException.class, () -> sut.delete(feedId));
        verify(articleRepository).deleteByFeed(feedId);
        verify(feedRepository).delete(feedId);
    }
}
