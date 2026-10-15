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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
            logger.error("Error reading default feed icon: {}", e.getMessage());
            return new byte[0];
        }
    }

    public List<Icon> findOneByFeed(int id) {
        var icon = iconRepository.findByFeed(id);
        if (icon.isEmpty()) {
            return getDefaultIcon();
        }
        return icon;
    }

    public Icon findIcon(Feed feed) {
        for (URI pageUri : htmlPageCandidates(feed)) {
            var optional = outboundHttpClient.fetchIcon(pageUri);
            if (optional.isEmpty()) {
                continue;
            }
            var response = optional.get();
            URI iconUri = resolveIconHref(response.body(), response.finalUri());
            Icon icon = fetchFavicon(iconUri, feed.getId());
            if (icon.image() != null) {
                return icon;
            }
        }
        return null;
    }

    private List<URI> htmlPageCandidates(Feed feed) {
        var uris = new ArrayList<URI>(1);
        URI site = feed.getUrl();
        if (site != null && site.isAbsolute()) {
            uris.add(site);
        }
        return uris;
    }

    private URI resolveIconHref(byte[] htmlBytes, URI baseUri) {
        URI fallback = baseUri.resolve("/favicon.ico");
        if (htmlBytes == null || htmlBytes.length == 0) {
            return fallback;
        }
        try {
            Document doc = Jsoup.parse(new ByteArrayInputStream(htmlBytes), null, baseUri.toString());
            Element link = doc.selectFirst("link[rel~=icon]");
            if (link != null) {
                String abs = link.attr("abs:href");
                if (!abs.isBlank()) {
                    return URI.create(abs);
                }
                String href = link.attr("href").trim();
                if (!href.isBlank()) {
                    return baseUri.resolve(href);
                }
            }
        } catch (Exception e) {
            logger.warn("Error while searching html for icon url: {}", e.getMessage());
        }
        return fallback;
    }

    public Icon fetchFavicon(URI iconUri, int feedId) {
        if (iconUri == null || !iconUri.isAbsolute()) {
            logger.warn("Malformed favicon URL: {}", iconUri);
            return new Icon(-1, feedId, null, "", "");
        }

        var optional = outboundHttpClient.fetchIcon(iconUri);
        if (optional.isEmpty()) {
            logger.debug("Failed to fetch favicon from {}", iconUri);
            return new Icon(-1, feedId, null, "", "");
        }

        var response = optional.get();
        String mime = response.contentType() != null ? response.contentType() : "application/octet-stream";
        String mimeLower = mime.toLowerCase();
        int sc = mimeLower.indexOf(';');
        String mimeBase = (sc >= 0 ? mimeLower.substring(0, sc) : mimeLower).trim();
        if (!mimeBase.startsWith("image/")) {
            logger.warn("Refusing non-image favicon (content-type={}) from {}", mime, iconUri);
            return new Icon(-1, feedId, null, "", "");
        }
        return new Icon(-1, feedId, response.body(), mimeBase, "");
    }

    public void create(Icon icon) {
        iconRepository.create(icon);
    }

    public List<Icon> getDefaultIcon() {
        return List.of(new Icon(-1, -1, DEFAULT_ICON_BYTES.clone(), DEFAULT_ICON_MIME, DEFAULT_ICON_NAME));
    }
}
