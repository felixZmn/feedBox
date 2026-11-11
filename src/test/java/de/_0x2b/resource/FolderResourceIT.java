package de._0x2b.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de._0x2b.model.Feed;
import de._0x2b.model.Folder;
import de._0x2b.repository.FeedRepository;
import de._0x2b.repository.FolderRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

@QuarkusTest
class FolderResourceIT {

    @Inject
    FolderRepository folderRepository;

    @Inject
    FeedRepository feedRepository;

    @Test
    void anonymousGetFolders_isUnauthorized() {
        given().when().get("/api/folder").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    void getFolders_omitsFeedsProperty() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        folderRepository.create(new Folder(-1, "folder-json-" + suffix, null, "f-base"));

        given()
                .when().get("/api/folder")
                .then()
                .statusCode(200)
                .body("find { it.name == 'folder-json-" + suffix + "' }.name",
                        equalTo("folder-json-" + suffix))
                .body("find { it.name == 'folder-json-" + suffix + "' }", not(hasKey("feeds")));
    }

    @Test
    void parseFeedsMode_defaultsToKeep() {
        assertTrue(FolderResource.parseFeedsMode(null));
        assertTrue(FolderResource.parseFeedsMode(""));
        assertTrue(FolderResource.parseFeedsMode("keep"));
    }

    @Test
    void parseFeedsMode_acceptsDelete() {
        assertEquals(false, FolderResource.parseFeedsMode("delete"));
    }

    @Test
    void parseFeedsMode_rejectsUnknown() {
        org.junit.jupiter.api.Assertions.assertThrows(
                BadRequestException.class, () -> FolderResource.parseFeedsMode("archive"));
    }

    @Test
    @TestSecurity(user = "alice")
    void deleteFolder_defaultKeep_movesFeedsToNoFolder() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "keep-feeds-" + suffix, null, "f-base"));
        int feedId = feedRepository.create(new Feed(-1, folderId, "kept-" + suffix,
                URI.create("https://keep.example/" + suffix),
                URI.create("https://keep.example/" + suffix + "/rss")));

        given()
                .when().delete("/api/folder/" + folderId)
                .then()
                .statusCode(204);

        assertNull(folderRepository.findById(folderId));
        Feed kept = feedRepository.findOne(feedId).getFirst();
        assertNull(kept.getFolderId());
        assertEquals("kept-" + suffix, kept.getName());
    }

    @Test
    @TestSecurity(user = "alice")
    void deleteFolder_feedsDelete_removesFeeds() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "drop-feeds-" + suffix, null, "f-base"));
        int feedId = feedRepository.create(new Feed(-1, folderId, "gone-" + suffix,
                URI.create("https://drop.example/" + suffix),
                URI.create("https://drop.example/" + suffix + "/rss")));

        given()
                .queryParam("feeds", "delete")
                .when().delete("/api/folder/" + folderId)
                .then()
                .statusCode(204);

        assertNull(folderRepository.findById(folderId));
        assertTrue(feedRepository.findOne(feedId).isEmpty());
    }

    @Test
    @TestSecurity(user = "alice")
    void deleteFolder_rejectsUnknownFeedsMode() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int folderId = folderRepository.create(new Folder(-1, "bad-mode-" + suffix, null, "f-base"));

        given()
                .queryParam("feeds", "archive")
                .when().delete("/api/folder/" + folderId)
                .then()
                .statusCode(400);
    }
}
