package de._0x2b.resource;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.model.Folder;
import de._0x2b.model.FolderColor;
import de._0x2b.resource.dto.CreateFolderRequest;
import de._0x2b.resource.dto.UpdateFolderRequest;
import de._0x2b.service.FolderService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/folder")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FolderResource {
    private static final Logger logger = LoggerFactory.getLogger(FolderResource.class);

    @Inject
    FolderService folderService;

    @GET
    public List<Folder> getFolders() {
        return folderService.findAll();
    }

    @POST
    public Response createFolder(@Valid CreateFolderRequest request) {
        FolderColor color = request.color != null ? request.color : FolderColor.F_BASE;
        Folder folder = new Folder(-1, request.name.trim(), null, color.cssClass());
        int id = folderService.create(folder);
        return Response.status(Response.Status.CREATED)
                .entity(id)
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response updateFolder(@PathParam("id") int id, @Valid UpdateFolderRequest request) {
        Folder folder = new Folder(id, request.name.trim(), null, request.color.cssClass());
        folderService.update(folder);
        return Response.ok(id).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteFolder(@PathParam("id") int id) {
        folderService.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/read")
    public Response markRead(@PathParam("id") int id) {
        logger.debug("markRead folder {}", id);
        folderService.markRead(id);
        return Response.noContent().build();
    }
}
