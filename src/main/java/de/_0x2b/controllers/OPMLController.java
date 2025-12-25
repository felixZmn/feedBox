package de._0x2b.controllers;

import de._0x2b.services.OPMLService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
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

    public void importOPML(Context ctx) {
        logger.debug("importOPML");
        try {
            InputStream bodyStream = ctx.bodyInputStream();
            opmlService.importOPML(bodyStream);
        } catch (XMLStreamException | InterruptedException e) {
            logger.error("Error while importing opml file", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void exportOPML(Context ctx) {
        logger.debug("exportOPML");
        ctx.contentType("text/x-opml; charset=UTF-8");
        ctx.header("Content-Disposition", "attachment; filename=\"feed-export.opml\"");
        ctx.result(opmlService.exportOpml());
    }
}
