package de._0x2b.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de._0x2b.database.Database;
import de._0x2b.models.Feed;
import de._0x2b.models.Folder;

public class FolderRepository {

    private static final String SELECT_ALL = """
            SELECT folder.id as "folder_id", folder.name as "folder_name", folder.color as "folder_color", feed.id  AS "feed_id", feed.name AS "feed_name", feed.url AS "feed_url"
            FROM folder
            LEFT JOIN feed ON folder.id = feed.folder_id
            ORDER BY folder.name, feed.name
            """;

    private static final String INSERT_ONE = """
            INSERT INTO folder (name, color) VALUES (?, ?) ON CONFLICT (name) DO UPDATE SET name = excluded.name RETURNING id
            """;

    public List<Folder> findAll() {
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(SELECT_ALL)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    public int save(Folder folder) {
        try (Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(INSERT_ONE)) {

            stmt.setString(1, folder.getName());
            stmt.setString(2, folder.getColor());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    return id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private List<Folder> parseResult(ResultSet rs) throws SQLException {
        Map<Integer, FolderBuilder> folders = new LinkedHashMap<>();

        while (rs.next()) {
            int folderId = rs.getInt("folder_id");
            String folderName = rs.getString("folder_name");
            String folderColor = rs.getString("folder_color");

            FolderBuilder fb = folders.get(folderId);
            if (fb == null) {
                fb = new FolderBuilder(folderId, folderName, folderColor);
                folders.put(folderId, fb);
            }

            // feed_id may be NULL
            int feedIdVal = rs.getInt("feed_id");
            if (!rs.wasNull()) {
                int feedId = feedIdVal;
                String feedName = rs.getString("feed_name");
                // prefer "url" column if present, otherwise "feed_url"
                String url = safeGetString(rs, "url");
                String feedUrl = safeGetString(rs, "feed_url");
                if (url == null)
                    url = feedUrl;

                Feed feed = new Feed(feedId, folderId, feedName, url, feedUrl);
                fb.feeds.add(feed);
            }
        }

        List<Folder> result = new ArrayList<>(folders.size());
        for (FolderBuilder fb : folders.values()) {
            // you can wrap with Collections.unmodifiableList(...) if you want immutability
            Folder folder = new Folder(fb.id, fb.name, fb.feeds, fb.color);
            result.add(folder);
        }
        return result;
    }

    // small helper to hold folder interim data
    private static class FolderBuilder {
        final int id;
        final String name;
        final String color;
        final List<Feed> feeds = new ArrayList<>();

        FolderBuilder(int id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }
    }

    // safe getter that returns null if column missing or SQL NULL
    private static String safeGetString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }
}
