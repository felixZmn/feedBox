package de._0x2b.controllers;

import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.models.Folder;
import de._0x2b.services.FolderService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderController {
    private static final Logger logger = LoggerFactory.getLogger(FolderController.class);
    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/folders", this::get);
        app.post("/api/folders/", this::create);
        app.put("/api/folders/{id}", this::update);
        app.delete("/api/folders/{id}", this::delete);
    }

    private void get(Context ctx) {
        logger.debug("get");
        var foo = folderService.findAll();
        ctx.json(foo);
    }

    private void create(Context ctx) {
        logger.debug("create");
        int result;
        try {
            result = folderService.create(ctx.bodyAsClass(Folder.class));
        } catch (DuplicateEntityException e) {
            ctx.status(409).result("Folder already exists");
            return;
        }

        ctx.status(201).json(result);
    }

    private void update(Context ctx) {
        logger.debug("update");
        var folder = ctx.bodyAsClass(Folder.class);
        folder.setId(Integer.parseInt(ctx.pathParam("id")));

        int result;
        try {
            result = folderService.update(folder);
        } catch (DuplicateEntityException e) {
            ctx.status(409).result("Folder with this name already exists");
            return;
        }
        ctx.status(201).json(result);
    }

    private void delete(Context ctx) {
        logger.debug("delete");
        int folderId = Integer.parseInt(ctx.pathParam("id"));
        var result = folderService.delete(folderId);
        if (result == -1) {
            ctx.status(500).result("Failed to delete folder");
            return;
        }
        ctx.status(HttpStatus.NO_CONTENT);
    }
}
