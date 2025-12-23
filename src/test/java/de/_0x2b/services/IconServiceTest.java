package de._0x2b.services;

import de._0x2b.models.Feed;
import de._0x2b.models.Icon;
import de._0x2b.repositories.IconRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static de._0x2b.services.IconService.getCharsetFromContentType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IconServiceTest {

    private static final byte[] IMAGE_BYTES = new byte[]{1, 2, 3}; // dummy favicon content
    private IconRepository iconRepository;
    private IconService iconService;
    private HTTPSService httpsService;

    @BeforeEach
    void setUp() {
        iconRepository = mock(IconRepository.class);
        httpsService = mock(HTTPSService.class);
        iconService = new IconService(iconRepository);
    }

    @Test
    void getCharsetFromContentTypeTest() {
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType(null));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("\"text/html; charset=UTF-8\""));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("text/javascript; charset=utf-8"));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("multipart/form-data; boundary=ExampleBoundaryString"));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("multipart/form-data; boundary=ExampleBoundaryString; charset=utf-8"));
        assertEquals(StandardCharsets.US_ASCII, getCharsetFromContentType("Content-Type: application/xml; charset=US-ASCII"));
        assertEquals(StandardCharsets.UTF_8, getCharsetFromContentType("Content-Type: text/plain; charset=LATIN-DUMMY"));
    }

    @Test
    void findOneByFeedTest() throws IOException {
        // mock'n'prepare
        var firstIcon = new Icon(1, 1, IMAGE_BYTES, "image/x-icon", "favicon.ico", "example.com/favicon.ico");
        var defaultIcon = new Icon(-1, -1, getClass().getResourceAsStream("/static/icons/rss.svg").readAllBytes(), "image/svg+xml", "icon.svg", "");

        when(iconRepository.findByFeed(1)).thenReturn(List.of(firstIcon));
        when(iconRepository.findByFeed(1337)).thenReturn(Collections.emptyList());

        // Act
        var shouldBeFirstIcon = iconService.findOneByFeed(1);
        var shouldBeDefault = iconService.findOneByFeed(1337);

        // Check
        assertEquals(firstIcon, shouldBeFirstIcon.getFirst());
        assertEquals(1, shouldBeFirstIcon.size());
        assertEquals(defaultIcon, shouldBeDefault.getFirst());
        assertEquals(1, shouldBeDefault.size());
    }

    @Test
    void findIconTest() {
        // mock'n'prepare
        Feed feed = mock(Feed.class);
        when(feed.getId()).thenReturn(123);
        URI uri = URI.create("https://example.org/feed.xml");
        when(feed.getURI()).thenReturn(uri);

        MockedStatic<HTTPSService> httpsServiceStatic;
        httpsServiceStatic = mockStatic(HTTPSService.class);
        httpsServiceStatic.when(HTTPSService::getInstance).thenReturn(httpsService);

        String html = """
                <html>
                    <head>
                      <link rel="shortcut icon" href="https://example.org/test.ico"/>
                    </head>
                    <body></body>
                </html>
                """;
        var response = mock(java.net.http.HttpResponse.class);
        when(response.body()).thenReturn(html.getBytes(StandardCharsets.UTF_8));
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(java.util.Map.of("Content-Type", List.of("text/html; charset=UTF-8")), (k, v) -> true));
        when(response.uri()).thenReturn(uri);
        when(httpsService.fetchURI(any(URI.class))).thenReturn(response);
        var favResponse = mock(java.net.http.HttpResponse.class);
        when(favResponse.body()).thenReturn(IMAGE_BYTES);
        when(favResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(java.util.Map.of("content-type", List.of("image/x-icon")), (k, v) -> true));
        when(httpsService.fetchURI(eq(URI.create("https://example.org/test.ico")))).thenReturn(favResponse);
        ArgumentCaptor<Icon> iconCaptor = ArgumentCaptor.forClass(Icon.class);

        // do
        iconService.findIcon(feed);

        // assert
        verify(iconRepository, times(1)).create(any(Icon.class));
        verify(iconRepository).create(iconCaptor.capture());
        Icon storedIcon = iconCaptor.getValue();
        assertEquals(feed.getId(), storedIcon.getFeedId());
        assertEquals("https://example.org/test.ico", storedIcon.getUrl());
        assertNotNull(storedIcon.getImage());
        assertArrayEquals(IMAGE_BYTES, storedIcon.getImage());
        assertEquals("image/x-icon", storedIcon.getMimeType());
    }
}
