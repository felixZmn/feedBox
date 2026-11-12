package de._0x2b.web;

import java.util.regex.Pattern;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Serve the SPA shell for document navigations that are not a real static file.
 * Known article and list paths stay authenticated in {@code application.properties}.
 * {@code /api/}, {@code /q/}, {@code /start}, {@code /logout}, and {@code /oidc}
 * are left alone, as are missing scripts and styles.
 */
@ApplicationScoped
public class SpaFallback {

    private static final Pattern FILE_EXTENSION = Pattern.compile("\\.[A-Za-z0-9]+$");

    /**
     * Before the REST not-found handler, which would otherwise answer an
     * unrecognized path with JSON. Static files and excluded prefixes call
     * {@code next()} or do not match, so they keep their own handlers.
     */
    private static final int BEFORE_REST_NOT_FOUND = 0;

    void register(@Observes Router router) {
        router.getWithRegex("^/(?!api(?:/|$)|q(?:/|$)|start(?:/|$)|logout(?:/|$)|oidc(?:/|$)).+")
                .order(BEFORE_REST_NOT_FOUND)
                .handler(ctx -> {
                    String path = ctx.request().path();
                    if (path == null || FILE_EXTENSION.matcher(path).find() || !wantsHtml(ctx.request().getHeader("Accept"))) {
                        ctx.next();
                        return;
                    }
                    ctx.reroute("/index.html");
                });
    }

    /**
     * Document navigations ask for HTML. A missing script must not receive the shell.
     * Rest clients that send {@code *}{@code /*} still count, so an unrecognized path
     * can be replaced with All Feeds.
     */
    private static boolean wantsHtml(String accept) {
        if (accept == null || accept.isBlank()) {
            return true;
        }
        return accept.contains("text/html") || accept.contains("*/*");
    }
}
