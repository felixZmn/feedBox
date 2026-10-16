package de._0x2b.resource;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import de._0x2b.resource.dto.AuthProviderDto;
import de._0x2b.resource.dto.AuthProvidersResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Public login-panel config. Returns the SSO provider button(s) so the shell
 * can customize labels without baking them into static HTML.
 */
@Path("/api/public/auth-providers")
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
public class AuthProvidersResource {

    private static final String DEFAULT_ID = "sso";
    private static final String DEFAULT_HREF = "/start";

    @ConfigProperty(name = "app.sso.button-label", defaultValue = "Sign in with SSO")
    String buttonLabel;

    @ConfigProperty(name = "app.sso.start-path", defaultValue = "/start")
    String startPath;

    @GET
    public AuthProvidersResponse list() {
        String label = sanitizeLabel(buttonLabel);
        String href = sanitizeHref(startPath);
        return new AuthProvidersResponse(
                List.of(new AuthProviderDto(DEFAULT_ID, label, href)));
    }

    static String sanitizeLabel(String raw) {
        if (raw == null) {
            return "Sign in with SSO";
        }
        String trimmed = raw.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty() || trimmed.length() > 80) {
            return "Sign in with SSO";
        }
        return trimmed;
    }

    static String sanitizeHref(String raw) {
        if (raw == null) {
            return DEFAULT_HREF;
        }
        String trimmed = raw.trim();
        // Same-origin relative path only — no scheme-relative //… open redirects.
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.contains("://")) {
            return DEFAULT_HREF;
        }
        return trimmed;
    }
}
