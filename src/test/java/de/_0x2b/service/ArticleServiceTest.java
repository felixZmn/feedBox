package de._0x2b.service;

import de._0x2b.model.Article;
import de._0x2b.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @InjectMocks
    ArticleService service;

    @Test
    void getAll_noPagination_callsRepoFindAll() {
        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findAll()).thenReturn(expected);

        List<Article> result = service.getAll(-1, "");

        assertSame(expected, result);
        verify(articleRepository).findAll();
        verify(articleRepository, never()).findAll(anyInt(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getAll_withPagination_callsRepoFindAllWithArgs() {
        int pagId = 123;
        String pagPublished = "2026-01-01T10:00:00Z";

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findAll(pagId, pagPublished)).thenReturn(expected);

        List<Article> result = service.getAll(pagId, pagPublished);

        assertSame(expected, result);
        verify(articleRepository).findAll(pagId, pagPublished);
        verify(articleRepository, never()).findAll();
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFolder_noPagination_callsRepoFindByFolder() {
        int folderId = 7;

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findByFolder(folderId)).thenReturn(expected);

        List<Article> result = service.findByFolder(-1, "", folderId);

        assertSame(expected, result);
        verify(articleRepository).findByFolder(folderId);
        verify(articleRepository, never()).findByFolder(anyInt(), anyInt(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFolder_withPagination_callsRepoFindByFolderWithArgs() {
        int folderId = 7;
        int pagId = 10;
        String pagPublished = "2026-01-01T10:00:00Z";

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findByFolder(folderId, pagId, pagPublished)).thenReturn(expected);

        List<Article> result = service.findByFolder(pagId, pagPublished, folderId);

        assertSame(expected, result);
        verify(articleRepository).findByFolder(folderId, pagId, pagPublished);
        verify(articleRepository, never()).findByFolder(folderId);
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFeed_noPagination_callsRepoFindByFeed() {
        int feedId = 3;

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findByFeed(feedId)).thenReturn(expected);

        List<Article> result = service.findByFeed(-1, "", feedId);

        assertSame(expected, result);
        verify(articleRepository).findByFeed(feedId);
        verify(articleRepository, never()).findByFeed(anyInt(), anyInt(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFeed_withPagination_callsRepoFindByFeedWithArgs() {
        int feedId = 3;
        int pagId = 10;
        String pagPublished = "2026-01-01T10:00:00Z";

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findByFeed(feedId, pagId, pagPublished)).thenReturn(expected);

        List<Article> result = service.findByFeed(pagId, pagPublished, feedId);

        assertSame(expected, result);
        verify(articleRepository).findByFeed(feedId, pagId, pagPublished);
        verify(articleRepository, never()).findByFeed(feedId);
        verifyNoMoreInteractions(articleRepository);
    }
}