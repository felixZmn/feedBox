package de._0x2b.service;

import de._0x2b.exception.DataAccessException;
import de._0x2b.exception.NotFoundException;
import de._0x2b.model.Article;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.resource.dto.ArticleSummary;
import de._0x2b.resource.dto.PaginationCursor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
public class ArticleService {
    private static final Logger logger = LoggerFactory.getLogger(ArticleService.class);

    @Inject
    ArticleRepository articleRepository;

    @ConfigProperty(name = "articles.retention.days", defaultValue = "90")
    int retentionDays;

    /**
     * @param id article primary key
     * @return the full article, or null when missing
     */
    public Article findById(long id) {
        return articleRepository.findById(id);
    }

    /**
     * Mark an article as read. Already-read articles are a no-op.
     *
     * @param id article primary key
     * @throws NotFoundException when the article does not exist
     */
    public void markRead(long id) {
        try {
            int updated = articleRepository.markRead(id);
            if (updated == 0 && !articleRepository.exists(id)) {
                throw new NotFoundException("Article not found: " + id);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark article read: " + id, e);
        }
    }

    /**
     * Mark every article as read.
     */
    public void markAllRead() {
        try {
            articleRepository.markAllRead();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark all articles read", e);
        }
    }

    /**
     * Get all articles with pagination and optional search (list projection).
     */
    public List<ArticleSummary> getAll(PaginationCursor cursor, String q) {
        if (cursor == null || cursor.isEmpty()) {
            return articleRepository.findAll(q);
        }
        return articleRepository.findAll(cursor.id(), cursor.published(), q);
    }

    /**
     * Get articles by folder with pagination and optional search (list projection).
     */
    public List<ArticleSummary> findByFolder(int folderId, PaginationCursor cursor, String q) {
        if (cursor == null || cursor.isEmpty()) {
            return articleRepository.findByFolder(folderId, q);
        }
        return articleRepository.findByFolder(folderId, cursor.id(), cursor.published(), q);
    }

    /**
     * Get articles by feed with pagination and optional search (list projection).
     */
    public List<ArticleSummary> findByFeed(int feedId, PaginationCursor cursor, String q) {
        if (cursor == null || cursor.isEmpty()) {
            return articleRepository.findByFeed(feedId, q);
        }
        return articleRepository.findByFeed(feedId, cursor.id(), cursor.published(), q);
    }

    /**
     * Delete read articles older than {@code articles.retention.days}. Unread
     * articles are never removed.
     *
     * @return number of articles deleted
     */
    public int purgeExpiredReadArticles() {
        if (retentionDays <= 0) {
            return 0;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        try {
            int deleted = articleRepository.deleteReadOlderThan(cutoff);
            if (deleted > 0) {
                logger.info("Purged {} read articles older than {} days", deleted, retentionDays);
            }
            return deleted;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to purge expired read articles", e);
        }
    }
}
