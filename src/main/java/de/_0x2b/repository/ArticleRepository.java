package de._0x2b.repository;

import de._0x2b.exception.DataAccessException;
import de._0x2b.model.Article;
import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@ApplicationScoped
public class ArticleRepository extends AbstractRepository<Article> {

    private static final Logger logger = LoggerFactory.getLogger(ArticleRepository.class);
    private static final String SELECT_COLS = """
            SELECT a.id, a.feed_id, f.name AS publisher, a.title, a.description,
                   a.content, a.link, a.published, a.authors, a.image_url, a.categories
            """;
    private static final String FROM_JOIN = """
            FROM article a
            JOIN feed f ON f.id = a.feed_id
            """;
    private static final String JOIN_FOLDER = " JOIN folder fo on fo.id = f.folder_id ";
    // The pagination cursor is now a real TIMESTAMPTZ. Articles with NULL
    // pubDate are excluded from the read query entirely (see WHERE clause
    // below) so we don't need NULLS FIRST/LAST gymnastics here.
    private static final String WHERE_PAGINATION = " AND ((a.published < ?) OR (a.published = ? AND a.id < ?)) ";
    private static final String ORDER_LIMIT = " ORDER BY a.published DESC, a.id DESC LIMIT 25";
    private static final String INSERT_SQL = """
            INSERT INTO article (feed_id, title, description, content, link, published, authors, image_url, categories)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (link) DO NOTHING
            """;
    private static final String DELETE_BY_FEED_SQL = "DELETE FROM article WHERE feed_id = ?";
    // The PostgreSQL JDBC driver does NOT support
    // ResultSet.getObject(int, Class) for java.time.Instant on a
    // timestamptz column - it raises
    // "conversion to class java.time.Instant from timestamptz not
    // supported". The supported path is getTimestamp(idx, Calendar)
    // with a UTC calendar (so the driver doesn't re-project the stored
    // UTC value into the JVM's default time zone), then
    // Timestamp.toInstant().
    private static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private final RowMapper<Article> articleMapper = rs -> new Article(rs.getLong("id"), rs.getInt("feed_id"),
            rs.getString("publisher"), rs.getString("title"), rs.getString("description"), rs.getString("content"),
            rs.getString("link"), readPublished(rs), rs.getString("authors"),
            rs.getString("image_url"), rs.getString("categories"));

    private static Instant readPublished(java.sql.ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("published", UTC);
        return ts == null ? null : ts.toInstant();
    }

    public ArticleRepository() {
    }

    public List<Article> findAll() {
        return findInternal(null, null, null, null);
    }

    public List<Article> findAll(long paginationId, String paginationDate) {
        return findInternal(null, null, paginationId, paginationDate);
    }

    public List<Article> findByFeed(int feedId) {
        return findInternal("f.id = ?", List.of(feedId), null, null);
    }

    public List<Article> findByFeed(int feedId, long paginationId, String paginationDate) {
        return findInternal("f.id = ?", List.of(feedId), paginationId, paginationDate);
    }

    public List<Article> findByFolder(int folderId) {
        return findInternal("fo.id = ?", List.of(folderId), null, null, true);
    }

    public List<Article> findByFolder(int folderId, long paginationId, String paginationDate) {
        return findInternal("fo.id = ?", List.of(folderId), paginationId, paginationDate, true);
    }

    public void deleteByFeed(int feedId) throws SQLException {
        super.update(DELETE_BY_FEED_SQL, List.of(feedId));
    }

    public void create(List<Article> articles) {
        if (articles == null || articles.isEmpty())
            return;

        logger.debug("Starting batch create for {} articles", articles.size());

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // Start Transaction

            try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
                int count = 0;
                for (Article a : articles) {
                    stmt.setInt(1, a.getFeedId());
                    stmt.setString(2, a.getTitle());
                    stmt.setString(3, a.getDescription());
                    stmt.setString(4, a.getContent());
                    stmt.setString(5, a.getLink());
                    if (a.getPublished() != null) {
                        stmt.setTimestamp(6, Timestamp.from(a.getPublished()));
                    } else {
                        stmt.setNull(6, Types.TIMESTAMP_WITH_TIMEZONE);
                    }
                    stmt.setString(7, a.getAuthors());
                    stmt.setString(8, a.getImageUrl());
                    stmt.setString(9, a.getCategories());
                    stmt.addBatch();

                    if (++count % 500 == 0)
                        stmt.executeBatch();
                }
                stmt.executeBatch(); // Execute remaining
                conn.commit(); // Commit Transaction
            } catch (SQLException e) {
                // Roll back the open transaction before propagating the failure.
                // The previous implementation swallowed this, which meant the
                // scheduler would silently lose a batch of articles and the
                // caller (FeedService.parseFeed) had no way to know.
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw new DataAccessException("Batch insert failed for feed_id=" + articles.getFirst().getFeedId(), e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database connection error during batch insert", e);
        }
    }

    private List<Article> findInternal(String whereClause, List<Object> params, Long pagId, String pagDate) {
        return findInternal(whereClause, params, pagId, pagDate, false);
    }

    private List<Article> findInternal(String whereClause, List<Object> initialParams, Long pagId, String pagDate,
            boolean joinFolder) {

        StringBuilder sql = new StringBuilder(SELECT_COLS).append(FROM_JOIN);
        List<Object> params = new ArrayList<>();

        if (initialParams != null) {
            params.addAll(initialParams);
        }

        if (joinFolder) {
            sql.append(JOIN_FOLDER);
        }

        // We exclude articles with NULL pubDate from the read query
        // entirely: the pagination cursor relies on chronological
        // ordering, and "no date" doesn't have a meaningful position
        // relative to a real timestamp.
        sql.append(" WHERE 1=1 AND a.published IS NOT NULL ");
        if (whereClause != null) {
            sql.append(" AND ").append(whereClause);
        }

        // Add Pagination. The cursor is (published, id) and id is a
        // bigint on the database - JDBC's setObject picks up the Long
        // argument and binds it as BIGINT.
        if (pagId != null && pagDate != null) {
            sql.append(WHERE_PAGINATION);
            params.add(java.time.Instant.parse(pagDate));
            params.add(java.time.Instant.parse(pagDate));
            params.add(pagId);
        }

        sql.append(ORDER_LIMIT);

        return super.query(sql.toString(), articleMapper, params);
    }
}
