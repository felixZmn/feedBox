package de._0x2b.resource;

import java.util.List;

import de._0x2b.exception.NotFoundException;
import de._0x2b.resource.dto.PaginationCursor;
import de._0x2b.resource.dto.SavedCount;
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
    private static final int MAX_SEARCH_LENGTH = 100;

    @Inject
    ArticleService articleService;

    @GET
    public Response getAllArticles(
            @QueryParam("folder") Integer folderId,
            @QueryParam("feed") Integer feedId,
            @QueryParam("pagination_id") Long paginationId,
            @QueryParam("pagination_date") String paginationDate,
            @QueryParam("q") String q,
            @QueryParam("view") String view) {

        ListView listView = resolveListView(folderId, feedId, view);
        PaginationCursor cursor = PaginationCursor.from(paginationId, paginationDate);
        String search = normalizeSearch(q);

        List<?> result;
        if (listView == ListView.UNREAD) {
            result = articleService.findUnread(cursor, search);
        } else if (listView == ListView.SAVED) {
            result = articleService.findSaved(cursor, search);
        } else if (folderId != null) {
            result = articleService.findByFolder(folderId, cursor, search);
        } else if (feedId != null) {
            result = articleService.findByFeed(feedId, cursor, search);
        } else {
            result = articleService.getAll(cursor, search);
        }

        return Response.ok(result).build();
    }

    @GET
    @Path("/saved/count")
    public SavedCount savedCount() {
        return new SavedCount(articleService.countSaved());
    }

    @GET
    @Path("/{id}")
    public Response getArticle(@PathParam("id") long id) {
        var article = articleService.findById(id);
        if (article == null) {
            throw new NotFoundException("Article not found: " + id);
        }
        return Response.ok(article).build();
    }

    @POST
    @Path("/read")
    public Response markAllRead() {
        articleService.markAllRead();
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/read")
    public Response markRead(@PathParam("id") long id) {
        articleService.markRead(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/saved")
    public Response save(@PathParam("id") long id) {
        articleService.save(id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/saved")
    public Response unsave(@PathParam("id") long id) {
        articleService.unsave(id);
        return Response.noContent().build();
    }

    enum ListView {
        ALL, UNREAD, SAVED
    }

    /**
     * @throws BadRequestException when folder and feed are both set, when {@code view}
     *         is combined with either, or when {@code view} is not unread or saved
     */
    static ListView resolveListView(Integer folderId, Integer feedId, String view) {
        if (folderId != null && feedId != null) {
            throw new BadRequestException("Specify at most one of folder or feed");
        }
        ListView parsed = parseView(view);
        if (parsed != ListView.ALL && (folderId != null || feedId != null)) {
            throw new BadRequestException("view cannot be combined with folder or feed");
        }
        return parsed;
    }

    static ListView parseView(String view) {
        if (view == null || view.isBlank()) {
            return ListView.ALL;
        }
        return switch (view) {
            case "unread" -> ListView.UNREAD;
            case "saved" -> ListView.SAVED;
            default -> throw new BadRequestException("view must be unread or saved");
        };
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
