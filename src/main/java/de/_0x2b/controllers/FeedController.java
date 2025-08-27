package de._0x2b.controllers;

import de._0x2b.services.FeedService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class FeedController {
    private final FeedService feedService = new FeedService();

    public void registerRoutes(Javalin app) {
        app.post("/api/feeds", this::saveFeed);
        app.get("/api/feeds/refresh", this::refreshFeeds);
    }

    private void saveFeed(Context ctx) {
        // feedService.saveOne()
    }

    private void refreshFeeds(Context ctx) {
        feedService.refresh();
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
