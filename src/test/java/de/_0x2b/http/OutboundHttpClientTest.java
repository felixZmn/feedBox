package de._0x2b.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboundHttpClientTest {

    private OutboundHttpClient sut;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        sut = new OutboundHttpClient("TestAgent/1.0", 3, 5, 1024, 512, httpClient);
    }

    @Test
    void fetch_when2xx_returnsBodyAndSetsUserAgent() throws Exception {
        URI uri = URI.create("https://example.com/data");
        stubResponse(200, "ok".getBytes(), "text/plain", null);

        Optional<FetchResult> result = sut.fetch(uri, 1024);

        assertTrue(result.isPresent());
        assertEquals(uri, result.get().requestUri());
        assertEquals(uri, result.get().finalUri());
        assertEquals(200, result.get().statusCode());
        assertArrayEquals("ok".getBytes(), result.get().body());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals(Optional.of("TestAgent/1.0"), captor.getValue().headers().firstValue("User-Agent"));
    }

    @Test
    void fetch_when4xx_returnsEmpty() throws Exception {
        stubResponse(404, new byte[0], null, null);

        assertTrue(sut.fetch(URI.create("https://example.com/missing"), 1024).isEmpty());
    }

    @Test
    void fetch_whenIoException_returnsEmpty() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("boom"));

        assertTrue(sut.fetch(URI.create("https://example.com/io"), 1024).isEmpty());
    }

    @Test
    void fetch_whenInterrupted_returnsEmptyAndRestoresFlag() throws Exception {
        Thread.interrupted();
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("interrupted"));

        assertTrue(sut.fetch(URI.create("https://example.com/interrupt"), 1024).isEmpty());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void fetch_whenBodyExceedsLimit_returnsEmpty() throws Exception {
        stubResponse(200, new byte[2048], "application/octet-stream", null);

        assertTrue(sut.fetch(URI.create("https://example.com/big"), 1024).isEmpty());
    }

    @Test
    void fetch_whenUnsafeUrl_returnsEmptyWithoutCallingClient() throws Exception {
        assertTrue(sut.fetch(URI.create("http://127.0.0.1/secret"), 1024).isEmpty());
        verifyNoInteractions(httpClient);
    }

    @Test
    void fetch_followsRedirectAndValidatesNextHop() throws Exception {
        URI start = URI.create("https://example.com/a");
        URI next = URI.create("https://example.com/b");

        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> redirect = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(redirect.statusCode()).thenReturn(302);
        when(redirect.headers()).thenReturn(HttpHeaders.of(
                Map.of("location", List.of(next.toString())), (a, b) -> true));
        when(redirect.body()).thenReturn(new ByteArrayInputStream(new byte[0]));

        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> ok = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(ok.statusCode()).thenReturn(200);
        when(ok.headers()).thenReturn(HttpHeaders.of(
                Map.of("content-type", List.of("text/plain")), (a, b) -> true));
        when(ok.body()).thenReturn(new ByteArrayInputStream("done".getBytes()));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(redirect, ok);

        Optional<FetchResult> result = sut.fetch(start, 1024);

        assertTrue(result.isPresent());
        assertEquals(next, result.get().finalUri());
        assertEquals("done", new String(result.get().body()));
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void fetch_refusesHttpsToHttpDowngrade() throws Exception {
        URI start = URI.create("https://example.com/a");

        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> redirect = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(redirect.statusCode()).thenReturn(302);
        when(redirect.headers()).thenReturn(HttpHeaders.of(
                Map.of("location", List.of("http://example.com/b")), (a, b) -> true));
        when(redirect.body()).thenReturn(new ByteArrayInputStream(new byte[0]));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(redirect);

        assertTrue(sut.fetch(start, 1024).isEmpty());
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    private void stubResponse(int status, byte[] body, String contentType, String location)
            throws Exception {
        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> response = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(new ByteArrayInputStream(body));
        Map<String, List<String>> headers = new java.util.HashMap<>();
        if (contentType != null) {
            headers.put("content-type", List.of(contentType));
        }
        if (location != null) {
            headers.put("location", List.of(location));
        }
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (a, b) -> true));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
    }
}
