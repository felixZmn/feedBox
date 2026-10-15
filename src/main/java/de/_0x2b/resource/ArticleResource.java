package de._0x2b.resource;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.exception.NotFoundException;
import de._0x2b.model.Article;
import de._0x2b.resource.dto.PaginationCursor;
import de._0x2b.service.ArticleService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/article")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArticleResource {
    private static final Logger logger = LoggerFactory.getLogger(ArticleResource.class);
    private static final int MAX_SEARCH_LENGTH = 100;

    @Inject
    ArticleService articleService;

    @GET
    public Response getAllArticles(
            @QueryParam("folder") Integer folderId,
            @QueryParam("feed") Integer feedId,
            @QueryParam("pagination_id") Long paginationId,
            @QueryParam("pagination_date") String paginationDate,
            @QueryParam("q") String q) {

        logger.debug("getAllArticles");

        if (folderId != null && feedId != null) {
            throw new BadRequestException("Specify at most one of folder or feed");
        }

        PaginationCursor cursor = PaginationCursor.from(paginationId, paginationDate);
        String search = normalizeSearch(q);

        List<Article> result;
        if (folderId != null) {
            result = articleService.findByFolder(folderId, cursor, search);
        } else if (feedId != null) {
            result = articleService.findByFeed(feedId, cursor, search);
        } else {
            result = articleService.getAll(cursor, search);
        }

        return Response.ok(result).build();
    }

    @GET
    @Path("/{id}")
    public Response getArticle(@PathParam("id") long id) {
        logger.debug("getArticle {}", id);
        var article = articleService.findById(id);
        if (article == null) {
            throw new NotFoundException("Article not found: " + id);
        }
        return Response.ok(article).build();
    }

    @POST
    @Path("/read")
    public Response markAllRead() {
        logger.debug("markAllRead");
        articleService.markAllRead();
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/read")
    public Response markRead(@PathParam("id") long id) {
        logger.debug("markRead {}", id);
        articleService.markRead(id);
        return Response.noContent().build();
    }

    /** Null → empty; trim; cap length so a runaway client cannot send huge patterns. */
    static String normalizeSearch(String q) {
        if (q == null) {
            return "";
        }
        String trimmed = q.trim();
        if (trimmed.length() > MAX_SEARCH_LENGTH) {
            return trimmed.substring(0, MAX_SEARCH_LENGTH);
        }
        return trimmed;
    }
}
