package de._0x2b.services;

import java.util.List;

import de._0x2b.models.Article;
import de._0x2b.repositories.ArticleRepository;

public class ArticleService {
    private final ArticleRepository articleRepository = new ArticleRepository();

    public List<Article> getAll(int paginationId, String paginationPublished) {
        if (paginationId == -1 && paginationPublished == "") {
            return articleRepository.findAll();

        }
        return articleRepository.findAll(paginationId, paginationPublished);
    }

    public List<Article> findByFolder(int paginationId, String paginationPublished, int folderId) {
        if (paginationId == -1 && paginationPublished == "") {
            return articleRepository.findByFolder(folderId);
        }
        return articleRepository.findByFolder(folderId, paginationId, paginationPublished);
    }

    public List<Article> findByFeed(int paginationId, String paginationPublished, int feedId) {
        if (paginationId == -1 && paginationPublished == "") {
            return articleRepository.findByFeed(feedId);
        }
        return articleRepository.findByFeed(feedId, paginationId, paginationPublished);
    }
}
