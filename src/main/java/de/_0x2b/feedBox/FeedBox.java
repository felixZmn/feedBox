package de._0x2b.feedBox;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.controllers.ArticleController;
import de._0x2b.controllers.FeedController;
import de._0x2b.controllers.FolderController;
import de._0x2b.controllers.HealthController;
import de._0x2b.controllers.OPMLController;
import de._0x2b.database.Database;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

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

        ArticleController articleController = new ArticleController();
        OPMLController opmlController = new OPMLController();
        FolderController folderController = new FolderController();
        FeedController feedController = new FeedController();
        HealthController healthController = new HealthController();

        articleController.registerRoutes(app);
        opmlController.registerRoutes(app);
        folderController.registerRoutes(app);
        feedController.registerRoutes(app);
        healthController.registerRoutes(app);

        app.start(Integer.parseInt(variables.get("appPort")));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Database.disconnect();
        }));
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
