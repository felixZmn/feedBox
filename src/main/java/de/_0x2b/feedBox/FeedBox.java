package de._0x2b.feedBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.controllers.ArticleController;
import de._0x2b.controllers.FeedController;
import de._0x2b.controllers.FolderController;
import de._0x2b.controllers.OPMLController;
import de._0x2b.database.Database;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class FeedBox {
    private static final Logger logger = LoggerFactory.getLogger(FeedBox.class);

    public static void main(String[] args) {
        Database.connect();
        Database.migrate();

        var app = Javalin.create(config -> {
            config.useVirtualThreads = true;
            config.staticFiles.add("/static", Location.CLASSPATH);
            config.requestLogger.http((ctx, ms) -> {
                // GET http://localhost:8080/style.css HTTP/1.1" from [::1]:44872 - 200 4839B in
                // 526.042µs
                logger.info(ctx.contextPath() + "in " + ms);
            });

        });

        ArticleController articleController = new ArticleController();
        OPMLController opmlController = new OPMLController();
        FolderController folderController = new FolderController();
        FeedController feedController = new FeedController();

        articleController.registerRoutes(app);
        opmlController.registerRoutes(app);
        folderController.registerRoutes(app);
        feedController.registerRoutes(app);

        app.start(7070);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Database.disconnect();
        }));
    }
}
