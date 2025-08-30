package de._0x2b.controllers;

import java.io.InputStream;
import java.util.List;

import be.ceau.opml.OpmlParseException;
import be.ceau.opml.OpmlParser;
import be.ceau.opml.entity.Opml;
import be.ceau.opml.entity.Outline;
import de._0x2b.models.Feed;
import de._0x2b.models.Folder;
import de._0x2b.services.FeedService;
import de._0x2b.services.FolderService;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class OPMLController {

    FolderService folderService = new FolderService();
    FeedService feedService = new FeedService();

    public void registerRoutes(Javalin app) {
        app.post("/api/opml", this::importOPML);
        app.get("api/opml", this::exportOPML);
    }

    private void importOPML(Context ctx) {
        try {
            InputStream bodyStream = ctx.bodyInputStream();
            Opml opml = new OpmlParser().parse(bodyStream);
            var body = opml.getBody();
            parseOutline(body.getOutlines(), 0);

        } catch (OpmlParseException e) {
            e.printStackTrace();
        }
    }

    private void exportOPML(Context ctx) {
        // not implemented
    }

    private void parseOutline(List<Outline> outlines, int folder) {
        for (Outline outline : outlines) {
            if ("rss".equals(outline.getAttribute("type"))) {
                // rss feed
                var feed = new Feed(-1, folder, outline.getAttribute("text"), outline.getAttribute("htmlUrl"),
                        outline.getAttribute("xmlUrl"));
                feedService.saveOne(feed);
            } else {
                // remaining: folder
                var folderName = outline.getAttribute("text");
                var folderId = folderService.saveIgnoreDuplicates(
                        new Folder(-1, folderName, List.of(), "f-base"));
                parseOutline(outline.getSubElements(), folderId);
            }
        }
    }
}
