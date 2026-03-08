package de._0x2b.services;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import de._0x2b.config.OidcConfig;

import java.net.URI;

public class OidcService {
    private final OIDCProviderMetadata providerMetadata;
    private final ClientID clientID;
    private final Secret clientSecret;
    private final URI redirectURI;
    private final Scope scope;

    public OidcService(OidcConfig config) throws Exception {
        this.providerMetadata = OIDCProviderMetadata.resolve(new Issuer(config.issuerUrl()));
        this.clientID = new ClientID(config.clientId());
        this.clientSecret = new Secret(config.clientSecret());
        this.redirectURI = new URI(config.redirectUri());
        String[] scopes = config.scopes().toArray(String[]::new);
        this.scope = new Scope(scopes);
    }

    public String getAuthorizationRequestURI() {
        AuthenticationRequest authRequest = new AuthenticationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE),
                new Scope(scope),
                clientID,
                redirectURI)
                .endpointURI(providerMetadata.getAuthorizationEndpointURI())
                .build();

        return authRequest.toURI().toString();
    }

    public Tokens exchangeCodeForTokens(String code) throws Exception {
        TokenRequest tokenRequest = new TokenRequest(
                providerMetadata.getTokenEndpointURI(),
                new ClientSecretBasic(this.clientID, this.clientSecret),
                new AuthorizationCodeGrant(new AuthorizationCode(code), redirectURI),
                scope);

        TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenRequest.toHTTPRequest().send());

        if (!tokenResponse.indicatesSuccess()) {
            throw new Exception("Token request failed: " + tokenResponse.toErrorResponse().getErrorObject());
        }

        OIDCTokenResponse successResponse = (OIDCTokenResponse) tokenResponse;
        OIDCTokens oidcTokens = successResponse.getOIDCTokens();

        return new Tokens(
                oidcTokens.getIDTokenString(),
                oidcTokens.getAccessToken().toString(),
                oidcTokens.getRefreshToken() != null ? oidcTokens.getRefreshToken().toString() : "");
    }

    public static class Tokens {
        public final String idToken;
        public final String accessToken;
        public final String refreshToken;

        public Tokens(String idToken, String accessToken, String refreshToken) {
            this.idToken = idToken;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}