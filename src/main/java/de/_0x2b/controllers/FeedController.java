package de._0x2b.controllers;

import de._0x2b.exceptions.DuplicateEntityException;
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
        var params = ctx.bodyAsClass(de._0x2b.models.Feed.class);
        var feed = feedService.query(params);

        if (feed.getName().equals("")) {
            ctx.status(404);
            return;
        }

        int id;
        try {
            id = feedService.create(feed);
        } catch (DuplicateEntityException e) {
            ctx.status(409).result("Feed with this URL already exists");
            return;
        }
        feed.setId(id);
        feedService.refresh(feed.getId());

        ctx.status(201).json(feed);
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
