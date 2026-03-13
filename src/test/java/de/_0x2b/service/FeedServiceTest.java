package de._0x2b.service;

import com.apptasticsoftware.rssreader.module.mediarss.MediaRssItem;
import de._0x2b.model.Article;
import de._0x2b.model.Feed;
import de._0x2b.model.Icon;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.repository.FeedRepository;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    HTTPSService httpsService;
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
        Feed feed = new Feed(-1, -1, "name", URI.create("https://site.example"), URI.create("https://site.example/rss"));
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
        Feed feed = new Feed(-1, -1, "name", URI.create("https://site.example"), URI.create("https://site.example/rss"));

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
        Feed enriched = new Feed(-1, -1, "Feed Title", URI.create("https://example.com"), URI.create("https://example.com/rss"));
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
    void update_delegatesToRepository() {
        Feed feed = new Feed(1, 1, "n", URI.create("https://site.example"), URI.create("https://site.example/rss"));
        when(feedRepository.update(feed)).thenReturn(1);

        int updated = sut.update(feed);

        assertEquals(1, updated);
        verify(feedRepository).update(feed);
    }

    @Test
    void checkFeedUrl_whenContentTypeIsRss_returnsDirectFeed() throws Exception {
        String url = "https://example.com/rss.xml";

        JsoupProvider provider = mock(JsoupProvider.class);
        Connection.Response response = mock(Connection.Response.class);

        when(provider.execute(url)).thenReturn(response);
        when(response.body()).thenReturn("<rss version=\"2.0\"></rss>");
        when(response.contentType()).thenReturn("application/rss+xml; charset=utf-8");

        List<Feed> feeds = sut.checkFeedUrl(url, provider);

        assertEquals(1, feeds.size());
        assertEquals(URI.create(url), feeds.getFirst().getFeedUrl());
        // direct feed path uses empty name and null url per implementation
    }

    @Test
    void checkFeedUrl_whenHtmlContainsAlternateLinks_extractsAndNormalizesFeedUrls() throws Exception {
        String url = "https://example.com";
        String html = """
                <html><head>
                  <link rel="alternate" type="application/rss+xml" title="My RSS" href="/rss.xml"/>
                  <link rel="alternate" type="application/atom+xml" title="" href="feed://example.com/atom.xml"/>
                  <link rel="alternate" type="application/rss+xml" title="FeedHTTPS" href="feed:https://example.com/x.rss"/>
                </head></html>
                """;

        Document doc = Jsoup.parse(html, url);

        JsoupProvider provider = mock(JsoupProvider.class);
        Connection.Response response = mock(Connection.Response.class);

        when(provider.execute(url)).thenReturn(response);
        when(response.body()).thenReturn(html);
        when(response.contentType()).thenReturn("text/html; charset=utf-8");
        when(response.parse()).thenReturn(doc);

        List<Feed> feeds = sut.checkFeedUrl(url, provider);

        assertEquals(3, feeds.size());

        // 1) relative href resolved by abs:href
        assertEquals("My RSS", feeds.get(0).getName());
        assertEquals(URI.create(url), feeds.get(0).getUrl());
        assertEquals(URI.create("https://example.com/rss.xml"), feeds.get(0).getFeedUrl());

        // 2) feed:// normalized to https://
        assertEquals("https://example.com/atom.xml", feeds.get(1).getName(), "blank title falls back to feedUrl");
        assertEquals(URI.create("https://example.com/atom.xml"), feeds.get(1).getFeedUrl());

        // 3) feed:https:// normalized by stripping 'feed:'
        assertEquals("FeedHTTPS", feeds.get(2).getName());
        assertEquals(URI.create("https://example.com/x.rss"), feeds.get(2).getFeedUrl());
    }

    @Test
    void checkFeedUrl_whenHttpStatusException_returnsEmptyList() throws Exception {
        String url = "https://example.com";
        JsoupProvider provider = mock(JsoupProvider.class);

        // HttpStatusException has ctor (message, statusCode, url)
        when(provider.execute(url)).thenThrow(new HttpStatusException("Forbidden", 403, url));

        List<Feed> feeds = sut.checkFeedUrl(url, provider);

        assertNotNull(feeds);
        assertTrue(feeds.isEmpty());
    }

    @Test
    void checkFeedUrl_whenIOException_returnsEmptyList() throws Exception {
        String url = "https://example.com";
        JsoupProvider provider = mock(JsoupProvider.class);

        when(provider.execute(url)).thenThrow(new IOException("network"));

        List<Feed> feeds = sut.checkFeedUrl(url, provider);

        assertTrue(feeds.isEmpty());
    }

    @Test
    void checkFeedUrl_whenMalformedUrl_returnsEmptyList() throws Exception {
        String url = "notaurl";
        JsoupProvider provider = mock(JsoupProvider.class);

        when(provider.execute(url)).thenThrow(new IllegalArgumentException("bad url"));

        List<Feed> feeds = sut.checkFeedUrl(url, provider);

        assertTrue(feeds.isEmpty());
    }

    @Test
    void refresh_allFeeds_whenFetchEmpty_doesNotStoreArticles() {
        Feed feed = new Feed(1, 1, "n", URI.create("https://example.com"), URI.create("https://example.com/rss"));

        when(feedRepository.findAll()).thenReturn(List.of(feed));
        when(httpsService.fetchUriAsStream(feed.getFeedUrl())).thenReturn(Optional.empty());

        sut.refresh();

        verify(articleRepository, never()).create(anyList());
    }

    @Test
    void refresh_singleFeed_whenStatusNot200_doesNotStoreArticles() {
        Feed feed = new Feed(1, 1, "n", URI.create("https://example.com"), URI.create("https://example.com/rss"));

        when(feedRepository.findOne(1)).thenReturn(List.of(feed));

        // You need to return whatever type HTTPSService returns; we mock it as Object with methods via deep stubs:
        var httpResponse = mock(java.net.http.HttpResponse.class, RETURNS_DEEP_STUBS);
        when(httpResponse.statusCode()).thenReturn(500);

        when(httpsService.fetchUriAsStream(feed.getFeedUrl())).thenReturn(Optional.of(httpResponse));

        sut.refresh(1);

        verify(articleRepository, never()).create(anyList());
    }

    @Test
    void parseFeed_whenFetchEmpty_doesNothing() {
        Feed feed = new Feed(1, 1, "Feed", URI.create("https://example.com"), URI.create("https://example.com/rss"));
        when(httpsService.fetchUriAsStream(feed.getFeedUrl())).thenReturn(Optional.empty());

        sut.parseFeed(feed);

        verifyNoInteractions(mediaRssParser, articleMapper, articleRepository);
    }

    @Test
    void parseFeed_whenStatusNot200_doesNothing() {
        Feed feed = new Feed(1, 1, "Feed", URI.create("https://example.com"), URI.create("https://example.com/rss"));

        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> resp = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(500);
        when(httpsService.fetchUriAsStream(feed.getFeedUrl())).thenReturn(Optional.of(resp));

        sut.parseFeed(feed);

        verifyNoInteractions(mediaRssParser, articleMapper, articleRepository);
    }

    @Test
    void parseFeed_whenStatus200_parsesMapsAndStoresArticles() {
        Feed feed = new Feed(5, 1, "MyFeed", URI.create("https://example.com"), URI.create("https://example.com/rss"));

        // response body InputStream
        InputStream body = new ByteArrayInputStream("<rss/>".getBytes());

        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> resp = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn(body);
        when(httpsService.fetchUriAsStream(feed.getFeedUrl())).thenReturn(Optional.of(resp));

        MediaRssItem item1 = mock(MediaRssItem.class);
        MediaRssItem item2 = mock(MediaRssItem.class);
        when(mediaRssParser.parse(body)).thenReturn(List.of(item1, item2));

        Article a1 = new Article(-1, feed.getId(), feed.getName(),
                "t1", "d1", "c1", "l1", "2020-01-01 00:00:00 UTC",
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

        verify(mediaRssParser).parse(body);
        verify(articleMapper).toArticle(feed, item1);
        verify(articleMapper).toArticle(feed, item2);
    }

    @Test
    void parseFeed_whenMapperThrowsForOneItem_continuesWithOthers() {
        Feed feed = new Feed(5, 1, "MyFeed", URI.create("https://example.com"), URI.create("https://example.com/rss"));

        InputStream body = new ByteArrayInputStream("<rss/>".getBytes());

        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> resp = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);
        when(resp.body()).thenReturn(body);
        when(httpsService.fetchUriAsStream(feed.getFeedUrl())).thenReturn(Optional.of(resp));

        MediaRssItem bad = mock(MediaRssItem.class);
        MediaRssItem good = mock(MediaRssItem.class);
        when(mediaRssParser.parse(body)).thenReturn(List.of(bad, good));

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
    }

    @Test
    void delete_whenAllDeletesSucceed_returnsRepoResult() throws Exception {
        int feedId = 10;

        // articleRepository.deleteByFeed is void but can throw SQLException
        doNothing().when(articleRepository).deleteByFeed(feedId);
        when(feedRepository.delete(feedId)).thenReturn(1);

        int result = sut.delete(feedId);

        assertEquals(1, result);
        verify(articleRepository).deleteByFeed(feedId);
        verify(feedRepository).delete(feedId);
    }

    @Test
    void delete_whenArticleDeleteThrowsSQLException_returnsMinus1AndDoesNotDeleteFeed() throws Exception {
        int feedId = 10;

        doThrow(new SQLException("db")).when(articleRepository).deleteByFeed(feedId);

        int result = sut.delete(feedId);

        assertEquals(-1, result);
        verify(feedRepository, never()).delete(anyInt());
    }

    @Test
    void delete_whenFeedDeleteThrowsSQLException_returnsMinus1() throws Exception {
        int feedId = 10;

        doNothing().when(articleRepository).deleteByFeed(feedId);
        when(feedRepository.delete(feedId)).thenThrow(new SQLException("db"));

        int result = sut.delete(feedId);

        assertEquals(-1, result);
        verify(articleRepository).deleteByFeed(feedId);
        verify(feedRepository).delete(feedId);
    }
}
