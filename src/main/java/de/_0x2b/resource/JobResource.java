package de._0x2b.resource;

import java.util.UUID;

import de._0x2b.exception.NotFoundException;
import de._0x2b.job.BackgroundJobService;
import de._0x2b.job.JobSnapshot;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/jobs")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class JobResource {

    @Inject
    BackgroundJobService backgroundJobService;

    @GET
    @Path("/{id}")
    public JobSnapshot getJob(@PathParam("id") UUID id) {
        return backgroundJobService.find(id)
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));
    }
}
