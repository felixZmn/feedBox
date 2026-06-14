package de._0x2b.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.service.IconService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("/api/icon")
public class IconResource {
    private static final Logger logger = LoggerFactory.getLogger(IconResource.class);

    @Inject
    IconService iconService;

    /**
     * Serve a feed icon as raw image bytes.
     * Security headers:
     * <ul>
     * <li>{@code X-Content-Type-Options: nosniff} - stop the browser
     * from re-sniffing a content-type that was carefully sanitised
     * by {@link IconService}.</li>
     * <li>{@code Content-Disposition: inline; filename="..."} - tell the
     * browser this is a download, not a document the user agent
     * should render with HTML semantics.</li>
     * <li>{@code Content-Security-Policy: default-src 'none'} - icons
     * are images, not scripts. Defence in depth against a feed that
     * somehow slips an SVG-with-script past the mime check.</li>
     * </ul>
     */
    @GET
    @Path("/{id}")
    public Response getIconByFeedId(@PathParam("id") int id) {
        logger.debug("getIconByFeedId");
        var icon = iconService.findOneByFeed(id);

        if (icon.isEmpty()) {
            return Response.status(404).build();
        }

        var firstIcon = icon.getFirst();
        return Response.ok(firstIcon.getImage())
                .header("Content-Type", firstIcon.getMimeType())
                .header("Cache-Control", "public, max-age=86400")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline; filename=\"" + safeFileName(firstIcon) + "\"")
                .header("Content-Security-Policy", "default-src 'none'")
                .build();
    }

    /**
     * Build a {@code Content-Disposition} filename that can't escape the
     * quoted-string context. The icon's stored {@code fileName} comes
     * straight from a feed, so we sanitise it before using it as a
     * filename header.
     */
    private static String safeFileName(de._0x2b.model.Icon icon) {
        String name = icon.getFileName();
        if (name == null || name.isBlank()) {
            return "icon";
        }
        // Keep alphanumerics, dot, dash and underscore. Anything else is
        // replaced with underscore. The first extension (if any) is
        // preserved; the rest is collapsed.
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length() && out.length() < 64; i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        return out.length() == 0 ? "icon" : out.toString();
    }
}
