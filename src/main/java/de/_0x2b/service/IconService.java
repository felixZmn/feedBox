package de._0x2b.service;

import de._0x2b.http.OutboundHttpClient;
import de._0x2b.model.Feed;
import de._0x2b.model.Icon;
import de._0x2b.repository.IconRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class IconService {
    private static final Logger logger = LoggerFactory.getLogger(IconService.class);
    private static final byte[] DEFAULT_ICON_BYTES = loadDefaultIconBytes();
    private static final String DEFAULT_ICON_MIME = "image/svg+xml";
    private static final String DEFAULT_ICON_NAME = "icon.svg";

    @Inject
    IconRepository iconRepository;

    @Inject
    OutboundHttpClient outboundHttpClient;

    private static byte[] loadDefaultIconBytes() {
        try (InputStream is = IconService.class.getResourceAsStream("/META-INF/resources/icons/rss.svg")) {
            if (is == null) {
                logger.error("Default feed icon resource missing");
                return new byte[0];
            }
            return is.readAllBytes();
        } catch (IOException e) {
            logger.error("Error reading default feed icon, Error: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Extracts the charset from the Content-Type header.
     * If charset is not specified or unsupported, returns UTF-8 as default.
     * 
     * @param contentType the Content-Type header string, e.g. "text/html;
     *                    charset=UTF-8"
     * @return the Charset to be used for decoding
     */
    public static Charset getCharsetFromContentType(String contentType) {
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }

        // Split the header into parts separated by ';'
        String[] parts = contentType.split(";");
        for (String part : parts) {
            part = part.trim().toLowerCase();
            // Check if part starts with "charset="
            if (!part.toLowerCase().startsWith("charset=")) {
                continue;
            }
            String charsetName = part.substring("charset=".length()).trim();
            // Remove quotes if present
            charsetName = charsetName.startsWith("\"") ? charsetName.substring(1) : charsetName;
            charsetName = charsetName.endsWith("\"") ? charsetName.substring(0, charsetName.length() - 1) : charsetName;
            try {
                return Charset.forName(charsetName);
            } catch (Exception e) {
                return StandardCharsets.UTF_8;
            }
        }
        return StandardCharsets.UTF_8;
    }

    /**
     * Find icon by feed id
     * 
     * @param id
     * @return
     */
    public List<Icon> findOneByFeed(int id) {
        logger.debug("findOne");
        var icon = iconRepository.findByFeed(id);
        if (icon.isEmpty()) {
            return getDefaultIcon();
        }
        return icon;
    }

    /**
     * Find icon for a given feed by parsing its HTML for icon links
     * 
     * @param feed
     * @return Icon object with the found icon data - or the default icon
     */
    public Icon findIcon(Feed feed) {
        var urls = constructHtmlUris(feed);

        for (URI uri : urls) {
            var optional = outboundHttpClient.fetchIcon(uri);
            if (optional.isEmpty()) {
                continue; // try next url
            }
            var response = optional.get();
            var charset = getCharsetFromContentType(response.contentType() != null ? response.contentType() : "");
            var iconUrl = parseHtml(new String(response.body(), charset));
            URI absoluteIcon = response.finalUri().resolve(iconUrl);
            var icon = fetchFavicon(new Icon(-1, feed.getId(), null, "", "", absoluteIcon.toString()));

            if (icon.getImage() != null) {
                return icon;
            }
        }
        return null;
    }

    /**
     * Helper method to construct a list of urls to check for a reference to a
     * favicon
     *
     * @param feed feed to use as a base
     * @return list of uris to use to search a favicon
     */
    private List<URI> constructHtmlUris(Feed feed) {
        var iconUris = new ArrayList<URI>();

        var scheme = feed.getUrl().getScheme();
        if (scheme == null) {
            scheme = "https";
        }

        var host = feed.getUrl().getHost();
        var path = feed.getUrl().getPath();

        iconUris.add(URI.create(scheme + "://" + host + path));
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
            Element iconEl = doc.selectFirst("icon");
            if (iconEl != null && iconEl.childNodeSize() > 0) {
                return URI.create(iconEl.childNode(0).toString());
            }
        } catch (Exception e) {
            logger.error("Error while searching html for icon url, error: {}", e.getMessage());
        }

        return URI.create("/favicon.ico");
    }

    /**
     * Method to fetch a favicon from a given url
     *
     * @param icon Icon object that contains the url - other fields are not relevant
     * @return Icon object that, in case of success is filled with icon data.
     *         If the response is not an image, the icon is returned with
     *         a null image byte[] so the caller can try the next URL or
     *         fall back to the default icon.
     */
    public Icon fetchFavicon(Icon icon) {
        URI iconUri;
        try {
            iconUri = URI.create(icon.getUrl());
        } catch (IllegalArgumentException e) {
            logger.warn("Malformed favicon URL: {}", icon.getUrl());
            return icon;
        }

        var optional = outboundHttpClient.fetchIcon(iconUri);
        if (optional.isEmpty()) {
            logger.warn("Failed to fetch favicon from {}", icon.getUrl());
            return icon;
        }

        var response = optional.get();
        String mime = response.contentType() != null ? response.contentType() : "application/octet-stream";
        // Security: a feed author could try to make us serve HTML or
        // JavaScript as the "icon". Reject anything that doesn't look
        // like an image MIME type. SVG is intentionally excluded here:
        // an SVG can contain <script> and is a script-capable format
        // when served as the document body. If you trust your feeds,
        // add "image/svg+xml" to the allow list.
        String mimeLower = mime.toLowerCase();
        if (!mimeLower.startsWith("image/") || mimeLower.startsWith("image/svg")) {
            logger.warn("Refusing non-image favicon (content-type={}) from {}", mime, icon.getUrl());
            icon.setImage(null);
            return icon;
        }
        icon.setImage(response.body());
        icon.setMimeType(mime);
        return icon;
    }

    public void create(Icon icon) {
        logger.debug("insert");
        iconRepository.create(icon);
    }

    public List<Icon> getDefaultIcon() {
        // Clone so callers cannot mutate the cached classpath bytes.
        return List.of(new Icon(-1, -1, DEFAULT_ICON_BYTES.clone(), DEFAULT_ICON_MIME, DEFAULT_ICON_NAME, ""));
    }
}
