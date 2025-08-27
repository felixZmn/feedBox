package de._0x2b.controllers;

import de._0x2b.services.FolderService;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class FolderController {
    private final FolderService folderService = new FolderService();

    public void registerRoutes(Javalin app) {
        app.get("/api/folders", this::getAllFolders);
    }

    private void getAllFolders(Context ctx) {
        ctx.json(folderService.getAll());
    }
}
