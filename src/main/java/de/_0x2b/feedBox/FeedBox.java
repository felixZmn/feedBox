package de._0x2b.feedBox;

import de._0x2b.controllers.*;
import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.exceptions.NotFoundException;
import de._0x2b.repositories.ArticleRepository;
import de._0x2b.repositories.FeedRepository;
import de._0x2b.repositories.FolderRepository;
import de._0x2b.repositories.IconRepository;
import de._0x2b.services.*;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.javalin.apibuilder.ApiBuilder.*;

public class FeedBox {
    private static final Logger logger = LoggerFactory.getLogger(FeedBox.class);

    public static void main(String[] args) throws SQLException {
        var appConfig = AppConfig.loadFromEnv();

        System.setProperty("jdk.xml.totalEntitySizeLimit", "500000");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "500000");

        var databaseService = new DatabaseService(appConfig.dbHost(), appConfig.dbPort(), appConfig.dbName(),
                appConfig.dbUser(), appConfig.dbPass());

        databaseService.migrate();

        var articleRepository = new ArticleRepository(databaseService);
        var feedRepository = new FeedRepository(databaseService);
        var folderRepository = new FolderRepository(databaseService);
        var iconRepository = new IconRepository(databaseService);

        var articleService = new ArticleService(articleRepository);
        var folderService = new FolderService(folderRepository);
        var iconService = new IconService(iconRepository);
        var feedService = new FeedService(iconService, feedRepository, articleRepository);
        var opmlService = new OPMLService(folderService, feedService);

        var articleController = new ArticleController(articleService);
        var feedController = new FeedController(feedService);
        var folderController = new FolderController(folderService);
        var healthController = new HealthController();
        var opmlController = new OPMLController(opmlService);
        var iconController = new IconController(iconService);

        var app = Javalin.create(config -> {
            config.useVirtualThreads = true;
            config.staticFiles.add("/static", Location.CLASSPATH);
            config.jetty.defaultHost = "0.0.0.0";

            config.requestLogger.http((ctx, ms) -> {
                // GET http://localhost:8080/style.css HTTP/1.1" from [::1]:44872 - 200 in
                // 526.042 ms
                logger.info("{} {} {} from {}:{} - {} in {} ms", ctx.method(), ctx.url(), ctx.protocol(),
                        ctx.req().getRemoteAddr(), ctx.req().getRemotePort(), ctx.status().getCode(), ms);
            });
            config.router.apiBuilder(() -> {
                path("/healthz", () -> {
                    get(healthController::healthz);
                });
                path("/api", () -> {
                    path("/articles", () -> {
                        get(articleController::getAllArticles);
                    });
                    path("/feeds", () -> {
                        post(feedController::createFeed);
                        path("/{id}", () -> {
                            put(feedController::updateFeed);
                            delete(feedController::delete);
                        });
                        path("/refresh", () -> {
                            get(feedController::refresh);
                        });
                        path("/check", () -> {
                            get(feedController::check);
                        });
                    });
                    path("/folders", () -> {
                        get(folderController::get);
                        post(folderController::create);
                        path("/{id}", () -> {
                            put(folderController::update);
                            delete(folderController::delete);
                        });
                    });
                    path("/icons", () -> {
                        path("/{id}", () -> {
                            get(iconController::getIconByFeedId);
                        });
                    });
                    path("/opml", () -> {
                        post(opmlController::importOPML);
                        get(opmlController::exportOPML);
                    });
                });
            });
        });

        app.exception(DuplicateEntityException.class, (e, ctx) -> {
            ctx.status(409).result(e.getMessage());
        });
        app.exception(NotFoundException.class, (e, ctx) -> {
            ctx.status(404).result(e.getMessage());
        });
        app.exception(NumberFormatException.class, (e, ctx) -> {
            ctx.status(400).result("Invalid parameter format");
        });

        app.start(appConfig.appPort());

        // set up periodic refresh; for now only via env var configurable
        if (appConfig.refreshRate() > 0) {
            var scheduler = Executors.newScheduledThreadPool(1);

            // Wrap task to prevent scheduler death on Exception
            Runnable task = () -> {
                try {
                    logger.info("Starting scheduled feed refresh...");
                    feedService.refresh();
                    logger.info("Finished scheduled feed refresh.");
                } catch (Throwable t) {
                    logger.error("Periodic refresh failed", t);
                }
            };
            scheduler.scheduleAtFixedRate(task, 30, appConfig.refreshRate() * 60L, TimeUnit.SECONDS);
            logger.info("Feed refresh scheduled to start in 30 seconds.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            databaseService.close();
        }));
    }
}
