package de._0x2b.controllers;

import be.ceau.opml.entity.Outline;
import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.models.Feed;
import de._0x2b.models.Folder;
import de._0x2b.services.FeedService;
import de._0x2b.services.FolderService;
import de._0x2b.services.OPMLService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.util.List;

public class OPMLController {
    private final OPMLService opmlService;

    public OPMLController(OPMLService opmlService) {
        this.opmlService = opmlService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/opml", this::importOPML);
        app.get("api/opml", this::exportOPML);
    }

    private void importOPML(Context ctx) {
        try {
            InputStream bodyStream = ctx.bodyInputStream();
            opmlService.importOPML(bodyStream);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    private void exportOPML(Context ctx) {
        // not implemented
    }
}
