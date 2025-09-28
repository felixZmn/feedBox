// Repo Errors:
//- -1: General error
//- -2: duplicate key violation

package de._0x2b.repositories;

import de._0x2b.database.Database;
import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.models.Feed;
import de._0x2b.models.Folder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FolderRepository {
    private static final Logger logger = LoggerFactory.getLogger(FolderRepository.class);

    private static final String SELECT_ALL = """
            SELECT folder.id as "folder_id", folder.name as "folder_name", folder.color as "folder_color", feed.id  AS "feed_id", feed.name AS "feed_name", feed.url as "url", feed.feed_url AS "feed_url"
            FROM folder
            LEFT JOIN feed ON folder.id = feed.folder_id
            ORDER BY folder.name, feed.name
            """;

    private static final String SELECT_ALL_BY_NAME = """
            SELECT folder.id as "folder_id", folder.name as "folder_name", folder.color as "folder_color", feed.id  AS "feed_id", feed.name AS "feed_name", feed.url as "url", feed.feed_url AS "feed_url"
            FROM folder
            LEFT JOIN feed ON folder.id = feed.folder_id
            wHERE folder.name = ?
            ORDER BY folder.name, feed.name
            """;

    private static final String INSERT_ONE = """
            INSERT INTO folder (name, color) VALUES (?, ?) RETURNING id
            """;

    private static final String UPDATE = """
            UPDATE folder set name = ?, color = ? WHERE id = ? RETURNING id
            """;

    private static final String DELETE = """
            DELETE FROM folder WHERE id = ?
            """;

    // safe getter that returns null if column missing or SQL NULL
    private static String safeGetString(ResultSet rs, String column) {
        logger.debug("safeGetString");
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    public List<Folder> findAll() {
        logger.debug("findAll");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public List<Folder> findByName(String name) {
        logger.debug("findByName");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_BY_NAME)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public int create(Folder folder) {
        logger.debug("create");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_ONE)) {
            stmt.setString(1, folder.getName());
            stmt.setString(2, folder.getColor());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                throw new DuplicateEntityException("Folder with this URL already exists");
            }
            logger.error("Error executing SQL statement", e);
        }
        return -1;
    }

    public int update(Folder folder) {
        logger.debug("update");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE)) {

            stmt.setString(1, folder.getName());
            stmt.setString(2, folder.getColor());
            stmt.setInt(3, folder.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                throw new DuplicateEntityException("Folder with this URL already exists");
            }
            logger.error("Error executing SQL statement", e);
        }
        return -1;
    }

    public int delete(int id) {
        logger.debug("delete");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return -1;
    }

    private ArrayList<Folder> parseResult(ResultSet rs) throws SQLException {
        logger.debug("parseResult");
        Map<Integer, Folder> folders = new LinkedHashMap<>();

        while (rs.next()) {
            int folderId = rs.getInt("folder_id");
            Folder folder = folders.computeIfAbsent(
                    folderId,
                    id -> {
                        try {
                            return new Folder(
                                    id,
                                    rs.getString("folder_name"),
                                    new ArrayList<>(),
                                    rs.getString("folder_color")
                            );
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
            int feedId = rs.getInt("feed_id");
            if (!rs.wasNull()) {
                Feed feed = new Feed(
                        feedId,
                        folderId,
                        rs.getString("feed_name"),
                        rs.getString("url"),
                        rs.getString("feed_url")
                );
                folder.getFeeds().add(feed);
            }
        }
        return new ArrayList<>(folders.values());
    }
}
