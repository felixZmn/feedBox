package de._0x2b.repository;

import de._0x2b.exception.DuplicateEntityException;
import de._0x2b.model.Feed;
import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@ApplicationScoped
public class FeedRepository extends AbstractRepository<Feed> {
    private static final Logger logger = LoggerFactory.getLogger(FeedRepository.class);
    private static final String SELECT_COLS = """
            SELECT id, folder_id, name, url, feed_url, last_refreshed_at, last_error
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
    private static final String MARK_REFRESH_SUCCESS = """
            UPDATE feed SET last_refreshed_at = ?, last_error = NULL WHERE id = ?
            """;
    private static final String MARK_REFRESH_ERROR = """
            UPDATE feed SET last_error = ?, last_refreshed_at = COALESCE(last_refreshed_at, ?) WHERE id = ?
            """;
    // Same as ArticleRepository: the PG driver doesn't bind
    // TIMESTAMPTZ -> Instant via getObject(idx, Class). Use
    // getTimestamp(idx, UTC) and convert.
    private static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private final RowMapper<Feed> feedMapper = rs -> {
        Feed f = new Feed(rs.getInt("id"), rs.getInt("folder_id"),
                rs.getString("name"), URI.create(rs.getString("url")), URI.create(rs.getString("feed_url")));
        Timestamp ts = rs.getTimestamp("last_refreshed_at", UTC);
        f.setLastRefreshedAt(ts == null ? null : ts.toInstant());
        f.setLastError(rs.getString("last_error"));
        return f;
    };

    public FeedRepository() {
    }

    public int create(Feed feed) {
        try {
            List<Object> params = List.of(feed.getFolderId(), feed.getName(), feed.getUrl().toString(),
                    feed.getFeedUrl().toString());

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
            List<Object> params = List.of(feed.getFolderId(), feed.getName(), feed.getUrl().toString(),
                    feed.getFeedUrl().toString(), feed.getId());

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

    /**
     * Mark a feed as successfully refreshed. Clears any previous error and
     * sets the last_refreshed_at column to the supplied timestamp (or
     * "now" if null). The caller is expected to supply a stable timestamp
     * so the value can be matched to the article batch that was committed
     * alongside it.
     */
    public void markRefreshSuccess(int feedId, Instant at) throws SQLException {
        super.update(MARK_REFRESH_SUCCESS, List.of(java.sql.Timestamp.from(at != null ? at : Instant.now()), feedId));
    }

    /**
     * Mark a feed as failing the last refresh. Preserves the previous
     * last_refreshed_at so a long-standing broken feed still surfaces a
     * "last seen working" timestamp.
     *
     * @param feedId the feed id
     * @param error  the error message; truncated to 1000 chars to keep
     *               multi-MB stack traces out of the database
     */
    public void markRefreshError(int feedId, String error) throws SQLException {
        String trimmed = error == null ? null
                : (error.length() > 1000 ? error.substring(0, 1000) : error);
        super.update(MARK_REFRESH_ERROR, List.of(trimmed, java.sql.Timestamp.from(Instant.now()), feedId));
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
