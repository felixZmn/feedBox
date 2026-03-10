package de._0x2b.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.service.IconService;
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

    @GET
    @Path("/{id}")
    public Response getIconByFeedId(@PathParam("id") int id) {
        logger.debug("getIconByFeedId");
        var icon = iconService.findOneByFeed(id);

        if (icon.isEmpty()) {
            return Response.status(404).build();
        }

        var firstIcon = icon.getFirst();
        return Response.ok(icon.getFirst().getImage()).header("Content-Type", firstIcon.getMimeType())
                .header("Cache-Control", "public, max-age=86400").build();
    }
}
