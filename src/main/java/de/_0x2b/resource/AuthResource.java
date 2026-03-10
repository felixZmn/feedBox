package de._0x2b.resource;

import java.net.URI;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/auth")
public class AuthResource {

    @GET
    @Path("/login")
    @Authenticated
    public Response login() {
        return Response.seeOther(URI.create("/")).build();
    }

    @GET
    @Path("/logout")
    public Response logout() {
        return Response.seeOther(URI.create("/")).build();
    }
}