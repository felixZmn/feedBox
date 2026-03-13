package de._0x2b.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HTTPSServiceTest {

    private HTTPSService sut;
    private HttpClient httpClient;

    private final String userAgent = "TestAgent/1.0";
    private final int timeoutSeconds = 3;

    @BeforeEach
    void setUp() {
        sut = new HTTPSService(userAgent, timeoutSeconds);

        // overwrite the internally built client with a mock
        httpClient = mock(HttpClient.class);
        sut.client = httpClient; // package-private field in HTTPSService
    }

    // -------------------------
    // fetchAsBytes
    // -------------------------

    @Test
    void fetchAsBytes_when2xx_returnsOptionalWithResponse_andSetsHeadersAndTimeout() throws Exception {
        URI uri = URI.create("https://example.com/data");

        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> response = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("ok".getBytes());

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        Optional<HttpResponse<byte[]>> result = sut.fetchAsBytes(uri);

        assertTrue(result.isPresent());
        assertSame(response, result.get());

        // verify request building: header + timeout + method
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest sent = requestCaptor.getValue();
        assertEquals(uri, sent.uri());
        assertEquals("GET", sent.method());
        assertEquals(Optional.of(userAgent), sent.headers().firstValue("User-Agent"));
        assertEquals(Optional.of(java.time.Duration.ofSeconds(timeoutSeconds)), sent.timeout());
    }

    @Test
    void fetchAsBytes_whenStatusIs4xxOr5xx_returnsEmpty() throws Exception {
        URI uri = URI.create("https://example.com/notfound");

        @SuppressWarnings("unchecked")
        HttpResponse<byte[]> response = (HttpResponse<byte[]>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(404);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        Optional<HttpResponse<byte[]>> result = sut.fetchAsBytes(uri);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchAsBytes_whenIOException_returnsEmpty() throws Exception {
        URI uri = URI.create("https://example.com/io");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("boom"));

        Optional<HttpResponse<byte[]>> result = sut.fetchAsBytes(uri);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchAsBytes_whenInterruptedException_returnsEmpty_andInterruptFlagIsSet() throws Exception {
        URI uri = URI.create("https://example.com/interrupt");
        // clear interrupt flag from any previous test run
        Thread.interrupted();

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("interrupted"));

        Optional<HttpResponse<byte[]>> result = sut.fetchAsBytes(uri);

        assertTrue(result.isEmpty());
        assertTrue(Thread.currentThread().isInterrupted(), "Interrupted flag should be re-set");
    }

    // -------------------------
    // fetchUriAsStream
    // -------------------------

    @Test
    void fetchUriAsStream_when2xx_returnsOptionalWithResponse() throws Exception {
        URI uri = URI.create("https://example.com/stream");

        InputStream body = new ByteArrayInputStream("data".getBytes());

        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> response = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        Optional<HttpResponse<InputStream>> result = sut.fetchUriAsStream(uri);

        assertTrue(result.isPresent());
        assertSame(response, result.get());

        // basic request verification (same as bytes variant)
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

        HttpRequest sent = requestCaptor.getValue();
        assertEquals(uri, sent.uri());
        assertEquals("GET", sent.method());
        assertEquals(Optional.of(userAgent), sent.headers().firstValue("User-Agent"));
        assertEquals(Optional.of(java.time.Duration.ofSeconds(timeoutSeconds)), sent.timeout());
    }

    @Test
    void fetchUriAsStream_whenStatusIs4xxOr5xx_returnsEmpty() throws Exception {
        URI uri = URI.create("https://example.com/fail");

        @SuppressWarnings("unchecked")
        HttpResponse<InputStream> response = (HttpResponse<InputStream>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        Optional<HttpResponse<InputStream>> result = sut.fetchUriAsStream(uri);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchUriAsStream_whenIOException_returnsEmpty() throws Exception {
        URI uri = URI.create("https://example.com/io2");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("boom"));

        Optional<HttpResponse<InputStream>> result = sut.fetchUriAsStream(uri);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchUriAsStream_whenInterruptedException_returnsEmpty_andInterruptFlagIsSet() throws Exception {
        URI uri = URI.create("https://example.com/interrupt2");
        Thread.interrupted(); // clear

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("interrupted"));

        Optional<HttpResponse<InputStream>> result = sut.fetchUriAsStream(uri);

        assertTrue(result.isEmpty());
        assertTrue(Thread.currentThread().isInterrupted(), "Interrupted flag should be re-set");
    }
}
