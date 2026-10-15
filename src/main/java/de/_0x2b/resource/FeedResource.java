package de._0x2b.resource;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.model.Feed;
import de._0x2b.resource.dto.CreateFeedRequest;
import de._0x2b.resource.dto.UpdateFeedRequest;
import de._0x2b.service.FeedService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/feed")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FeedResource {
    private static final Logger logger = LoggerFactory.getLogger(FeedResource.class);

    @Inject
    FeedService feedService;

    @GET
    public List<Feed> getFeeds() {
        logger.debug("getFeeds");
        return feedService.findAll();
    }

    @POST
    public Response createFeed(@Valid CreateFeedRequest request) {
        logger.debug("createFeed");
        Feed feed = new Feed(-1, request.folderId, "", null, request.feedUrl);
        int id = feedService.create(feed);
        return Response.status(Response.Status.CREATED)
                .entity(id)
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response updateFeed(@PathParam("id") int id, @Valid UpdateFeedRequest request) {
        logger.debug("updateFeed");
        Feed existing = feedService.findById(id);
        existing.setFolderId(request.folderId);
        existing.setFeedUrl(request.feedUrl);
        feedService.update(existing);
        return Response.ok(id).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteFeed(@PathParam("id") int id) {
        logger.debug("deleteFeed");
        feedService.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/read")
    public Response markRead(@PathParam("id") int id) {
        logger.debug("markRead feed {}", id);
        feedService.markRead(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/refresh")
    public Response refreshFeeds() {
        logger.debug("refreshFeeds");
        feedService.refresh();
        return Response.noContent().build();
    }

    @GET
    @Path("/check")
    public Response checkFeedUrl(@QueryParam("url") String url) {
        logger.debug("checkFeedUrl: {}", url);
        if (url == null || url.isBlank()) {
            throw new BadRequestException("url query parameter is required");
        }
        var result = feedService.checkFeedUrl(url);
        return Response.ok(result).build();
    }
}
