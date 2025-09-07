package de._0x2b.feedBox;

import de._0x2b.controllers.*;
import de._0x2b.database.Database;
import de._0x2b.repositories.ArticleRepository;
import de._0x2b.repositories.FeedRepository;
import de._0x2b.repositories.FolderRepository;
import de._0x2b.services.ArticleService;
import de._0x2b.services.FeedService;
import de._0x2b.services.FolderService;
import de._0x2b.services.OPMLService;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FeedBox {
    private static final Logger logger = LoggerFactory.getLogger(FeedBox.class);

    public static void main(String[] args) {
        var variables = getVariables();
        System.out.println("Using database at " + variables.get("dbHost") + ":" + variables.get("dbPort") + "/"
                + variables.get("dbName")); // ToDo: Logger

        Database.connect(
                variables.get("dbHost"),
                variables.get("dbPort"),
                variables.get("dbName"),
                variables.get("dbUsername"),
                variables.get("dbPassword"));
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

        var articleRepository = new ArticleRepository();
        var feedRepository = new FeedRepository();
        var folderRepository = new FolderRepository();

        var articleService = new ArticleService(articleRepository);
        var feedService = new FeedService(feedRepository, articleRepository);
        var folderService = new FolderService(folderRepository);
        var opmlService = new OPMLService(folderService, feedService);

        var articleController = new ArticleController(articleService);
        var feedController = new FeedController(feedService);
        var folderController = new FolderController(folderService);
        var healthController = new HealthController();
        var opmlController = new OPMLController(opmlService);

        articleController.registerRoutes(app);
        opmlController.registerRoutes(app);
        folderController.registerRoutes(app);
        feedController.registerRoutes(app);
        healthController.registerRoutes(app);

        app.start(Integer.parseInt(variables.get("appPort")));
        Runtime.getRuntime().addShutdownHook(new Thread(Database::disconnect));
    }

    private static Map<String, String> getVariables() {
        var dbUsername = System.getenv("PG_USER");
        var dbPassword = System.getenv("PG_PASSWORD");
        var dbHost = System.getenv("PG_HOST");
        var dbPort = System.getenv("PG_PORT");
        var dbName = System.getenv("PG_DB");
        var appPort = System.getenv("PORT");

        if (dbUsername == null)
            throw new IllegalStateException("PG_USER not set");
        if (dbPassword == null)
            throw new IllegalStateException("PG_PASSWORD not set");
        if (dbHost == null)
            throw new IllegalStateException("PG_HOST not set");
        if (dbPort == null)
            throw new IllegalStateException("PG_PORT not set");
        if (dbName == null)
            throw new IllegalStateException("PG_DB not set");
        if (appPort == null)
            appPort = "7070"; // default port

        Map<String, String> variables = new HashMap<>();
        variables.put("dbUsername", dbUsername);
        variables.put("dbPassword", dbPassword);
        variables.put("dbHost", dbHost);
        variables.put("dbPort", dbPort);
        variables.put("dbName", dbName);
        variables.put("appPort", appPort);

        return variables;
    }
}
