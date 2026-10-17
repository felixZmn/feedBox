package de._0x2b.web;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Serve the SPA shell for bookmarkable client routes. Only matches feed, folder,
 * and article paths so {@code /api/}, {@code /q/}, and real static files are
 * untouched.
 */
@ApplicationScoped
public class SpaFallback {

    private static final String SPA_PATH =
            "^/(?:(?:feed|folder)/[0-9]+(?:/article/[0-9]+)?|article/[0-9]+|unread(?:/article/[0-9]+)?|saved(?:/article/[0-9]+)?)/?$";

    void register(@Observes Router router) {
        // Run after static resources so missing files fall through to the shell.
        router.getWithRegex(SPA_PATH)
                .last()
                .handler(ctx -> ctx.reroute("/index.html"));
    }
}
