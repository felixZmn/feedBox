package de._0x2b.services;

import de._0x2b.models.Feed;
import de._0x2b.repositories.ArticleRepository;
import de._0x2b.repositories.FeedRepository;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeedServiceTest {
    private FeedService feedServce;

    @BeforeEach
    void setUp(){
        var mockIconService = mock(IconService.class);
        var mockFeedRepository = mock(FeedRepository.class);
        var mockArticleRepository = mock(ArticleRepository.class);
        feedServce = new FeedService(mockIconService, mockFeedRepository, mockArticleRepository);
    }

    @Test
    void checkFeedTest() throws URISyntaxException, IOException {
        // mock'n'prepare
        JsoupProvider mockProvider = mock(JsoupProvider.class);
        Connection.Response mockResponse = mock(Connection.Response.class);

        List<String> urls = new ArrayList<>(List.of(
                "https://html.without.feed.example",
                "https://feed.example",
                "https://html.with.feed.example",
                "https://html.with.multiple.feeds.example"
        ));
        List<String> fileContents = new ArrayList<>(List.of(
                Files.readString(Path.of(getClass().getResource("/services/noLink.html").toURI())),
                Files.readString(Path.of(getClass().getResource("/services/feed.html").toURI())),
                Files.readString(Path.of(getClass().getResource("/services/oneLink.html").toURI())),
                Files.readString(Path.of(getClass().getResource("/services/twoLinks.html").toURI()))
        ));
        List<String> contentTypes = new ArrayList<>(List.of(
                "text/html; charset=UTF-8",
                "application/xml; charset=utf-8",
                "text/html; charset=utf-8",
                "text/html; charset=UTF-8"
        ));

        // do
        List<List<Feed>> result = new ArrayList<>();

        for (int i = 0; i < urls.size(); i++) {
            when(mockProvider.execute(urls.get(i))).thenReturn(mockResponse);
            when(mockResponse.parse()).thenReturn(Jsoup.parse(fileContents.get(i)));
            when(mockResponse.body()).thenReturn(fileContents.get(i));
            when(mockResponse.contentType()).thenReturn(contentTypes.get(i));

            result.add(feedServce.checkFeed(urls.get(i), mockProvider));
        }

        // assert
        assertEquals(result.get(0).size(), 0);
        assertEquals(result.get(1).getFirst().getFeedURI(), URI.create("https://feed.example"));
        assertEquals(result.get(2).getFirst().getFeedURI(), URI.create("https://example.com/feed"));
        assertEquals(result.get(3).get(0).getFeedURI(), URI.create("https://example.com/feed/"));
        assertEquals(result.get(3).get(1).getFeedURI(), URI.create("https://example.com/comments/feed/"));

//        assertArrayEquals(result.get(0).toArray(), List.of().toArray());
//        assertArrayEquals(result.get(1).toArray(), List.of("https://feed.example").toArray());
//        assertArrayEquals(result.get(2).toArray(), List.of("https://example.com/feed").toArray());
//        assertArrayEquals(result.get(3).toArray(), List.of("https://example.com/comments/feed/", "https://example.com/feed/").toArray());
    }
}
