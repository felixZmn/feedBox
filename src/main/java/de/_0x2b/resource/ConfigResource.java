package de._0x2b.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/config")
public class ConfigResource {

    @ConfigProperty(name = "sso.clientid")
    String clientId;

    @ConfigProperty(name = "sso.redirectUri")
    String redirectUri;

    @ConfigProperty(name = "sso.authServerUrl")
    String authServerUrl;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ConfigResponse getConfig() {
        return new ConfigResponse(clientId, redirectUri, authServerUrl);
    }

    public static class ConfigResponse {
        public String clientId;
        public String redirectUri;
        public String authServerUrl;

        public ConfigResponse(String clientId, String redirectUri, String authServerUrl) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.authServerUrl = authServerUrl;
        }
    }
}