package de._0x2b.resource;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(JobResource.class);

    @Inject
    BackgroundJobService backgroundJobService;

    @GET
    @Path("/{id}")
    public JobSnapshot getJob(@PathParam("id") UUID id) {
        logger.debug("getJob {}", id);
        return backgroundJobService.find(id)
                .orElseThrow(() -> new NotFoundException("Job not found: " + id));
    }
}
