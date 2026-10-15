package de._0x2b.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

/**
 * Successful outbound GET after SSRF checks, redirect following, and size limits.
 */
public record FetchResult(
        URI requestUri,
        URI finalUri,
        int statusCode,
        String contentType,
        byte[] body) {

    public InputStream bodyStream() {
        return new ByteArrayInputStream(body != null ? body : new byte[0]);
    }
}
