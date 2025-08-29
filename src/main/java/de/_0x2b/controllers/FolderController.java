package de._0x2b.controllers;

import de._0x2b.models.Folder;
import de._0x2b.services.FolderService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class FolderController {
    private final FolderService folderService = new FolderService();

    public void registerRoutes(Javalin app) {
        app.get("/api/folders", this::getAllFolders);
        app.post("/api/folders/", this::createFolder);
        app.put("/api/folders/{id}", this::updateFolder);
        app.delete("/api/folders/{id}", this::deleteFolder);
    }

    private void getAllFolders(Context ctx) {
        ctx.json(folderService.getAll());
    }

    private void createFolder(Context ctx) {
        var result = folderService.save(ctx.bodyAsClass(Folder.class));
        if (result == -1) {
            ctx.status(500).result("Failed to create folder");
            return;
        }
        if (result == -2) {
            ctx.status(409).result("Folder with this name already exists");
            return;
        }
        ctx.status(201).json(result);
    }

    private void updateFolder(Context ctx) {
        var folder = ctx.bodyAsClass(Folder.class);
        folder.setId(Integer.parseInt(ctx.pathParam("id")));

        var result = folderService.update(folder);
        if (result == -1) {
            ctx.status(500).result("Failed to create folder");
            return;
        }
        if (result == -2) {
            ctx.status(409).result("Folder with this name already exists");
            return;
        }
        ctx.status(201).json(result);
    }

    private void deleteFolder(Context ctx) {
        int folderId = Integer.parseInt(ctx.pathParam("id"));
        var result = folderService.delete(folderId);
        if (result == -1) {
            ctx.status(500).result("Failed to delete folder");
            return;
        }
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
