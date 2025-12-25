package de._0x2b.controllers;

import de._0x2b.services.IconService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconController {
    private static final Logger logger = LoggerFactory.getLogger(IconController.class);
    private final IconService iconService;

    public IconController(IconService iconService) {
        this.iconService = iconService;
    }

    public void getIconByFeedId(Context ctx) {
        logger.debug("getIconByFeedId");
        int feedId = Integer.parseInt(ctx.pathParam("id"));
        var icon = iconService.findOneByFeed(feedId);

        if (icon.isEmpty()) {
            ctx.status(404);
            return;
        }

        ctx.header("Content-Type", icon.getFirst().getMimeType());
        ctx.header("Cache-Control", "max-age: public, max-age=86400");
        ctx.result(icon.getFirst().getImage());
        ctx.status(200);
    }
}
