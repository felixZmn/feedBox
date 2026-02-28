package de._0x2b.controllers;

import de._0x2b.models.Article;
import de._0x2b.services.ArticleService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ArticleController {
    private static final Logger logger = LoggerFactory.getLogger(ArticleController.class);
    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    public void getAllArticles(Context ctx) {
        logger.debug("getAllArticles");
        int folderId = ctx.queryParamAsClass("folder", Integer.class).getOrDefault(-1);
        int feedId = ctx.queryParamAsClass("feed", Integer.class).getOrDefault(-1);
        var paginationId = ctx.queryParam("pagination_id") != null ? Integer.parseInt(ctx.queryParam("pagination_id"))
                : -1;
        var paginationDate = ctx.queryParam("pagination_date") != null ? ctx.queryParam("pagination_date") : "";

        List<Article> result;
        if (folderId > -1) {
            result = articleService.findByFolder(paginationId, paginationDate, folderId);
        } else if (feedId > -1) {
            result = articleService.findByFeed(paginationId, paginationDate, feedId);
        } else {
            result = articleService.getAll(paginationId, paginationDate);
        }
        ctx.json(result);
    }
}
