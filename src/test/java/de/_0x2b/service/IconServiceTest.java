package de._0x2b.service;

import de._0x2b.model.Feed;
import de._0x2b.model.Icon;
import de._0x2b.repository.IconRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IconServiceTest {

    @Mock
    IconRepository iconRepository;
    @Mock
    HTTPSService httpsService;

    @InjectMocks
    IconService sut;

    // Helper: replicate IconService.constructHtmlUris result for feeds with empty path
    private static URI expectedHtmlUriFor(Feed feed) {
        URI u = feed.getUrl();
        String scheme = (u.getScheme() == null) ? "https" : u.getScheme();
        String host = u.getHost();
        String path = u.getPath(); // empty string if none
        return URI.create(scheme + "://" + host + path);
    }

    @Test
    void getCharsetFromContentType_null_returnsUtf8() {
        assertEquals(StandardCharsets.UTF_8, IconService.getCharsetFromContentType(null));
    }

    @Test
    void getCharsetFromContentType_validCharset_returnsThatCharset() {
        Charset cs = IconService.getCharsetFromContentType("text/html; charset=ISO-8859-1");
        assertEquals(Charset.forName("ISO-8859-1"), cs);
    }

    @Test
    void getCharsetFromContentType_quotedCharset_isHandled() {
        Charset cs = IconService.getCharsetFromContentType("text/html; charset=\"UTF-8\"");
        assertEquals(StandardCharsets.UTF_8, cs);
    }

    @Test
    void getCharsetFromContentType_unsupportedCharset_fallsBackToUtf8() {
        Charset cs = IconService.getCharsetFromContentType("text/html; charset=NO_SUCH_CHARSET");
        assertEquals(StandardCharsets.UTF_8, cs);
    }

    @Test
    void findOneByFeed_whenRepoReturnsIcons_returnsThem() {
        List<Icon> icons = List.of(new Icon(-1, 1, new byte[]{1}, "image/png", "f.png", "https://x"));
        when(iconRepository.findByFeed(1)).thenReturn(icons);

        List<Icon> result = sut.findOneByFeed(1);

        assertSame(icons, result);
        verify(iconRepository).findByFeed(1);
    }

    @Test
    void findOneByFeed_whenRepoReturnsEmpty_returnsDefaultIconList() {
        when(iconRepository.findByFeed(1)).thenReturn(List.of());

        List<Icon> result = sut.findOneByFeed(1);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertNotNull(result.getFirst().getImage());
        verify(iconRepository).findByFeed(1);
    }
    
    @Test
    void fetchFavicon_whenFetchFails_returnsIconWithoutImage() {
        Icon icon = new Icon(-1, 1, null, "", "", "https://example.com/favicon.ico");
        when(httpsService.fetchAsBytes(URI.create(icon.getUrl()))).thenReturn(Optional.empty());

        Icon result = sut.fetchFavicon(icon);

        assertSame(icon, result);
        assertNull(result.getImage());
    }

    @Test
    void fetchFavicon_whenFetchSucceeds_setsImageAndMimeType() {
        Icon icon = new Icon(-1, 1, null, "", "", "https://example.com/favicon.ico");

        byte[] body = new byte[]{1, 2, 3};
        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> resp = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(resp.body()).thenReturn(body);
        when(resp.headers()).thenReturn(HttpHeaders.of(
                Map.of("content-type", List.of("image/x-icon")),
                (k, v) -> true
        ));
        when(httpsService.fetchAsBytes(URI.create(icon.getUrl()))).thenReturn(Optional.of(resp));

        Icon result = sut.fetchFavicon(icon);

        assertArrayEquals(body, result.getImage());
        assertEquals("image/x-icon", result.getMimeType());
    }

    @Test
    void findIcon_whenHtmlHasAbsoluteIconUrl_andFaviconFetchSucceeds_returnsIcon() {
        Feed feed = new Feed(7, 1, "Feed", URI.create("https://example.com"), URI.create("https://example.com/rss"));
        URI htmlUri = expectedHtmlUriFor(feed);

        String html = """
                <html><head>
                  <link rel="icon" href="https://cdn.example.com/i.png">
                </head></html>
                """;

        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> htmlResp = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(htmlResp.body()).thenReturn(html.getBytes(StandardCharsets.UTF_8));
        when(htmlResp.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("text/html; charset=UTF-8")),
                (k, v) -> true
        ));
        when(httpsService.fetchAsBytes(htmlUri)).thenReturn(Optional.of(htmlResp));

        byte[] iconBytes = new byte[]{9, 8, 7};
        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> favResp = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(favResp.body()).thenReturn(iconBytes);
        when(favResp.headers()).thenReturn(HttpHeaders.of(
                Map.of("content-type", List.of("image/png")),
                (k, v) -> true
        ));
        when(httpsService.fetchAsBytes(URI.create("https://cdn.example.com/i.png")))
                .thenReturn(Optional.of(favResp));

        Icon result = sut.findIcon(feed);

        assertNotNull(result);
        assertArrayEquals(iconBytes, result.getImage());
        assertEquals("image/png", result.getMimeType());
        assertEquals("https://cdn.example.com/i.png", result.getUrl());
    }

    @Test
    void findIcon_whenHtmlHasRelativeIconUrl_buildsAbsoluteUrl_andFetches() {
        Feed feed = new Feed(7, 1, "Feed", URI.create("https://example.com"), URI.create("https://example.com/rss"));
        URI htmlUri = expectedHtmlUriFor(feed);

        String html = """
                <html><head>
                  <link rel="icon" href="/favicon-32.png">
                </head></html>
                """;

        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> htmlResp = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(htmlResp.body()).thenReturn(html.getBytes(StandardCharsets.UTF_8));
        when(htmlResp.uri()).thenReturn(htmlUri);
        when(htmlResp.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("text/html; charset=UTF-8")),
                (k, v) -> true
        ));

        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> favResp = (HttpResponse<byte[]>) mock(HttpResponse.class);
        byte[] iconBytes = new byte[]{1};
        when(favResp.body()).thenReturn(iconBytes);
        when(favResp.headers()).thenReturn(HttpHeaders.of(
                Map.of("content-type", List.of("image/png")),
                (k, v) -> true
        ));

        URI expectedIconUri = URI.create("https://example.com/favicon-32.png");

        when(httpsService.fetchAsBytes(htmlUri)).thenReturn(Optional.of(htmlResp));
        when(httpsService.fetchAsBytes(expectedIconUri)).thenReturn(Optional.of(favResp));

        Icon result = sut.findIcon(feed);

        assertNotNull(result);
        assertArrayEquals(iconBytes, result.getImage());
        assertEquals(expectedIconUri.toString(), result.getUrl());
    }

    @Test
    void findIcon_whenHtmlNoIcon_fallsBackToFaviconIco() {
        Feed feed = new Feed(7, 1, "Feed", URI.create("https://example.com"), URI.create("https://example.com/rss"));
        URI htmlUri = expectedHtmlUriFor(feed);

        String html = "<html><head></head><body>No icon</body></html>";

        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> htmlResp = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(htmlResp.body()).thenReturn(html.getBytes(StandardCharsets.UTF_8));
        when(htmlResp.uri()).thenReturn(htmlUri);
        when(htmlResp.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("text/html; charset=UTF-8")),
                (k, v) -> true
        ));

        URI expectedIconUri = URI.create("https://example.com/favicon.ico");

        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> favResp = (HttpResponse<byte[]>) mock(HttpResponse.class);
        byte[] iconBytes = new byte[]{2, 2};
        when(favResp.body()).thenReturn(iconBytes);
        when(favResp.headers()).thenReturn(HttpHeaders.of(
                Map.of("content-type", List.of("image/x-icon")),
                (k, v) -> true
        ));

        when(httpsService.fetchAsBytes(htmlUri)).thenReturn(Optional.of(htmlResp));
        when(httpsService.fetchAsBytes(expectedIconUri)).thenReturn(Optional.of(favResp));

        Icon result = sut.findIcon(feed);

        assertNotNull(result);
        assertArrayEquals(iconBytes, result.getImage());
        assertEquals(expectedIconUri.toString(), result.getUrl());
    }

    @Test
    void findIcon_whenHtmlFetchFails_returnsNull() {
        Feed feed = new Feed(7, 1, "Feed", URI.create("https://example.com"), URI.create("https://example.com/rss"));
        URI htmlUri = expectedHtmlUriFor(feed);

        when(httpsService.fetchAsBytes(htmlUri)).thenReturn(Optional.empty());

        Icon result = sut.findIcon(feed);

        assertNull(result);
    }

    @Test
    void create_delegatesToRepository() {
        Icon icon = new Icon(-1, 1, new byte[]{1}, "image/png", "x.png", "https://x");
        sut.create(icon);
        verify(iconRepository).create(icon);
    }
}