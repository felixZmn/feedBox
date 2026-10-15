package de._0x2b.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de._0x2b.model.Article;
import de._0x2b.model.Feed;
import de._0x2b.model.Folder;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.repository.FeedRepository;
import de._0x2b.repository.FolderRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

@QuarkusTest
class ArticleResourceSecurityIT {

    @Inject
    FolderRepository folderRepository;
    @Inject
    FeedRepository feedRepository;
    @Inject
    ArticleRepository articleRepository;

    @Test
    void anonymousGetArticles_isUnauthorized() {
        given().when().get("/api/article").then().statusCode(401);
    }

    @Test
    void anonymousGetArticleById_isUnauthorized() {
        given().when().get("/api/article/1").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    void authenticatedList_returnsSummariesWithoutRequiringContent() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "sec-folder-" + suffix, null, "f-base"));
        int feedId = feedRepository.create(new Feed(-1, folderId, "sec-feed-" + suffix,
                URI.create("https://sec.example/" + suffix),
                URI.create("https://sec.example/" + suffix + "/rss")));
        Instant published = Instant.parse("2026-03-01T10:00:00Z");
        articleRepository.create(List.of(new Article(-1, feedId, "sec-feed-" + suffix, "Sec Title",
                "hidden-desc", "<p>secret-body</p>", "https://sec.example/a", "https://sec.example/a",
                published, "", "", "[]")));

        given()
                .when().get("/api/article?feed=" + feedId)
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].title", equalTo("Sec Title"))
                .body("[0].content", equalTo(null))
                .body("[0].description", equalTo(null));

        long id = articleRepository.findByFeed(feedId, "").getFirst().id();
        given()
                .when().get("/api/article/" + id)
                .then()
                .statusCode(200)
                .body("content", equalTo("<p>secret-body</p>"))
                .body("title", equalTo("Sec Title"));
    }

    @Test
    @TestSecurity(user = "alice")
    void authenticatedMarkRead_missingArticle_returns404() {
        given()
                .contentType("application/json")
                .when().post("/api/article/" + (Long.MAX_VALUE - 3) + "/read")
                .then()
                .statusCode(404);
    }
}
