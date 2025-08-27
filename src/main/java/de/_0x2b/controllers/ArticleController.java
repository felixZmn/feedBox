package de._0x2b.controllers;

import java.util.List;

import de._0x2b.services.ArticleService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import de._0x2b.models.Article;

public class ArticleController {
    private final ArticleService articleService = new ArticleService();

    public void registerRoutes(Javalin app) {
        app.get("/api/articles", this::getAllArticles);
    }

    private void getAllArticles(Context ctx) {
        var folderId = ctx.queryParam("folder") != null ? Integer.parseInt(ctx.queryParam("folder")) : -1;
        var feedId = ctx.queryParam("feed") != null ? Integer.parseInt(ctx.queryParam("feed")) : -1;
        var paginationId = ctx.queryParam("pagination_id") != null ? Integer.parseInt(ctx.queryParam("pagination_id"))
                : -1;
        var paginationDate = ctx.queryParam("pagination_date") != null ? ctx.queryParam("pagination_date") : "";

        List<Article> result;
        if (folderId != -1) {
            result = articleService.getByFolder(paginationId, paginationDate, folderId);
        } else if (feedId != -1) {
            System.out.println("FeedId: " + feedId);
            result = articleService.getByFeed(paginationId, paginationDate, feedId);
        } else {
            result = articleService.getAll(paginationId, paginationDate);
        }
        ctx.json(result);
    }
}
