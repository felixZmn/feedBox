package de._0x2b.service;

import de._0x2b.exception.DataAccessException;
import de._0x2b.model.Article;
import de._0x2b.repository.ArticleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

@ApplicationScoped
public class ArticleService {
    private static final Logger logger = LoggerFactory.getLogger(ArticleService.class);

    @Inject
    ArticleRepository articleRepository;

    /**
     * @param id article primary key
     * @return the article, or null when missing
     */
    public Article findById(long id) {
        logger.debug("findById {}", id);
        return articleRepository.findById(id);
    }

    /**
     * Mark an article as read. Already-read articles are a no-op.
     *
     * @param id article primary key
     * @return false when the article does not exist
     */
    public boolean markRead(long id) {
        logger.debug("markRead {}", id);
        if (articleRepository.findById(id) == null) {
            return false;
        }
        try {
            articleRepository.markRead(id);
            return true;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark article read: " + id, e);
        }
    }

    /**
     * Mark every article as read.
     */
    public void markAllRead() {
        logger.debug("markAllRead");
        try {
            articleRepository.markAllRead();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark all articles read", e);
        }
    }

    /**
     * Get all articles with pagination and optional search.
     *
     * @param paginationId
     * @param paginationPublished
     * @param q                   search query (blank = no filter)
     * @return
     */
    public List<Article> getAll(long paginationId, String paginationPublished, String q) {
        logger.debug("getAll");
        if (paginationId == -1L && "".equals(paginationPublished)) {
            return articleRepository.findAll(q);
        }
        return articleRepository.findAll(paginationId, paginationPublished, q);
    }

    /**
     * Get articles by folder with pagination and optional search.
     *
     * @param paginationId
     * @param paginationPublished
     * @param folderId
     * @param q                   search query (blank = no filter)
     * @return
     */
    public List<Article> findByFolder(long paginationId, String paginationPublished, int folderId, String q) {
        logger.debug("findByFolder");
        if (paginationId == -1L && "".equals(paginationPublished)) {
            return articleRepository.findByFolder(folderId, q);
        }
        return articleRepository.findByFolder(folderId, paginationId, paginationPublished, q);
    }

    /**
     * Get articles by feed with pagination and optional search.
     *
     * @param paginationId
     * @param paginationPublished
     * @param feedId
     * @param q                   search query (blank = no filter)
     * @return
     */
    public List<Article> findByFeed(long paginationId, String paginationPublished, int feedId, String q) {
        logger.debug("findByFeed");
        if (paginationId == -1L && "".equals(paginationPublished)) {
            return articleRepository.findByFeed(feedId, q);
        }
        return articleRepository.findByFeed(feedId, paginationId, paginationPublished, q);
    }
}
