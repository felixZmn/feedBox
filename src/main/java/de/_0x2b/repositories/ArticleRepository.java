package de._0x2b.repositories;

import de._0x2b.database.Database;
import de._0x2b.models.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    private static final String WHERE_PAGINATION = " AND ((a.published < ?) OR (a.published = ? AND a.id < ?)) ";
    private static final String ORDER_LIMIT = " ORDER BY a.published DESC, a.id DESC LIMIT 25";
    private static final String INSERT_SQL = """
            INSERT INTO article (feed_id, title, description, content, link, published, authors, image_url, categories)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (link) DO NOTHING
            """;
    private static final String DELETE_BY_FEED_SQL = "DELETE FROM article WHERE feed_id = ?";

    private final RowMapper<Article> articleMapper = rs -> new Article(rs.getInt("id"), rs.getInt("feed_id"), rs.getString("publisher"), rs.getString("title"), rs.getString("description"), rs.getString("content"), rs.getString("link"), rs.getString("published"), rs.getString("authors"), rs.getString("image_url"), rs.getString("categories"));

    public List<Article> findAll() {
        return findInternal(null, null, null, null);
    }

    public List<Article> findAll(int paginationId, String paginationDate) {
        return findInternal(null, null, paginationId, paginationDate);
    }

    public List<Article> findByFeed(int feedId) {
        return findInternal("f.id = ?", List.of(feedId), null, null);
    }

    public List<Article> findByFeed(int feedId, int paginationId, String paginationDate) {
        return findInternal("f.id = ?", List.of(feedId), paginationId, paginationDate);
    }

    public List<Article> findByFolder(int folderId) {
        return findInternal("fo.id = ?", List.of(folderId), null, null, true);
    }

    public List<Article> findByFolder(int folderId, int paginationId, String paginationDate) {
        return findInternal("fo.id = ?", List.of(folderId), paginationId, paginationDate, true);
    }

    public void deleteByFeed(int feedId) throws SQLException {
        super.update(DELETE_BY_FEED_SQL, List.of(feedId));
    }

    public void create(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return;

        logger.debug("Starting batch create for {} articles", articles.size());

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); // Start Transaction

            try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
                int count = 0;
                for (Article a : articles) {
                    stmt.setInt(1, a.getFeedId());
                    stmt.setString(2, a.getTitle());
                    stmt.setString(3, a.getDescription());
                    stmt.setString(4, a.getContent());
                    stmt.setString(5, a.getLink());
                    stmt.setString(6, a.getPublished());
                    stmt.setString(7, a.getAuthors());
                    stmt.setString(8, a.getImageUrl());
                    stmt.setString(9, a.getCategories());
                    stmt.addBatch();

                    if (++count % 500 == 0) stmt.executeBatch();
                }
                stmt.executeBatch(); // Execute remaining
                conn.commit(); // Commit Transaction
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Batch insert failed, rolled back", e);
            }
        } catch (SQLException e) {
            logger.error("Database connection error", e);
        }
    }

    private List<Article> findInternal(String whereClause, List<Object> params, Integer pagId, String pagDate) {
        return findInternal(whereClause, params, pagId, pagDate, false);
    }

    private List<Article> findInternal(String whereClause, List<Object> initialParams, Integer pagId, String pagDate, boolean joinFolder) {

        StringBuilder sql = new StringBuilder(SELECT_COLS).append(FROM_JOIN);
        List<Object> params = new ArrayList<>();

        if (initialParams != null) {
            params.addAll(initialParams);
        }

        if (joinFolder) {
            sql.append(JOIN_FOLDER);
        }

        // Add where
        sql.append(" WHERE 1=1 ");
        if (whereClause != null) {
            sql.append(" AND ").append(whereClause);
        }

        // Add Pagination
        if (pagId != null && pagDate != null) {
            sql.append(WHERE_PAGINATION);
            params.add(pagDate);
            params.add(pagDate);
            params.add(pagId);
        }

        sql.append(ORDER_LIMIT);

        return super.query(sql.toString(), articleMapper, params);
    }
}
