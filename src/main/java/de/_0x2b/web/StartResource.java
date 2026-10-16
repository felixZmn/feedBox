package de._0x2b.web;

import java.net.URI;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Authenticated bounce used by the Sign-in button. An unauthenticated document
 * navigation here triggers the OIDC challenge; after login Quarkus restores
 * and this handler sends the browser to the public SPA shell.
 */
@Path("/start")
@Authenticated
public class StartResource {

    @GET
    public Response start() {
        return Response.seeOther(URI.create("/")).build();
    }
}
