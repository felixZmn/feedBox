package de._0x2b.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import de._0x2b.model.Folder;
import de._0x2b.repository.FolderRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;

@QuarkusTest
class FolderResourceIT {

    @Inject
    FolderRepository folderRepository;

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
}
