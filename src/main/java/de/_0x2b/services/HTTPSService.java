package de._0x2b.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HTTPSService {
    private static final Logger logger = LoggerFactory.getLogger(HTTPSService.class);
    private static HTTPSService instance;
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";
    private static final int TIMEOUT_SECONDS = 10;

    HttpClient client;

    private HTTPSService() {
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build();
    }

    public static HTTPSService getInstance() {
        if (instance == null) {
            instance = new HTTPSService();
        }
        return instance;
    }

    /**
     * Helper to perform http requests. Takes care about errors and logging
     *
     * @param uri URI to fetch
     * @return Response object or null if request is not successful
     */
    public HttpResponse<byte[]> fetchUriAsBytes(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        HttpResponse<byte[]> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            logger.error("Could not find icon for feed {} \nReason: {}", uri, e.getMessage());
            return null;
        }

        if (response.statusCode() >= 400) {
            logger.error("cannot fetch {}; status code {}", uri, response.statusCode());
            return null;
        }
        return response;
    }

    public InputStream fetchUriAsStream(URI uri){
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        HttpResponse<InputStream> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (response.statusCode() == 200){
            return response.body();
        }
        return null;
    }
}
