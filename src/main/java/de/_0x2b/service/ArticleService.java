package de._0x2b.service;

import de._0x2b.exception.DataAccessException;
import de._0x2b.exception.NotFoundException;
import de._0x2b.model.Article;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.resource.dto.ArticleSummary;
import de._0x2b.resource.dto.PaginationCursor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.SQLException;
import java.util.List;

@ApplicationScoped
public class ArticleService {

    @Inject
    ArticleRepository articleRepository;

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
     * Save an article for later. Does not change read state. Already saved is a no-op.
     *
     * @throws NotFoundException when the article does not exist
     */
    public void save(long id) {
        try {
            int updated = articleRepository.save(id);
            if (updated == 0 && !articleRepository.exists(id)) {
                throw new NotFoundException("Article not found: " + id);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save article: " + id, e);
        }
    }

    /**
     * Remove the saved flag. Does not change read state. Already unsaved is a no-op.
     *
     * @throws NotFoundException when the article does not exist
     */
    public void unsave(long id) {
        try {
            int updated = articleRepository.unsave(id);
            if (updated == 0 && !articleRepository.exists(id)) {
                throw new NotFoundException("Article not found: " + id);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to unsave article: " + id, e);
        }
    }

    public int countSaved() {
        return articleRepository.countSaved();
    }

    /**
     * Advance the global read horizon so every existing article counts as read.
     * New inserts (higher ids) stay unread.
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

    /** Dated articles that are not effectively read. */
    public List<ArticleSummary> findUnread(PaginationCursor cursor, String q) {
        if (cursor == null || cursor.isEmpty()) {
            return articleRepository.findUnread(q);
        }
        return articleRepository.findUnread(cursor.id(), cursor.published(), q);
    }

    /** Dated saved articles, read or unread. */
    public List<ArticleSummary> findSaved(PaginationCursor cursor, String q) {
        if (cursor == null || cursor.isEmpty()) {
            return articleRepository.findSaved(q);
        }
        return articleRepository.findSaved(cursor.id(), cursor.published(), q);
    }
}
