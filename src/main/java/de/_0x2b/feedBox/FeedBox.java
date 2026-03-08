package de._0x2b.feedBox;

import de._0x2b.config.Config;
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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.javalin.apibuilder.ApiBuilder.*;

public class FeedBox {
    private static final Logger logger = LoggerFactory.getLogger(FeedBox.class);

    public static void main(String[] args) throws Exception {
        System.setProperty("jdk.xml.totalEntitySizeLimit", "500000");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "500000");
        Config config = Config.load();

        var databaseService = new DatabaseService(config.db());

        databaseService.migrate();

        var articleRepository = new ArticleRepository(databaseService);
        var feedRepository = new FeedRepository(databaseService);
        var folderRepository = new FolderRepository(databaseService);
        var iconRepository = new IconRepository(databaseService);

        var httpService = new HTTPSService(config.app().userAgent(), config.app().timeout());
        var articleService = new ArticleService(articleRepository);
        var folderService = new FolderService(folderRepository);
        var iconService = new IconService(httpService, iconRepository);
        var feedService = new FeedService(httpService, iconService, feedRepository, articleRepository);
        var opmlService = new OPMLService(folderService, feedService);

        var articleController = new ArticleController(articleService);
        var feedController = new FeedController(feedService);
        var folderController = new FolderController(folderService);
        var healthController = new HealthController();
        var opmlController = new OPMLController(opmlService);
        var iconController = new IconController(iconService);

        AuthController authController;
        if (config.oidc().isPresent()) {
            logger.info("Enabling OIDC");
            var oidcService = new OidcService(config.oidc().get());
            authController = new AuthController(oidcService);
        } else {
            authController = null;
        }

        var app = Javalin.create(javalinConfig -> {
            javalinConfig.concurrency.useVirtualThreads = true;
            javalinConfig.staticFiles.add("/static", Location.CLASSPATH);
            javalinConfig.jetty.host = "0.0.0.0";
            javalinConfig.http.maxRequestSize = 10_000_000L; // 10 MB cap to guard against oversized uploads

            javalinConfig.requestLogger.http((ctx, ms) -> {
                // GET http://localhost:8080/style.css HTTP/1.1" from [::1]:44872 - 200 in
                // 526.042 ms
                logger.info("{} {} {} from {}:{} - {} in {} ms", ctx.method(), ctx.url(), ctx.protocol(),
                        ctx.req().getRemoteAddr(), ctx.req().getRemotePort(), ctx.status().getCode(), ms);
            });

            javalinConfig.routes.beforeMatched(ctx -> {

                // handle login
            });

            javalinConfig.routes.apiBuilder(() -> {
                if (config.oidc().isPresent()) {
                    path("/auth", () -> {
                        path("/login", () -> {
                            get(authController::login);
                        });
                        path("/callback", () -> {
                            get(authController::callback);
                        });
                        path("/refresh", () -> {
                            get(authController::refresh);
                        });
                        path("/logout", () -> {
                            get(authController::logout);
                        });
                    });
                }

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
                            post(feedController::refresh);
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
            javalinConfig.routes.exception(DuplicateEntityException.class, (e, ctx) -> {
                ctx.status(409).result(e.getMessage());
            });
            javalinConfig.routes.exception(NotFoundException.class, (e, ctx) -> {
                ctx.status(404).result(e.getMessage());
            });
            javalinConfig.routes.exception(NumberFormatException.class, (e, ctx) -> {
                ctx.status(400).result("Invalid parameter format");
            });
        });

        app.start(config.app().port());

        // set up periodic refresh; for now only via env var configurable
        if (config.app().refreshInterval() > 0) {
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
            scheduler.scheduleAtFixedRate(task, 30 * 30, config.app().refreshInterval() * 60L, TimeUnit.SECONDS);
            logger.info("Feed refresh scheduled to start in 30 seconds.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(databaseService::close));
    }
}
