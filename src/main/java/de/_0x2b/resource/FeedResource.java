package de._0x2b.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.model.Feed;
import de._0x2b.service.FeedService;
import io.quarkus.security.Authenticated;

@Path("/api/feed")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeedResource {
    private static final Logger logger = LoggerFactory.getLogger(FeedResource.class);

    @Inject
    FeedService feedService;

    @POST
    @Authenticated
    public Response createFeed(Feed feed) {
        logger.debug("createFeed");
        return Response.ok(feedService.create(feed)).build();
    }

    @PUT
    @Path("/{id}")
    @Authenticated
    public Response updateFeed(@PathParam("id") int id, Feed feed) {
        logger.debug("updateFeed");
        feed.setId(id);
        return Response.ok(feedService.update(feed)).build();
    }

    @DELETE
    @Path("/{id}")
    @Authenticated
    public Response deleteFeed(@PathParam("id") int id) {
        logger.debug("deleteFeed");
        feedService.delete(id);
        return Response.ok().build();
    }

    @POST
    @Path("/refresh")
    @Authenticated
    public Response refreshFeeds() {
        logger.debug("refreshFeeds");
        feedService.refresh();
        return Response.ok().build();
    }

    @GET
    @Path("/check")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkFeedUrl(@QueryParam("url") String url) {
        logger.debug("checkFeedUrl: {}", url);
        var result = feedService.checkFeedUrl(url);
        return Response.ok(result).build();
    }
}
