package de._0x2b.repositories;

import de._0x2b.database.Database;
import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.models.Feed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FeedRepository {
    private static final Logger logger = LoggerFactory.getLogger(FeedRepository.class);

    private static final String INSERT_ONE = """
            INSERT INTO feed (folder_id, name, url, feed_url) VALUES (?, ?, ?, ?) RETURNING id
            """;
    private static final String SELECT_ALL = """
            SELECT id, folder_id, name, url, feed_url FROM feed
            """;
    private static final String SELECT_ONE = """
            SELECT id, folder_id, name, url, feed_url FROM feed where id = ?
            """;
    private static final String UPDATE = """
            UPDATE feed set folder_id = ?, name = ?, url = ?, feed_url = ? WHERE id = ? RETURNING id
            """;
    private static final String DELETE = """
            DELETE FROM feed WHERE id = ?
            """;

    public int create(Feed feed) {
        logger.debug("create");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_ONE)) {

            stmt.setInt(1, feed.getFolderId());
            stmt.setString(2, feed.getName());
            stmt.setString(3, feed.getURI().toString());
            stmt.setString(4, feed.getFeedURI().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                throw new DuplicateEntityException("Feed with this URL already exists");
            }
            logger.error("Error executing SQL statement", e);
        }
        return -1;
    }

    public int update(Feed feed) {
        logger.debug("update");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE)) {

            stmt.setInt(1, feed.getFolderId());
            stmt.setString(2, feed.getName());
            stmt.setString(3, feed.getURI().toString());
            stmt.setString(4, feed.getFeedURI().toString());
            stmt.setInt(5, feed.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                throw new DuplicateEntityException("Feed with this URL already exists");
            }
            logger.error("Error executing SQL statement", e);
        }
        return -1;
    }

    public List<Feed> findAll() {
        logger.debug("findAll");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL)) {
            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public List<Feed> findOne(int id) {
        logger.debug("findOne");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ONE)) {

            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
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

    private List<Feed> parseResult(ResultSet rs) throws SQLException {
        logger.debug("parseResult");
        List<Feed> feeds = new ArrayList<>();
        while (rs.next()) {
            feeds.add(new Feed(rs.getInt("id"), rs.getInt("folder_id"), rs.getString("name"), URI.create(rs.getString("url")),
                    URI.create(rs.getString("feed_url"))));
        }
        return feeds;
    }
}
