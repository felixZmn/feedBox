package de._0x2b.services;

import de._0x2b.models.Article;
import de._0x2b.repositories.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ArticleService {
    private static final Logger logger = LoggerFactory.getLogger(ArticleService.class);
    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository repository) {
        this.articleRepository = repository;
    }

    /**
     * Get all articles with pagination
     * @param paginationId 
     * @param paginationPublished
     * @return
     */
    public List<Article> getAll(int paginationId, String paginationPublished) {
        logger.debug("getAll");
        if (paginationId == -1 && "".equals(paginationPublished)) {
            return articleRepository.findAll();

        }
        return articleRepository.findAll(paginationId, paginationPublished);
    }

    /**
     * Get articles by folder with pagination
     * @param paginationId
     * @param paginationPublished
     * @param folderId
     * @return
     */
    public List<Article> findByFolder(int paginationId, String paginationPublished, int folderId) {
        logger.debug("findByFolder");
        if (paginationId == -1 && "".equals(paginationPublished)) {
            return articleRepository.findByFolder(folderId);
        }
        return articleRepository.findByFolder(folderId, paginationId, paginationPublished);
    }

    /**
     * Get articles by feed with pagination
     * @param paginationId
     * @param paginationPublished
     * @param feedId
     * @return
     */
    public List<Article> findByFeed(int paginationId, String paginationPublished, int feedId) {
        logger.debug("findByFeed");
        if (paginationId == -1 && "".equals(paginationPublished)) {
            return articleRepository.findByFeed(feedId);
        }
        return articleRepository.findByFeed(feedId, paginationId, paginationPublished);
    }
}
