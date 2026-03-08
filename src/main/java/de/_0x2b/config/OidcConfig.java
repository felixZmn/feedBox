package de._0x2b.config;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static de._0x2b.config.ConfigUtils.getEnv;

public record OidcConfig(
        String issuerUrl,
        String clientId,
        String clientSecret,
        String redirectUri,
        List<String> scopes
) {
    public OidcConfig {
        Objects.requireNonNull(issuerUrl, "OIDC_ISSUER_URI cannot be null");
        Objects.requireNonNull(clientId, "OIDC_CLIENT_ID cannot be null");
        Objects.requireNonNull(clientSecret, "OIDC_CLIENT_SECRET cannot be null");
        Objects.requireNonNull(redirectUri, "OIDC_REDIRECT_URI cannot be null");
        Objects.requireNonNull(scopes, "OIDC_SCOPES cannot be null");
    }

    public static OidcConfig fromEnv() {
        return new OidcConfig(
                getEnv("OIDC_ISSUER_URI"),
                getEnv("OIDC_CLIENT_ID"),
                getEnv("OIDC_CLIENT_SECRET"),
                getEnv("OIDC_REDIRECT_URI"),
                Arrays.asList(getEnv("OIDC_SCOPES", "openid,profile,email").split(","))
        );
    }
}
