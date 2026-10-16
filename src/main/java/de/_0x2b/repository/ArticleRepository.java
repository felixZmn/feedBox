package de._0x2b.repository;

import de._0x2b.exception.DataAccessException;
import de._0x2b.model.Article;
import de._0x2b.resource.dto.ArticleSummary;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@ApplicationScoped
public class ArticleRepository extends AbstractRepository {
    /** Effective read: column, global horizon, or per-feed horizon. */
    private static final String EFFECTIVE_READ = """
            (a.read OR a.id <= rs.mark_read_through_id OR a.id <= f.mark_read_through_id)
            """;
    /** Full row for the reader ({@code GET /api/article/{id}}). */
    private static final String SELECT_DETAIL_COLS = """
            SELECT a.id, a.feed_id, f.name AS publisher, a.title, a.description,
                   a.content, a.link, a.guid, a.published, a.authors, a.image_url, a.categories,
                   """ + EFFECTIVE_READ + """
                   AS read
            """;
    /** List/pagination projection — no HTML bodies. */
    private static final String SELECT_LIST_COLS = """
            SELECT a.id, a.feed_id, f.name AS publisher, a.title,
                   a.link, a.published, a.authors, a.image_url,
                   """ + EFFECTIVE_READ + """
                   AS read
            """;
    private static final String FROM_JOIN = """
            FROM article a
            JOIN feed f ON f.id = a.feed_id
            CROSS JOIN reader_state rs
            """;
    private static final String JOIN_FOLDER = " JOIN folder fo on fo.id = f.folder_id ";
    // The pagination cursor is now a real TIMESTAMPTZ. Articles with NULL
    // pubDate are excluded from the read query entirely (see WHERE clause
    // below) so we don't need NULLS FIRST/LAST gymnastics here.
    private static final String WHERE_PAGINATION = " AND ((a.published < ?) OR (a.published = ? AND a.id < ?)) ";
    private static final String ORDER_LIMIT = " ORDER BY a.published DESC, a.id DESC LIMIT 25";
    private static final String SEARCH_TOKEN_CLAUSE =
            " AND (a.title ILIKE ? ESCAPE '\\' OR a.description ILIKE ? ESCAPE '\\') ";
    private static final String INSERT_SQL = """
            INSERT INTO article (feed_id, title, description, content, link, guid, published, authors, image_url, categories)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (feed_id, guid) DO UPDATE SET
                title = EXCLUDED.title,
                description = EXCLUDED.description,
                content = EXCLUDED.content,
                link = EXCLUDED.link,
                published = EXCLUDED.published,
                authors = EXCLUDED.authors,
                image_url = EXCLUDED.image_url,
                categories = EXCLUDED.categories
            """;
    private static final String MARK_READ_SQL = "UPDATE article SET read = true WHERE id = ? AND read = false";
    private static final String MARK_READ_ALL_SQL = """
            UPDATE reader_state
            SET mark_read_through_id = GREATEST(
                mark_read_through_id,
                COALESCE((SELECT MAX(id) FROM article), 0))
            WHERE id = 1
            """;
    private static final String MARK_READ_BY_FEED_SQL = """
            UPDATE feed
            SET mark_read_through_id = GREATEST(
                mark_read_through_id,
                COALESCE((SELECT MAX(id) FROM article WHERE feed_id = feed.id), 0))
            WHERE id = ?
            """;
    private static final String MARK_READ_BY_FOLDER_SQL = """
            UPDATE feed
            SET mark_read_through_id = GREATEST(
                mark_read_through_id,
                COALESCE((SELECT MAX(id) FROM article WHERE feed_id = feed.id), 0))
            WHERE folder_id = ?
            """;
    private static final String COUNT_UNREAD_BY_FEED_SQL = """
            SELECT a.feed_id, COUNT(*)::int AS unread_count
            FROM article a
            JOIN feed f ON f.id = a.feed_id
            CROSS JOIN reader_state rs
            WHERE a.read = false
              AND a.published IS NOT NULL
              AND a.id > rs.mark_read_through_id
              AND a.id > f.mark_read_through_id
            GROUP BY a.feed_id
            """;
    // The PostgreSQL JDBC driver does NOT support
    // ResultSet.getObject(int, Class) for java.time.Instant on a
    // timestamptz column - it raises
    // "conversion to class java.time.Instant from timestamptz not
    // supported". The supported path is getTimestamp(idx, Calendar)
    // with a UTC calendar (so the driver doesn't re-project the stored
    // UTC value into the JVM's default time zone), then
    // Timestamp.toInstant().
    private static final Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private static final String EXISTS_BY_ID_SQL = "SELECT 1 FROM article WHERE id = ?";

    private final RowMapper<Article> articleMapper = rs -> new Article(rs.getLong("id"), rs.getInt("feed_id"),
            rs.getString("publisher"), rs.getString("title"), rs.getString("description"), rs.getString("content"),
            rs.getString("link"), rs.getString("guid"), readPublished(rs), rs.getString("authors"),
            rs.getString("image_url"), rs.getString("categories"), rs.getBoolean("read"));

    private final RowMapper<ArticleSummary> summaryMapper = rs -> new ArticleSummary(
            rs.getLong("id"), rs.getInt("feed_id"), rs.getString("publisher"), rs.getString("title"),
            rs.getString("link"), readPublished(rs), rs.getString("authors"), rs.getString("image_url"),
            rs.getBoolean("read"));

    private static Instant readPublished(java.sql.ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("published", UTC);
        return ts == null ? null : ts.toInstant();
    }

    public ArticleRepository() {
    }

    public Article findById(long id) {
        String sql = SELECT_DETAIL_COLS + FROM_JOIN + " WHERE a.id = ?";
        List<Article> results = super.query(sql, articleMapper, List.of(id));
        return results.isEmpty() ? null : results.getFirst();
    }

    public boolean exists(long id) {
        return super.exists(EXISTS_BY_ID_SQL, List.of(id));
    }

    public List<ArticleSummary> findAll(String q) {
        return findInternal(null, null, null, null, false, q);
    }

    public List<ArticleSummary> findAll(long paginationId, Instant paginationPublished, String q) {
        return findInternal(null, null, paginationId, paginationPublished, false, q);
    }

    public List<ArticleSummary> findByFeed(int feedId, String q) {
        return findInternal("f.id = ?", List.of(feedId), null, null, false, q);
    }

    public List<ArticleSummary> findByFeed(int feedId, long paginationId, Instant paginationPublished, String q) {
        return findInternal("f.id = ?", List.of(feedId), paginationId, paginationPublished, false, q);
    }

    public List<ArticleSummary> findByFolder(int folderId, String q) {
        return findInternal("fo.id = ?", List.of(folderId), null, null, true, q);
    }

    public List<ArticleSummary> findByFolder(int folderId, long paginationId, Instant paginationPublished, String q) {
        return findInternal("fo.id = ?", List.of(folderId), paginationId, paginationPublished, true, q);
    }

    /**
     * Mark a single article as read.
     *
     * @return number of rows updated (0 if already read or missing)
     */
    public int markRead(long id) throws SQLException {
        return super.update(MARK_READ_SQL, List.of(id));
    }

    /**
     * Advance the global read horizon to the current max article id.
     *
     * @return number of reader_state rows updated (0 or 1)
     */
    public int markAllRead() throws SQLException {
        return super.update(MARK_READ_ALL_SQL, List.of());
    }

    /**
     * Advance the feed read horizon to the max article id in that feed.
     *
     * @return number of feed rows updated (0 if the feed id does not exist)
     */
    public int markReadByFeed(int feedId) throws SQLException {
        return super.update(MARK_READ_BY_FEED_SQL, List.of(feedId));
    }

    /**
     * Advance each feed's read horizon for feeds in the folder.
     *
     * @return number of feed rows updated
     */
    public int markReadByFolder(int folderId) throws SQLException {
        return super.update(MARK_READ_BY_FOLDER_SQL, List.of(folderId));
    }

    /**
     * Unread dated-article counts keyed by feed id.
     * Feeds with zero unread are omitted.
     */
    public Map<Integer, Integer> countUnreadByFeed() {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(COUNT_UNREAD_BY_FEED_SQL);
                ResultSet rs = stmt.executeQuery()) {
            Map<Integer, Integer> counts = new HashMap<>();
            while (rs.next()) {
                counts.put(rs.getInt("feed_id"), rs.getInt("unread_count"));
            }
            return counts;
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + COUNT_UNREAD_BY_FEED_SQL, e);
        }
    }

    /**
     * Batch-insert articles. Callers must run inside a JTA {@code @Transactional}
     * boundary — this method does not manage commits itself.
     */
    public void create(List<Article> articles) {
        if (articles == null || articles.isEmpty())
            return;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            int count = 0;
            for (Article a : articles) {
                stmt.setInt(1, a.getFeedId());
                stmt.setString(2, a.getTitle());
                stmt.setString(3, a.getDescription());
                stmt.setString(4, a.getContent());
                if (a.getLink() != null && !a.getLink().isBlank()) {
                    stmt.setString(5, a.getLink());
                } else {
                    stmt.setNull(5, Types.VARCHAR);
                }
                stmt.setString(6, a.getGuid());
                if (a.getPublished() != null) {
                    stmt.setTimestamp(7, Timestamp.from(a.getPublished()));
                } else {
                    stmt.setNull(7, Types.TIMESTAMP_WITH_TIMEZONE);
                }
                stmt.setString(8, a.getAuthors());
                stmt.setString(9, a.getImageUrl());
                stmt.setString(10, a.getCategories());
                stmt.addBatch();

                if (++count % 500 == 0)
                    stmt.executeBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new DataAccessException("Batch insert failed for feed_id=" + articles.getFirst().getFeedId(), e);
        }
    }

    private List<ArticleSummary> findInternal(String whereClause, List<Object> initialParams, Long pagId,
            Instant pagPublished, boolean joinFolder, String q) {
        // NULL pubDate excluded: pagination cursor needs chronological order.
        return queryWhere(
                SELECT_LIST_COLS + FROM_JOIN,
                joinFolder ? JOIN_FOLDER : null,
                "a.published IS NOT NULL",
                whereClause,
                initialParams,
                (sql, params) -> {
                    appendSearch(sql, params, q);
                    if (pagId != null && pagPublished != null) {
                        sql.append(WHERE_PAGINATION);
                        params.add(pagPublished);
                        params.add(pagPublished);
                        params.add(pagId);
                    }
                },
                ORDER_LIMIT.trim(),
                summaryMapper);
    }

    /**
     * Append token-AND search predicates. Each whitespace-separated token must
     * match title or description (case-insensitive substring). Wildcards in the
     * user input are escaped so they cannot widen the pattern.
     */
    private static void appendSearch(StringBuilder sql, List<Object> params, String q) {
        if (q == null || q.isBlank()) {
            return;
        }
        for (String token : q.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            String pattern = "%" + escapeIlike(token) + "%";
            sql.append(SEARCH_TOKEN_CLAUSE);
            params.add(pattern);
            params.add(pattern);
        }
    }

    /** Escape {@code \}, {@code %} and {@code _} for use with {@code ILIKE ... ESCAPE '\'}. */
    static String escapeIlike(String token) {
        return token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
