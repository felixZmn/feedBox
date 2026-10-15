package de._0x2b.service;

import de._0x2b.http.FetchResult;
import de._0x2b.http.OutboundHttpClient;
import de._0x2b.model.Feed;
import de._0x2b.model.Icon;
import de._0x2b.repository.IconRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IconServiceTest {

    @Mock
    IconRepository iconRepository;
    @Mock
    OutboundHttpClient outboundHttpClient;

    @InjectMocks
    IconService sut;

    private static URI htmlUriFor(Feed feed) {
        String scheme = feed.getUrl().getScheme() != null ? feed.getUrl().getScheme() : "https";
        return URI.create(scheme + "://" + feed.getUrl().getHost() + feed.getUrl().getPath());
    }

    private static FetchResult htmlResult(URI uri, String html) {
        return new FetchResult(uri, uri, 200, "text/html; charset=utf-8",
                html.getBytes(StandardCharsets.UTF_8));
    }

    private static FetchResult bytesResult(URI uri, String contentType, byte[] body) {
        return new FetchResult(uri, uri, 200, contentType, body);
    }

    @Test
    void findOneByFeed_whenFound_returnsRepositoryResult() {
        Icon icon = new Icon(1, 5, new byte[]{1}, "image/png", "f.png", "");
        when(iconRepository.findByFeed(5)).thenReturn(List.of(icon));

        List<Icon> result = sut.findOneByFeed(5);

        assertEquals(1, result.size());
        assertSame(icon, result.getFirst());
        verify(iconRepository).findByFeed(5);
    }

    @Test
    void findOneByFeed_whenMissing_returnsDefaultIcon() {
        when(iconRepository.findByFeed(5)).thenReturn(List.of());

        List<Icon> result = sut.findOneByFeed(5);

        assertFalse(result.isEmpty());
        assertEquals("image/svg+xml", result.getFirst().getMimeType());
    }

    @Test
    void findIcon_whenHtmlHasFaviconLink_fetchesImage() {
        Feed feed = new Feed(1, 1, "n", URI.create("https://example.com/blog"),
                URI.create("https://example.com/rss"));
        URI htmlUri = htmlUriFor(feed);
        String html = """
                <html><head>
                  <link rel="icon" href="/favicon.png" type="image/png"/>
                </head></html>
                """;
        when(outboundHttpClient.fetchIcon(htmlUri)).thenReturn(Optional.of(htmlResult(htmlUri, html)));

        URI iconUri = URI.create("https://example.com/favicon.png");
        byte[] bytes = new byte[]{1, 2, 3};
        when(outboundHttpClient.fetchIcon(iconUri))
                .thenReturn(Optional.of(bytesResult(iconUri, "image/png", bytes)));

        Icon result = sut.findIcon(feed);

        assertNotNull(result);
        assertEquals(feed.getId(), result.getFeedId());
        assertEquals("image/png", result.getMimeType());
        assertArrayEquals(bytes, result.getImage());
        assertEquals(iconUri.toString(), result.getUrl());
    }

    @Test
    void findIcon_whenNoIconInHtml_fallsBackToFaviconIco() {
        Feed feed = new Feed(1, 1, "n", URI.create("https://example.com"),
                URI.create("https://example.com/rss"));
        URI htmlUri = htmlUriFor(feed);
        when(outboundHttpClient.fetchIcon(htmlUri))
                .thenReturn(Optional.of(htmlResult(htmlUri, "<html><head></head></html>")));

        URI fallback = URI.create("https://example.com/favicon.ico");
        byte[] bytes = new byte[]{9};
        when(outboundHttpClient.fetchIcon(fallback))
                .thenReturn(Optional.of(bytesResult(fallback, "image/x-icon", bytes)));

        Icon result = sut.findIcon(feed);

        assertNotNull(result);
        assertEquals("image/x-icon", result.getMimeType());
        assertArrayEquals(bytes, result.getImage());
    }

    @Test
    void findIcon_whenHtmlFetchFails_returnsNull() {
        Feed feed = new Feed(1, 1, "n", URI.create("https://example.com"),
                URI.create("https://example.com/rss"));
        when(outboundHttpClient.fetchIcon(any())).thenReturn(Optional.empty());

        assertNull(sut.findIcon(feed));
    }

    @Test
    void findIcon_whenIconFetchFails_returnsNull() {
        Feed feed = new Feed(1, 1, "n", URI.create("https://example.com"),
                URI.create("https://example.com/rss"));
        URI htmlUri = htmlUriFor(feed);
        when(outboundHttpClient.fetchIcon(htmlUri))
                .thenReturn(Optional.of(htmlResult(htmlUri, "<html><head></head></html>")));
        when(outboundHttpClient.fetchIcon(URI.create("https://example.com/favicon.ico")))
                .thenReturn(Optional.empty());

        assertNull(sut.findIcon(feed));
    }

    @Test
    void fetchFavicon_whenNonImageContentType_clearsImage() {
        Icon icon = new Icon(-1, 1, null, "", "", "https://example.com/not-an-icon");
        URI uri = URI.create(icon.getUrl());
        when(outboundHttpClient.fetchIcon(uri))
                .thenReturn(Optional.of(bytesResult(uri, "text/html", "<html/>".getBytes(StandardCharsets.UTF_8))));

        Icon result = sut.fetchFavicon(icon);

        assertNull(result.getImage());
    }

    @Test
    void fetchFavicon_whenSvgContentType_clearsImage() {
        Icon icon = new Icon(-1, 1, null, "", "", "https://example.com/icon.svg");
        URI uri = URI.create(icon.getUrl());
        when(outboundHttpClient.fetchIcon(uri))
                .thenReturn(Optional.of(bytesResult(uri, "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8))));

        Icon result = sut.fetchFavicon(icon);

        assertNull(result.getImage());
    }

    @Test
    void fetchFavicon_whenFetchEmpty_leavesImageUnset() {
        Icon icon = new Icon(-1, 1, null, "", "", "https://example.com/missing.ico");
        when(outboundHttpClient.fetchIcon(URI.create(icon.getUrl()))).thenReturn(Optional.empty());

        Icon result = sut.fetchFavicon(icon);

        assertNull(result.getImage());
    }

    @Test
    void create_delegatesToRepository() {
        Icon icon = new Icon(-1, 3, new byte[]{1}, "image/png", "a.png", "");
        sut.create(icon);
        verify(iconRepository).create(icon);
    }

    @Test
    void getCharsetFromContentType_parsesCharsetOrDefaultsUtf8() {
        assertEquals(StandardCharsets.UTF_8, IconService.getCharsetFromContentType(null));
        assertEquals(StandardCharsets.UTF_8, IconService.getCharsetFromContentType("text/html"));
        assertEquals(StandardCharsets.ISO_8859_1,
                IconService.getCharsetFromContentType("text/html; charset=ISO-8859-1"));
    }
}
