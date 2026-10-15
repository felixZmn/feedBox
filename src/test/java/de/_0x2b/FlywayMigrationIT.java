package de._0x2b;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Smoke test: Flyway applies V1–V5 on a clean Dev Services Postgres.
 */
@QuarkusTest
class FlywayMigrationIT {

    @Inject
    DataSource dataSource;

    @Test
    void migratesArticleGuidAndSearchIndexes() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertTrue(columnExists(conn, "article", "guid"), "V5 should add article.guid");
            assertTrue(columnExists(conn, "article", "read"), "V4 should add article.read");
            assertTrue(indexExists(conn, "article_feed_guid_uidx"));
            assertTrue(indexExists(conn, "article_feed_link_uidx"));
            assertTrue(indexExists(conn, "article_published_id_idx"));
            assertTrue(indexExists(conn, "article_title_trgm_idx"));
            assertTrue(extensionExists(conn, "pg_trgm"));
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT 1 FROM information_schema.columns
                WHERE table_name = ? AND column_name = ?
                """)) {
            stmt.setString(1, table);
            stmt.setString(2, column);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean indexExists(Connection conn, String indexName) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT 1 FROM pg_indexes WHERE indexname = ?
                """)) {
            stmt.setString(1, indexName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean extensionExists(Connection conn, String extension) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT 1 FROM pg_extension WHERE extname = ?
                """)) {
            stmt.setString(1, extension);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
