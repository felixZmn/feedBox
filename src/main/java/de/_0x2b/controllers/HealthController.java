package de._0x2b.controllers;

import io.javalin.Javalin;

public class HealthController {
    public HealthController() {

    }

    public void registerRoutes(Javalin app) {
        app.get("/healthz", this::healthz);
    }

    private void healthz(io.javalin.http.Context ctx) {
        ctx.status(200).result("OK");
    }
}
