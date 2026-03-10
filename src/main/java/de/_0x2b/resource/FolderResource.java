package de._0x2b.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de._0x2b.model.Folder;
import de._0x2b.service.FolderService;

@Path("/api/folder")
public class FolderResource {
    private static final Logger logger = LoggerFactory.getLogger(FolderResource.class);

    @Inject
    FolderService folderService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFolders() {
        logger.debug("getFolders");
        return Response.ok(folderService.findAll()).build();
    }

    @POST
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createFolder(Folder folder) {
        logger.debug("createFolder");
        return Response.ok(folderService.create(folder)).build();
    }

    @PUT()
    @Path("/{id}")
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateFolder(@PathParam("id") int id, Folder folder) {
        logger.debug("updateFolder");
        folder.setId(id);
        return Response.ok(folderService.update(folder)).build();
    }

    @DELETE()
    @Path("/{id}")
    @Transactional
    public Response deleteFolder(@PathParam("id") int id) {
        logger.debug("deleteFolder");
        folderService.delete(id);
        return Response.ok().build();
    }
}
