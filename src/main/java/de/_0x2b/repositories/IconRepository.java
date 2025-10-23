package de._0x2b.repositories;

import de._0x2b.database.Database;
import de._0x2b.models.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IconRepository {
    private static final Logger logger = LoggerFactory.getLogger(IconRepository.class);

    private static final String SELECT_ONE_BY_FEED = """
            SELECT id, feed_id, image, mime_type, file_name from icon where feed_id = ?
            """;

    private static final String INSERT_ONE = """
            INSERT INTO icon (feed_id, mime_type, file_name, image) VALUES (?, ?, ?, ?) RETURNING id
            """;

    public List<Icon> findOneByFeed(int id) {
        logger.debug("findOneByFeed");

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ONE_BY_FEED)) {

            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public int create(Icon icon) {
        logger.debug("create");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_ONE)) {

            stmt.setInt(1, icon.getFeedId());
            stmt.setString(2, icon.getMimeType());
            stmt.setString(3, icon.getFileName());
            stmt.setBytes(4, icon.getImage());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
//            if (e.getSQLState().equals("23505")) {
//                throw new DuplicateEntityException("Feed with this URL already exists");
//            }
            logger.error("Error executing SQL statement", e);
        }
        return -1;
    }

    private List<Icon> parseResult(ResultSet rs) throws SQLException {
        logger.debug("parseResult");
        List<Icon> feeds = new ArrayList<>();
        while (rs.next()) {
            feeds.add(new Icon(
                    rs.getInt("id"),
                    rs.getInt("feed_id"),
                    rs.getBytes("image"),
                    rs.getString("mime_type"),
                    rs.getString("file_name"), ""));
        }
        return feeds;
    }
}
