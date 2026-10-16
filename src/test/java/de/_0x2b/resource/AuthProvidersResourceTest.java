package de._0x2b.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuthProvidersResourceTest {

    @Test
    void sanitizeLabel_trimsAndCollapsesWhitespace() {
        assertEquals("Sign in with Authentik",
                AuthProvidersResource.sanitizeLabel("  Sign in with Authentik  "));
        assertEquals("Sign in with SSO", AuthProvidersResource.sanitizeLabel(""));
        assertEquals("Sign in with SSO", AuthProvidersResource.sanitizeLabel(null));
        assertEquals("Sign in with SSO",
                AuthProvidersResource.sanitizeLabel("x".repeat(81)));
    }

    @Test
    void sanitizeHref_rejectsOpenRedirects() {
        assertEquals("/start", AuthProvidersResource.sanitizeHref("/start"));
        assertEquals("/oidc/start", AuthProvidersResource.sanitizeHref("/oidc/start"));
        assertEquals("/start", AuthProvidersResource.sanitizeHref("//evil.example"));
        assertEquals("/start", AuthProvidersResource.sanitizeHref("https://evil.example"));
        assertEquals("/start", AuthProvidersResource.sanitizeHref(null));
    }
}
