package de._0x2b.repositories;

import de._0x2b.database.Database;
import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.models.Feed;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FeedRepository {
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
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_ONE)) {

            stmt.setInt(1, feed.getFolderId());
            stmt.setString(2, feed.getName());
            stmt.setString(3, feed.getUrl());
            stmt.setString(4, feed.getFeedUrl());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            if (e.getSQLState().equals("23505")) {
                throw new DuplicateEntityException("Feed with this URL already exists");
            }
        }
        return -1;
    }

    public int update(Feed feed) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE)) {

            stmt.setInt(1, feed.getFolderId());
            stmt.setString(2, feed.getName());
            stmt.setString(3, feed.getUrl());
            stmt.setString(4, feed.getFeedUrl());
            stmt.setInt(5, feed.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            if (e.getSQLState().equals("23505")) {
                throw new DuplicateEntityException("Feed with this URL already exists");
            }
        }
        return -1;
    }

    public List<Feed> findAll() {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL)) {
            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public List<Feed> findOne(int id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ONE)) {

            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public int delete(int id) {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private List<Feed> parseResult(ResultSet rs) throws SQLException {
        List<Feed> feeds = new ArrayList<>();
        while (rs.next()) {
            feeds.add(new Feed(rs.getInt("id"), rs.getInt("folder_id"), rs.getString("name"), rs.getString("url"),
                    rs.getString("feed_url")));
        }
        return feeds;
    }
}
