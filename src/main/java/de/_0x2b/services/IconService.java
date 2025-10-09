package de._0x2b.services;

import de._0x2b.models.Feed;
import de._0x2b.models.Icon;
import de._0x2b.repositories.IconRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class IconService {
    private static final Logger logger = LoggerFactory.getLogger(IconService.class);
    private final IconRepository iconRepository;
    HttpClient client;

    public IconService(IconRepository iconRepository) {
        this.iconRepository = iconRepository;

        client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public List<Icon> findOneByFeed(int id){
        logger.debug("findOne");
        var icon = iconRepository.findOneByFeed(id);
        if (icon.isEmpty()){
            return getDefaultIcon();
        }
        return icon;
    }

    public void findIcon(Feed feed){
        var baseURL = feed.getFeedURI().getScheme() + "://"+ feed.getFeedURI().getHost();

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseURL))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        HttpResponse<byte[]> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            System.out.println("oh shit!");
            return;
        }

        if (response.statusCode() >= 400) {
            logger.warn("cannot fetch {}", baseURL);
            return;
        }
        var contentType = response.headers().firstValue("Content-Type").orElse("");
        var charset = getCharsetFromContentType(contentType);
        var iconUrl = parseHtml(new String(response.body(), charset));

        var url = "";
        if (iconUrl.getHost() != null){
            url = iconUrl.toString();
        } else {
            url = response.uri().getScheme() + "://"+ response.uri().getHost() + (iconUrl.getPath().startsWith("/") ? "" : "/")+ iconUrl.getPath();
        }

        var icondata = fetchFavicon(url);

        if (icondata.length > 0){
            var icon = new Icon(-1, feed.getId(), icondata, "", "", "");
            create(icon);
        }
    }

    private URI parseHtml(String content){
        if (content == null || content.isBlank()) {
            return URI.create("/favicon.ico");
        }

        try {
            Document doc = Jsoup.parse(content);
            // select link elements whose rel contains "icon" (case-insensitive)
            Element link = doc.selectFirst("link[rel~=icon]");
            if (link != null) {
                String href = link.attr("href");
                if (!href.isBlank()) {
                    return URI.create(href.trim());
                }
            }
        } catch (Exception e) {
            // do something
            System.out.println("oh no!");
        }

        return URI.create("/favicon.ico");
    }

    public byte[] fetchFavicon(String faviconUrl)  {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(faviconUrl))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                logger.error("Failed to fetch favicon: HTTP {}", response.statusCode());
                return new byte[0];
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error fetching favicon {}", e.getMessage());
        }
        return new byte[0];
    }

    public void create(Icon icon){
        logger.debug("insert");
        iconRepository.create(icon);
    }

    public List<Icon> getDefaultIcon()  {
        ArrayList<Icon> icons = new ArrayList<>();
        try(InputStream is = getClass().getResourceAsStream("/static/icons/rss.svg")){
            icons.add(new Icon(-1, -1, is.readAllBytes(), "image/svg+xml", "icon.svg", ""));
        } catch (IOException e){
            logger.error("Error reading default feed icon");
        }
        return icons;
    }

    /**
     * Extracts the charset from the Content-Type header.
     * If charset is not specified or unsupported, returns UTF-8 as default.
     *
     * @param contentType the Content-Type header string, e.g. "text/html; charset=UTF-8"
     * @return the Charset to be used for decoding
     */
    public static Charset getCharsetFromContentType(String contentType) {
        if (contentType == null) {
            return StandardCharsets.UTF_8; // default
        }

        // Split the header into parts separated by ';'
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim();
            // Check if part starts with "charset="
            if (part.toLowerCase().startsWith("charset=")) {
                String charsetName = part.substring("charset=".length()).trim();
                // Remove quotes if present
                if (charsetName.startsWith("\"") && charsetName.endsWith("\"") && charsetName.length() > 1) {
                    charsetName = charsetName.substring(1, charsetName.length() - 1);
                }
                try {
                    return Charset.forName(charsetName);
                } catch (Exception e) {
                    // Unsupported charset, fallback to default
                    return StandardCharsets.UTF_8;
                }
            }
        }

        // If no charset param found, return default charset
        return StandardCharsets.UTF_8;
    }
}
