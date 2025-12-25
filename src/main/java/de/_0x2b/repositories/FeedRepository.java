package de._0x2b.repositories;

import de._0x2b.exceptions.DuplicateEntityException;
import de._0x2b.models.Feed;
import de._0x2b.services.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FeedRepository extends AbstractRepository<Feed> {
    private static final Logger logger = LoggerFactory.getLogger(FeedRepository.class);
    private static final String SELECT_COLS = """
            SELECT id, folder_id, name, url, feed_url
            """;
    private static final String FROM = " FROM feed";
    private static final String INSERT_SQL = """
            INSERT INTO feed (folder_id, name, url, feed_url) VALUES (?, ?, ?, ?) RETURNING id
            """;
    private static final String UPDATE = """
            UPDATE feed set folder_id = ?, name = ?, url = ?, feed_url = ? WHERE id = ? RETURNING id
            """;
    private static final String DELETE = """
            DELETE FROM feed WHERE id = ?
            """;
    private final RowMapper<Feed> feedMapper = rs -> new Feed(rs.getInt("id"), rs.getInt("folder_id"),

            rs.getString("name"), URI.create(rs.getString("url")), URI.create(rs.getString("feed_url")));

    public FeedRepository(DatabaseService db) {
        super(db);
    }

    public int create(Feed feed) {
        try {
            List<Object> params = List.of(feed.getFolderId(), feed.getName(), feed.getURI().toString(),
                    feed.getFeedURI().toString());

            return super.insert(INSERT_SQL, params);

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new DuplicateEntityException("Feed with this URL already exists");
            }
            logger.error("Error creating feed", e);
            return -1;
        }
    }

    public int update(Feed feed) {
        try {
            List<Object> params = List.of(feed.getFolderId(), feed.getName(), feed.getURI().toString(),
                    feed.getFeedURI().toString(), feed.getId());

            int rows = super.update(UPDATE, params);
            return (rows > 0) ? feed.getId() : -1;
        } catch (SQLException e) {
            logger.error("Error updating feed", e);
            return -1;
        }
    }

    public List<Feed> findAll() {
        return findInternal(null, null);
    }

    public List<Feed> findOne(int id) {
        return findInternal("id = ?", List.of(id));
    }

    public int delete(int id) throws SQLException {
        return super.update(DELETE, List.of(id));
    }

    private List<Feed> findInternal(String whereClause, List<Object> initialParams) {

        StringBuilder sql = new StringBuilder(SELECT_COLS).append(FROM);
        List<Object> params = new ArrayList<>();

        if (initialParams != null) {
            params.addAll(initialParams);
        }

        // Add where
        sql.append(" WHERE 1=1 ");
        if (whereClause != null) {
            sql.append(" AND ").append(whereClause);
        }

        return super.query(sql.toString(), feedMapper, params);
    }
}
