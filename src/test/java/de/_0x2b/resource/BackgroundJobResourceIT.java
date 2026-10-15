package de._0x2b.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
class BackgroundJobResourceIT {

    @Test
    @TestSecurity(user = "alice")
    void refresh_returnsAcceptedJob_andBecomesTerminal() {
        String jobId = given()
                .contentType("application/json")
                .when().post("/api/feed/refresh")
                .then()
                .statusCode(202)
                .body("id", notNullValue())
                .body("type", equalTo("REFRESH"))
                .body("status", anyOf(equalTo("RUNNING"), equalTo("COMPLETED"), equalTo("FAILED")))
                .extract()
                .path("id");

        awaitTerminal(UUID.fromString(jobId));
    }

    @Test
    @TestSecurity(user = "alice")
    void getUnknownJob_returns404() {
        given()
                .when().get("/api/jobs/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void refresh_requiresAuth() {
        given().when().post("/api/feed/refresh").then().statusCode(401);
    }

    private void awaitTerminal(UUID id) {
        for (int i = 0; i < 100; i++) {
            String status = given()
                    .when().get("/api/jobs/" + id)
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("status");
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("job did not finish: " + id);
    }
}
