package de._0x2b.controllers;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthController {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    public HealthController() {
    }

    public void registerRoutes(Javalin app) {
        app.get("/healthz", this::healthz);
    }

    private void healthz(io.javalin.http.Context ctx) {
        logger.debug("healthz");
        ctx.status(200).result("OK");
    }
}
