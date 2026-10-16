package de._0x2b.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import de._0x2b.job.BackgroundJobService;
import de._0x2b.job.JobSnapshot;
import de._0x2b.service.OPMLService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Path("/api/opml")
@Authenticated
public class OpmlResource {
    /** 5 MiB — enough for large OPML exports without unbounded buffering. */
    private static final int MAX_OPML_BYTES = 5 * 1024 * 1024;

    @Inject
    OPMLService opmlService;
    @Inject
    BackgroundJobService backgroundJobService;

    /**
     * Accept an OPML body and import feeds asynchronously. Returns 202 with a
     * job snapshot; poll {@code GET /api/jobs/{id}} until terminal.
     */
    @POST
    public Response importOpml(InputStream bodyStream) {
        byte[] bytes;
        try {
            bytes = bodyStream.readNBytes(MAX_OPML_BYTES + 1);
        } catch (IOException e) {
            throw new WebApplicationException("Failed to read OPML body", e, 400);
        }
        if (bytes.length > MAX_OPML_BYTES) {
            throw new WebApplicationException("OPML payload too large (max 5 MiB)", 413);
        }

        final byte[] payload = bytes;
        JobSnapshot job = backgroundJobService.startImport(progress -> {
            try {
                opmlService.importOPML(new ByteArrayInputStream(payload), progress);
            } catch (XMLStreamException e) {
                throw new IllegalStateException("OPML parse failed", e);
            }
        });
        return Response.accepted(job).type("application/json").build();
    }

    @GET
    public Response exportOpml() {
        String opmlContent = opmlService.exportOpml();
        return Response.ok(opmlContent)
                .type("text/x-opml; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"feed-export.opml\"")
                .build();
    }
}
