package de._0x2b.config;

import java.util.*;

public record Config(
        DbConfig db,
        AppConfig app,
        Optional<OidcConfig> oidc  // optional
) {
    public static Config load() {
        return new Config(
                DbConfig.fromEnv(),
                AppConfig.fromEnv(),
                Optional.ofNullable(tryLoadOidcConfig())
        );
    }

    private static OidcConfig tryLoadOidcConfig() {
        try {
            return OidcConfig.fromEnv();
        } catch (IllegalStateException e) {
            // OIDC env vars are missing -> OIDC is disabled
            return null;
        }
    }
}