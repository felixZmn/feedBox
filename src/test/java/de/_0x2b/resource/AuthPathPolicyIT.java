package de._0x2b.resource;

import static io.restassured.RestAssured.given;

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
}
