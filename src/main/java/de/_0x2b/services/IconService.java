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
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class IconService {
    private static final Logger logger = LoggerFactory.getLogger(IconService.class);
    private final IconRepository iconRepository;

    public IconService(IconRepository iconRepository) {
        this.iconRepository = iconRepository;
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
            part = part.trim().toLowerCase();
            // Check if part starts with "charset="
            if (!part.toLowerCase().startsWith("charset=")){
                continue;
            }
            String charsetName = part.substring("charset=".length()).trim();
            // Remove quotes if present
            charsetName = charsetName.startsWith("\"") ? charsetName.substring(1) : charsetName;
            charsetName = charsetName.endsWith("\"") ? charsetName.substring(0, charsetName.length() - 1) : charsetName;
            try {
                return Charset.forName(charsetName);
            } catch (Exception e) {
                // Unsupported charset, fallback to default
                return StandardCharsets.UTF_8;
            }
        }

        // If no charset param found, return default charset
        return StandardCharsets.UTF_8;
    }

    public List<Icon> findOneByFeed(int id) {
        logger.debug("findOne");
        var icon = iconRepository.findByFeed(id);
        if (icon.isEmpty()) {
            return getDefaultIcon();
        }
        return icon;
    }

    public void findIcon(Feed feed) {
        var urls = constructHtmlUris(feed);

        for (URI uri : urls) {
            var response = HTTPSService.getInstance().fetchURI(uri);
            if (response == null) {
                continue; // try next url
            }
            var charset = getCharsetFromContentType(response.headers().firstValue("Content-Type").orElse(""));
            var iconUrl = parseHtml(new String(response.body(), charset));

            var url = "";
            if (iconUrl.getHost() != null) {
                url = iconUrl.toString();
            } else {
                url = response.uri().getScheme() + "://" + response.uri().getHost() + (iconUrl.getPath().startsWith("/") ? "" : "/") + iconUrl.getPath();
            }
            var icon = fetchFavicon(new Icon(-1, feed.getId(), null, "", "", url));

            if (icon.getImage() != null) {
                create(icon);
                return; // icon stored; break
            }
        }
    }

    /**
     * Helper method to construct a list of urls to check for a reference to a favicon
     *
     * @param feed feed to use as a base
     * @return list of uris to use to search a favicon
     */
    private List<URI> constructHtmlUris(Feed feed) {
        var iconUris = new ArrayList<URI>();

        var scheme = feed.getURI().getScheme();
        if (scheme == null) {
            scheme = "https";
        }

        var host = feed.getURI().getHost();
        var path = feed.getURI().getPath();

        iconUris.add(URI.create(scheme + "://" + host + path ));
        return iconUris;
    }

    /**
     * Helper method to filter favicon links from a given html page
     *
     * @param content html to search
     * @return first found favicon url
     */
    private URI parseHtml(String content) {
        if (content == null || content.isBlank()) {
            return URI.create("/favicon.ico");
        }

        try {
            Document doc = Jsoup.parse(content);
            Element link = doc.selectFirst("link[rel~=icon]");
            if (link != null) {
                String href = link.attr("href");
                if (!href.isBlank()) {
                    return URI.create(href.trim());
                }
            }

            // try <icon></icon>
            return URI.create(doc.selectFirst("icon").childNode(0).toString());
        } catch (Exception e) {
            // do something
            logger.error("Error while searching html for icon url, error: {}", e.getMessage());
        }

        return URI.create("/favicon.ico");
    }

    /**
     * Method to fetch a favicon from a given url
     *
     * @param icon Icon object that contains the url - other fields are not relevant
     * @return Icon object that, in case of success is filled with icon data
     */
    public Icon fetchFavicon(Icon icon) {
        HttpResponse<byte[]> response = HTTPSService.getInstance().fetchURI(URI.create(icon.getUrl()));

        if (response == null) {
            logger.error("Failed to fetch favicon");
            return icon;
        }

        icon.setImage(response.body());
        icon.setMimeType(response.headers().map().get("content-type").getFirst());
        return icon;
    }

    public void create(Icon icon) {
        logger.debug("insert");
        iconRepository.create(icon);
    }

    public List<Icon> getDefaultIcon() {
        ArrayList<Icon> icons = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/static/icons/rss.svg")) {
            icons.add(new Icon(-1, -1, is.readAllBytes(), "image/svg+xml", "icon.svg", ""));
        } catch (IOException e) {
            logger.error("Error reading default feed icon, Error: {}", e.getMessage());
        }
        return icons;
    }
}
