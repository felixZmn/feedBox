package de._0x2b.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Smoke checks for path policies with OIDC disabled in tests.
 * Folder APIs stay protected via {@code @Authenticated}; health stays public.
 */
@QuarkusTest
class AuthPathPolicyIT {

    @Test
    void folderEndpoint_requiresAuth() {
        given().when().get("/api/folder").then().statusCode(401);
    }

    @Test
    void healthEndpoint_isPublic() {
        given().when().get("/q/health").then().statusCode(200);
    }

    @Test
    void spaShell_isPublic() {
        given().when().get("/").then().statusCode(200);
        given().when().get("/index.html").then().statusCode(200);
    }

    @Test
    void authProviders_isPublic() {
        given().when().get("/api/public/auth-providers").then()
                .statusCode(200)
                .body("providers", hasSize(1))
                .body("providers[0].id", equalTo("sso"))
                .body("providers[0].label", equalTo("Sign in with SSO"))
                .body("providers[0].href", equalTo("/start"));
    }

    @Test
    void startBounce_requiresAuth() {
        given().redirects().follow(false).when().get("/start").then().statusCode(401);
    }

    @Test
    void unreadAndSavedPaths_requireAuth() {
        given().redirects().follow(false).when().get("/unread").then().statusCode(401);
        given().redirects().follow(false).when().get("/saved").then().statusCode(401);
    }

    @Test
    void deepLinks_requireAuth() {
        given().redirects().follow(false).when().get("/article/1").then().statusCode(401);
        given().redirects().follow(false).when().get("/feed/1").then().statusCode(401);
        given().redirects().follow(false).when().get("/feed/1/article/2").then().statusCode(401);
        given().redirects().follow(false).when().get("/folder/1").then().statusCode(401);
        given().redirects().follow(false).when().get("/folder/1/article/2").then().statusCode(401);
        given().redirects().follow(false).when().get("/unread/article/1").then().statusCode(401);
        given().redirects().follow(false).when().get("/saved/article/1").then().statusCode(401);
    }

    @Test
    void articleApi_requiresAuth() {
        given().redirects().follow(false).when().get("/api/article/1").then().statusCode(401);
    }

    @Test
    void unrecognizedHtmlPath_returnsShell() {
        given().redirects().follow(false)
                .when().get("/no-such")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("id=\"login-panel\""))
                .body(not(containsString("\"feedId\"")));
    }

    @Test
    void missingScript_isNotTheShell() {
        given().redirects().follow(false)
                .when().get("/scripts/does-not-exist.js")
                .then()
                .statusCode(404);
    }
}
