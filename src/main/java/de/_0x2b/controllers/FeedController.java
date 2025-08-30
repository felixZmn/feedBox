package de._0x2b.controllers;

import de._0x2b.services.FeedService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class FeedController {
    private final FeedService feedService = new FeedService();

    public void registerRoutes(Javalin app) {
        app.post("/api/feeds", this::createFeed);
        app.put("/api/feeds/{id}", this::updateFeed);
        app.get("/api/feeds/refresh", this::refresh);
        app.delete("/api/feeds/{id}", this::delete);
    }

    private void createFeed(Context ctx) {
        var result = feedService.create(ctx.bodyAsClass(de._0x2b.models.Feed.class));
        if (result == -1) {
            ctx.status(500).result("Failed to create feed");
            return;
        }
        if (result == -2) {
            ctx.status(409).result("Feed with this URL already exists");
            return;
        }
        ctx.status(201).json(result);
    }

    private void updateFeed(Context ctx) {
        var feed = ctx.bodyAsClass(de._0x2b.models.Feed.class);
        feed.setId(Integer.parseInt(ctx.pathParam("id")));

        var result = feedService.update(feed);
        if (result == -1) {
            ctx.status(500).result("Failed to update feed");
            return;
        }
        if (result == -2) {
            ctx.status(409).result("Feed with this URL already exists");
            return;
        }
        ctx.status(201).json(result);
    }

    private void refresh(Context ctx) {
        feedService.refresh();
        ctx.status(HttpStatus.NO_CONTENT);
    }

    private void delete(Context ctx) {
        int feedId = Integer.parseInt(ctx.pathParam("id"));
        var result = feedService.deleteFeed(feedId);
        if (result == -1) {
            ctx.status(500).result("Failed to delete feed");
            return;
        }
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
