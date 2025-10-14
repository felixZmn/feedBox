package de._0x2b.controllers;

import de._0x2b.services.IconService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconController {
    private static final Logger logger = LoggerFactory.getLogger(IconController.class);
    private final IconService iconService;

    public IconController(IconService iconService) {
        this.iconService = iconService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/icons/{id}", this::getIconByFeedId);
    }

    private void getIconByFeedId(Context ctx) {
        int feedId = Integer.parseInt(ctx.pathParam("id"));
        var foo = iconService.findOneByFeed(feedId);

        if (foo.isEmpty()) {
            ctx.status(404);
            return;
        }

        ctx.header("Content-Type", foo.getFirst().getMimeType());
        ctx.result(foo.getFirst().getImage());
        ctx.status(200);
    }
}
