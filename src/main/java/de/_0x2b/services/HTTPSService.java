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
import java.util.Optional;

public class HTTPSService {
    private static final Logger logger = LoggerFactory.getLogger(HTTPSService.class);
    private static HTTPSService instance;
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36";
    private static final int TIMEOUT_SECONDS = 15;

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
    public Optional<HttpResponse<byte[]>> fetchAsBytes(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                logger.error("Cannot fetch {}; status code {}", uri, response.statusCode());
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (IOException | InterruptedException e) {
            logger.error("Could not fetch URI: {}", uri, e);
            return Optional.empty();
        }
    }

    /**
     * Helper to perform http requests. Takes care about errors and logging
     *
     * @param uri URI to fetch
     * @return Response object or null if request is not successful
     */
    public Optional<HttpResponse<InputStream>> fetchUriAsStream(URI uri){
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                logger.error("Cannot fetch {}; status code {}", uri, response.statusCode());
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (IOException | InterruptedException e) {
            logger.error("Could not fetch URI: {}", uri, e);
            return Optional.empty();
        }
    }
}
