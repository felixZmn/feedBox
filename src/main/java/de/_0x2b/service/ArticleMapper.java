package de._0x2b.service;

import com.apptasticsoftware.rssreader.module.mediarss.MediaRssItem;
import de._0x2b.model.Article;
import de._0x2b.model.Feed;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ApplicationScoped
public class ArticleMapper {
    public Article toArticle(Feed feed, MediaRssItem item) {
        var title = item.getTitle().orElse("");
        var description = item.getDescription().orElse("");
        var content = item.getContent().orElse("");
        // Blank links become null so UNIQUE (feed_id, link) does not collapse
        // every link-less item onto a single "" row.
        String link = item.getLink().map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
        Instant published = item.getPubDateAsZonedDateTime()
                .map(zdt -> zdt.toInstant())
                .orElse(null);
        var author = item.getAuthor().orElse("");
        String guid = resolveGuid(item, link, title, published);

        var imageUrl = "";
        if (item.getMediaThumbnail().isPresent()) {
            imageUrl = item.getMediaThumbnail().get().getUrl();
        }
        if (imageUrl.isEmpty() && item.getEnclosure().isPresent()
                && item.getEnclosure().get().getType().startsWith("image/")) {
            imageUrl = item.getEnclosure().get().getUrl();
        }

        var categories = formatCategories(item.getCategories());

        return new Article(
                -1, feed.getId(), feed.getName(), title, description, content, link,
                guid, published, author, imageUrl, categories);
    }

    /**
     * Per-feed identity for {@code ON CONFLICT (feed_id, guid)} and
     * {@code UNIQUE (feed_id, link)}.
     * <p>
     * Prefer the permalink when present so identity matches the link unique
     * index. Many feeds use a WordPress-style {@code ?p=} (or UUID) guid that
     * differs from the canonical URL; using that guid for upsert while also
     * enforcing unique links causes refresh to fail on every existing article
     * (V5 backfill set {@code guid = link}). Fall back to the feed guid, then
     * a hash of title + published for link-less items.
     */
    static String resolveGuid(MediaRssItem item, String link, String title, Instant published) {
        if (link != null && !link.isBlank()) {
            return link;
        }
        return item.getGuid()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseGet(() -> "fb:" + sha256Hex(
                        (title != null ? title : "") + "|" + (published != null ? published : "")));
    }

    /** JSON array of category strings (no List#toString brackets). */
    static String formatCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return "[]";
        }
        return categories.stream()
                .map(ArticleMapper::jsonString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String jsonString(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required on every JVM; fall back so insert never NPE.
            return Integer.toHexString(input.toLowerCase(Locale.ROOT).hashCode());
        }
    }
}
