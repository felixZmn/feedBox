package de._0x2b.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ConfigResourceIT {

    @Test
    void configEndpoint_isPublic() {
        given()
                .when().get("/api/config")
                .then()
                .statusCode(200)
                .body("clientId", equalTo("feedbox-test"))
                .body("authServerUrl", equalTo("https://feedbox.test/"));
    }

    @Test
    void folderEndpoint_requiresAuth() {
        given().when().get("/api/folder").then().statusCode(401);
    }
}
