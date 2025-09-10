package de._0x2b.repositories;

import de._0x2b.database.Database;
import de._0x2b.models.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ArticleRepository {
    private static final Logger logger = LoggerFactory.getLogger(ArticleRepository.class);

    private static final String SELECT_ALL = """
            SELECT article.id, article.feed_id, feed.name AS publisher, article.title, article.description, article.content, article.link, article.published, article.authors, article.image_url, article.categories
            FROM article
            JOIN feed ON feed.id = feed_id
            ORDER BY article.published DESC, article.id DESC
            LIMIT 25
            """;
    private static final String SELECT_ALL_PAGINATED = """
            SELECT article.id, article.feed_id, feed.name AS publisher, article.title, article.description, article.content, article.link, article.published, article.authors, article.image_url, article.categories
            FROM article
            JOIN feed ON feed.id = feed_id
            WHERE ((article.published < ?) OR (article.published = ? AND article.id < ?))
            ORDER BY article.published DESC, article.id DESC
            LIMIT 25
            """;
    private static final String SELECT_BY_FEED = """
            SELECT article.id, article.feed_id, feed.name AS publisher, article.title, article.description, article.content, article.link, article.published, article.authors, article.image_url, article.categories
            FROM article
            JOIN feed ON feed.id = feed_id
            WHERE feed.id = ?
            ORDER BY article.published DESC, article.id DESC
            LIMIT 25
            """;
    private static final String SELECT_BY_FEED_PAGINATED = """
            SELECT article.id, article.feed_id, feed.name AS publisher, article.title, article.description, article.content, article.link, article.published, article.authors, article.image_url, article.categories
            FROM article
            JOIN feed ON feed.id = feed_id
            WHERE feed.id = ?
            and ((article.published < ?) OR (article.published = ? AND article.id < ?))
            ORDER BY article.published DESC, article.id DESC
            LIMIT 25
            """;
    private static final String SELECT_BY_FOLDER = """
            SELECT article.id, article.feed_id, feed.name AS publisher, article.title, article.description, article.content, article.link, article.published, article.authors, article.image_url, article.categories
            FROM article
            JOIN feed ON feed.id = feed_id
            JOIN folder on folder.id = feed.folder_id
            WHERE folder.id = ?
            ORDER BY article.published DESC, article.id DESC
            LIMIT 25
            """;
    private static final String SELECT_BY_FOLDER_PAGINATED = """
            SELECT article.id, article.feed_id, feed.name AS publisher, article.title, article.description, article.content, article.link, article.published, article.authors, article.image_url, article.categories
            FROM article
            JOIN feed ON feed.id = feed_id
            JOIN folder on folder.id = feed.folder_id
            WHERE folder.id = ?
            and ((article.published < ?) OR (article.published = ? AND article.id < ?))
            ORDER BY article.published DESC, article.id DESC
            LIMIT 25
            """;

    private static final String INSERT = """
            INSERT INTO article (feed_id, title, description, content, link, published, authors, image_url, categories) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (link) DO NOTHING
            """;

    public List<Article> findAll() {
        logger.debug("findAll");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public List<Article> findAll(int paginationId, String paginationDate) {
        logger.debug("findAll");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_PAGINATED)) {

            stmt.setString(1, paginationDate);
            stmt.setString(2, paginationDate);
            stmt.setInt(3, paginationId);

            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public List<Article> findByFeed(int feedId) {
        logger.debug("findByFeed");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_FEED)) {

            stmt.setObject(1, feedId);

            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public List<Article> findByFeed(int feedId, int paginationId, String paginationDate) {
        logger.debug("findByFeed");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_FEED_PAGINATED)) {

            stmt.setInt(1, feedId);
            stmt.setString(2, paginationDate);
            stmt.setString(3, paginationDate);
            stmt.setInt(4, paginationId);

            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public List<Article> findByFolder(int folderId) {
        logger.debug("findByFolder");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_FOLDER)) {

            stmt.setInt(1, folderId);

            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public List<Article> findByFolder(int folderId, int paginationId, String paginationDate) {
        logger.debug("findByFolder");
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_FOLDER_PAGINATED)) {

            stmt.setInt(1, folderId);
            stmt.setString(2, paginationDate);
            stmt.setString(3, paginationDate);
            stmt.setInt(4, paginationId);

            try (var rs = stmt.executeQuery()) {
                return parseResult(rs);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
        return List.of();
    }

    public void create(List<Article> articles) {
        logger.debug("create");
        if (articles == null || articles.isEmpty()) {
            return;
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(INSERT)) {
                final int batchSize = 500;
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
                    if (++count % batchSize == 0) {
                        stmt.executeBatch();
                    }
                }
                stmt.executeBatch(); // execute remaining
                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                    logger.error("Error executing SQL statement", e);
                } catch (SQLException ex) {
                    logger.error("Error executing SQL statement", ex);
                }
                logger.error("Error executing SQL statement", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error executing SQL statement", e);
        }
    }

    private List<Article> parseResult(ResultSet rs) throws SQLException {
        logger.debug("parseResult");
        List<Article> articles = new ArrayList<>();
        while (rs.next()) {
            Article a = new Article(
                    rs.getInt("id"),
                    rs.getInt("feed_id"),
                    rs.getString("publisher"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("content"),
                    rs.getString("link"),
                    rs.getString("published"),
                    rs.getString("authors"),
                    rs.getString("image_url"),
                    rs.getString("categories"));
            articles.add(a);
        }
        return articles;
    }
}
