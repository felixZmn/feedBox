package de._0x2b.controllers;

import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.exceptions.NotFoundException;
import de._0x2b.services.FeedService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;

public class FeedController {
    private static final Logger logger = LoggerFactory.getLogger(FeedController.class);
    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/feeds", this::createFeed);
        app.put("/api/feeds/{id}", this::updateFeed);
        app.get("/api/feeds/refresh", this::refresh);
        app.delete("/api/feeds/{id}", this::delete);
    }

    private void createFeed(Context ctx) {
        logger.debug("createFeed");
        var params = ctx.bodyAsClass(de._0x2b.models.Feed.class);

        int id;
        try {
            id = feedService.create(params);
        } catch (DuplicateEntityException e) {
            ctx.status(409).result("Feed with this URL already exists");
            return;
        } catch (NotFoundException e){
            ctx.status(404).result("Feed with this URL not found");
            return;
        }
        feedService.refresh(id);
        ctx.status(201);
    }

    private void updateFeed(Context ctx) {
        logger.debug("updateFeed");
        var feed = ctx.bodyAsClass(de._0x2b.models.Feed.class);
        feed.setId(Integer.parseInt(ctx.pathParam("id")));

        int result;
        try {
            result = feedService.update(feed);
        } catch (DuplicateEntityException e) {
            ctx.status(409).result("Feed with this URL already exists");
            return;
        }
        ctx.status(201).json(result);
    }

    private void refresh(Context ctx) {
        logger.debug("refresh");
        feedService.refresh();
        ctx.status(HttpStatus.NO_CONTENT);
    }

    private void delete(Context ctx) {
        logger.debug("delete");
        int feedId = Integer.parseInt(ctx.pathParam("id"));
        var result = feedService.deleteFeed(feedId);
        if (result == -1) {
            ctx.status(500).result("Failed to delete feed");
            return;
        }
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
