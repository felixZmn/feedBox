package de._0x2b.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de._0x2b.model.Feed;
import de._0x2b.repository.FeedRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
class FeedResourceIT {

    @Inject
    FeedRepository feedRepository;

    private int insertFeed(String name) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return feedRepository.create(new Feed(-1, null, name,
                URI.create("https://example.com/" + suffix),
                URI.create("https://example.com/" + suffix + "/rss")));
    }

    private String feedUrlOf(int id) {
        return feedRepository.findOne(id).getFirst().getFeedUrl().toString();
    }

    @Test
    @TestSecurity(user = "alice")
    void updateFeed_renamesWhenNameGiven() {
        int id = insertFeed("Old name");

        given().contentType(ContentType.JSON)
                .body(Map.of("feedUrl", feedUrlOf(id), "name", "  New name  "))
                .when().put("/api/feed/" + id)
                .then().statusCode(200);

        given().when().get("/api/feed")
                .then().statusCode(200)
                .body("find { it.id == " + id + " }.name", equalTo("New name"));
    }

    @Test
    @TestSecurity(user = "alice")
    void updateFeed_keepsNameWhenBlank() {
        int id = insertFeed("Keep me");

        given().contentType(ContentType.JSON)
                .body(Map.of("feedUrl", feedUrlOf(id), "name", " "))
                .when().put("/api/feed/" + id)
                .then().statusCode(200);

        given().when().get("/api/feed")
                .then().statusCode(200)
                .body("find { it.id == " + id + " }.name", equalTo("Keep me"));
    }

    @Test
    @TestSecurity(user = "alice")
    void updateFeed_missingFeedIsJsonNotFound() {
        given().contentType(ContentType.JSON)
                .body(Map.of("feedUrl", "https://example.com/rss"))
                .when().put("/api/feed/999999")
                .then().statusCode(404)
                .contentType(ContentType.JSON)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "alice")
    void createFeed_privateAddressIsRejectedWithCode() {
        given().contentType(ContentType.JSON)
                .body(Map.of("feedUrl", "http://127.0.0.1/feed.xml"))
                .when().post("/api/feed")
                .then().statusCode(422)
                .contentType(ContentType.JSON)
                .body("code", equalTo("BLOCKED"))
                .body("message", equalTo("Host not allowed"));
    }

    @Test
    @TestSecurity(user = "alice")
    void checkFeed_privateAddressIsRejectedWithCode() {
        given().queryParam("url", "http://127.0.0.1/")
                .when().get("/api/feed/check")
                .then().statusCode(422)
                .contentType(ContentType.JSON)
                .body("code", equalTo("BLOCKED"));
    }

    @Test
    @TestSecurity(user = "alice")
    void checkFeed_overlongUrlIsBadRequest() {
        given().queryParam("url", "https://example.com/" + "a".repeat(FeedResource.MAX_URL_LENGTH))
                .when().get("/api/feed/check")
                .then().statusCode(400)
                .contentType(ContentType.JSON)
                .body("code", equalTo("BAD_REQUEST"));
    }
}
