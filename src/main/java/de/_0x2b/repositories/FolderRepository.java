// Repo Errors:
//- -1: General error
//- -2: duplicate key violation

package de._0x2b.repositories;

import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.models.Feed;
import de._0x2b.models.Folder;
import de._0x2b.services.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FolderRepository extends AbstractRepository<Folder> {

    private static final Logger logger = LoggerFactory.getLogger(FolderRepository.class);
    private static final String SELECT_COLS = """
            SELECT folder.id as "folder_id", folder.name as "folder_name", folder.color as "folder_color", feed.id  AS "feed_id", feed.name AS "feed_name", feed.url as "url", feed.feed_url AS "feed_url"
            """;
    private static final String FROM_JOIN = """
            FROM folder
            LEFT JOIN feed ON folder.id = feed.folder_id
            """;
    private static final String ORDER = """
            ORDER BY folder.name, feed.name
            """;
    private static final String WHERE = """
            WHERE folder.name = ?
            """;
    private static final String SELECT_ALL = SELECT_COLS + FROM_JOIN + ORDER;
    private static final String SELECT_ALL_BY_NAME = SELECT_COLS + FROM_JOIN + WHERE + ORDER;
    private static final String INSERT_ONE = """
            INSERT INTO folder (name, color) VALUES (?, ?) RETURNING id
            """;
    private static final String UPDATE = """
            UPDATE folder set name = ?, color = ? WHERE id = ? RETURNING id
            """;
    private static final String DELETE = """
            DELETE FROM folder WHERE id = ?
            """;

    public FolderRepository(DatabaseService db) {
        super(db);
    }

    public List<Folder> findAll() {
        logger.debug("findAll");
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(SELECT_ALL)) {
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
        try (Connection conn = db.getConnection(); PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_BY_NAME)) {
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
        try {
            List<Object> params = List.of(folder.getName(), folder.getColor());

            return super.insert(INSERT_ONE, params);

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateEntityException("Folder with this Name already exists");
            }
            logger.error("Error creating feed", e);
            return -1;
        }
    }

    public int update(Folder folder) {
        try {
            List<Object> params = List.of(folder.getName(), folder.getColor(), folder.getId());

            int rows = super.update(UPDATE, params);
            return (rows > 0) ? folder.getId() : -1;
        } catch (SQLException e) {
            logger.error("Error updating feed", e);
            return -1;
        }
    }

    public int delete(int id) throws SQLException {
        return super.update(DELETE, List.of(id));
    }

    private ArrayList<Folder> parseResult(ResultSet rs) throws SQLException {
        logger.debug("parseResult");
        Map<Integer, Folder> folders = new LinkedHashMap<>();

        while (rs.next()) {
            int folderId = rs.getInt("folder_id");
            Folder folder = folders.computeIfAbsent(folderId, id -> {
                try {
                    return new Folder(id, rs.getString("folder_name"), new ArrayList<>(), rs.getString("folder_color"));
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            int feedId = rs.getInt("feed_id");
            if (!rs.wasNull()) {
                Feed feed = new Feed(feedId, folderId, rs.getString("feed_name"), URI.create(rs.getString("url")),
                        URI.create(rs.getString("feed_url")));
                folder.getFeeds().add(feed);
            }
        }
        return new ArrayList<>(folders.values());
    }
}
