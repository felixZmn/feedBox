package de._0x2b.resource;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.model.Article;
import de._0x2b.service.ArticleService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/article")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArticleResource {
    private static final Logger logger = LoggerFactory.getLogger(ArticleResource.class);

    @Inject
    ArticleService articleService;

    @GET
    public Response getAllArticles(
            @QueryParam("folder") Integer folderId,
            @QueryParam("feed") Integer feedId,
            @QueryParam("pagination_id") Integer paginationId,
            @QueryParam("pagination_date") String paginationDate) {

        logger.debug("getAllArticles");

        // Set default values if parameters are null
        int folder = folderId != null ? folderId : -1;
        int feed = feedId != null ? feedId : -1;
        int pagId = paginationId != null ? paginationId : -1;
        String pagDate = paginationDate != null ? paginationDate : "";

        List<Article> result;

        if (folder > -1) {
            result = articleService.findByFolder(pagId, pagDate, folder);
        } else if (feed > -1) {
            result = articleService.findByFeed(pagId, pagDate, feed);
        } else {
            result = articleService.getAll(pagId, pagDate);
        }

        return Response.ok(result).build();
    }
}