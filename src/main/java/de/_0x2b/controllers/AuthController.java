package de._0x2b.controllers;

import de._0x2b.services.OidcService;
import io.javalin.http.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final OidcService oidcService;

    public AuthController(OidcService oidcService) {
        this.oidcService = oidcService;
    }

    public void login(Context ctx) {
        logger.debug("login");
        String authorizationUrl = oidcService.getAuthorizationRequestURI();
        ctx.redirect(authorizationUrl);
    }

    public void logout(Context ctx) {
        logger.debug("logout");
        // Invalidate session or tokens as needed
        ctx.status(200).result("Logged out");
    }

    public void callback(Context ctx) {
        logger.debug("callback");
        String code = ctx.queryParam("code");

        if (code == null) {
            ctx.status(400).result("Missing 'code' query parameter");
            return;
        }

        try {
            var tokens = oidcService.exchangeCodeForTokens(code);
            // Store tokens in session or database as needed
            StringBuilder redirectUrl = new StringBuilder("/auth-callback.html?");
            redirectUrl.append("access_token=").append(tokens.accessToken);
            redirectUrl.append("&id_token=").append(tokens.idToken);
            if (tokens.refreshToken != null) {
                redirectUrl.append("&refresh_token=").append(tokens.refreshToken);
            }
            ctx.redirect(redirectUrl.toString());
        } catch (Exception e) {
            logger.error("Error handling OIDC callback", e);
            ctx.status(500).result("Internal Server Error");
        }
    }

    public void refresh(Context ctx) {
        logger.debug("refresh");
        throw new UnsupportedOperationException("Token refresh not implemented yet");
    }
}
