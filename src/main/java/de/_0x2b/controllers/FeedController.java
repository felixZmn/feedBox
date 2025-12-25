package de._0x2b.controllers;

import de._0x2b.services.FeedService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedController {
    private static final Logger logger = LoggerFactory.getLogger(FeedController.class);
    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    public void createFeed(Context ctx) {
        logger.debug("createFeed");
        var params = ctx.bodyAsClass(de._0x2b.models.Feed.class);
        int id = feedService.create(params);
        feedService.refresh(id);
        ctx.status(HttpStatus.CREATED).json(id);
    }

    public void updateFeed(Context ctx) {
        logger.debug("updateFeed");
        var feed = ctx.bodyAsClass(de._0x2b.models.Feed.class);
        feed.setId(Integer.parseInt(ctx.pathParam("id")));
        int result = feedService.update(feed);
        ctx.status(HttpStatus.CREATED).json(result);
    }

    public void refresh(Context ctx) {
        logger.debug("refresh");
        feedService.refresh();
        ctx.status(HttpStatus.NO_CONTENT);
    }

    public void delete(Context ctx) {
        logger.debug("delete");
        int feedId = Integer.parseInt(ctx.pathParam("id"));
        var result = feedService.deleteFeed(feedId);
        if (result == -1) {
            ctx.status(500).result("Failed to delete feed");
            return;
        }
        ctx.status(HttpStatus.NO_CONTENT);
    }

    public void check(Context ctx) {
        String urlToCheck = ctx.queryParam("url");

        if (urlToCheck == null || urlToCheck.isBlank()) {
            ctx.status(400).result("Missing 'url' query parameter");
            return;
        }

        ctx.json(feedService.checkFeed(urlToCheck));
    }
}
