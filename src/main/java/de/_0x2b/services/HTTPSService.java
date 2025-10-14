package de._0x2b.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HTTPSService {
    private static final Logger logger = LoggerFactory.getLogger(HTTPSService.class);
    private static HTTPSService instance;

    HttpClient client;

    private HTTPSService() {
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.ofSeconds(30)).build();
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
    public HttpResponse<byte[]> fetchURI(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofSeconds(30)).build();
        HttpResponse<byte[]> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            logger.error("Could not find icon for feed {} \nReason: {}", uri, e.getMessage());
            return null;
        }

        if (response.statusCode() >= 400) {
            logger.error("cannot fetch {}", uri);
            return null;
        }
        return response;
    }
}
