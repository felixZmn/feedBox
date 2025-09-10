package de._0x2b.controllers;

import de._0x2b.services.OPMLService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

public class OPMLController {
    private static final Logger logger = LoggerFactory.getLogger(OPMLController.class);
    private final OPMLService opmlService;

    public OPMLController(OPMLService opmlService) {
        this.opmlService = opmlService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/opml", this::importOPML);
        app.get("api/opml", this::exportOPML);
    }

    private void importOPML(Context ctx) {
        logger.debug("importOPML");
        try {
            InputStream bodyStream = ctx.bodyInputStream();
            opmlService.importOPML(bodyStream);
        } catch (XMLStreamException e) {
            logger.error("Error while importing opml file", e);
            ctx.status(500);
        }
    }

    private void exportOPML(Context ctx) {
        logger.debug("exportOPML");
        // not implemented
    }
}
