package de._0x2b.resource;

import de._0x2b.service.OPMLService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

@Path("/api/opml")
public class OpmlResource {
    private static final Logger logger = LoggerFactory.getLogger(OpmlResource.class);

    @Inject
    OPMLService opmlService;

    @POST
    @Authenticated
    public Response importOpml(InputStream bodyStream) {
        logger.debug("importOPML");
        try {
            opmlService.importOPML(bodyStream);
            return Response.ok().build();
        } catch (XMLStreamException e) {
            logger.error("Error while importing opml file", e);
            return Response.serverError().build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error while importing opml file", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Authenticated
    public Response exportOpml() {
        logger.debug("exportOPML");
        String opmlContent = opmlService.exportOpml();
        return Response.ok(opmlContent)
                .type("text/x-opml; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"feed-export.opml\"")
                .build();
    }
}
