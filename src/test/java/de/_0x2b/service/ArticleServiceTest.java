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
    void getAll_noPagination_blankQ_callsRepoFindAll() {
        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findAll("")).thenReturn(expected);

        List<Article> result = service.getAll(-1L, "", "");

        assertSame(expected, result);
        verify(articleRepository).findAll("");
        verify(articleRepository, never()).findAll(anyLong(), anyString(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getAll_noPagination_withQ_forwardsQ() {
        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findAll("postgres")).thenReturn(expected);

        List<Article> result = service.getAll(-1L, "", "postgres");

        assertSame(expected, result);
        verify(articleRepository).findAll("postgres");
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getAll_withPagination_callsRepoFindAllWithArgs() {
        long pagId = 123L;
        String pagPublished = "2026-01-01T10:00:00Z";

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findAll(pagId, pagPublished, "")).thenReturn(expected);

        List<Article> result = service.getAll(pagId, pagPublished, "");

        assertSame(expected, result);
        verify(articleRepository).findAll(pagId, pagPublished, "");
        verify(articleRepository, never()).findAll(anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getAll_withPagination_withQ_forwardsQ() {
        long pagId = 123L;
        String pagPublished = "2026-01-01T10:00:00Z";

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findAll(pagId, pagPublished, "index")).thenReturn(expected);

        List<Article> result = service.getAll(pagId, pagPublished, "index");

        assertSame(expected, result);
        verify(articleRepository).findAll(pagId, pagPublished, "index");
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFolder_noPagination_blankQ_callsRepoFindByFolder() {
        int folderId = 7;

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findByFolder(folderId, "")).thenReturn(expected);

        List<Article> result = service.findByFolder(-1L, "", folderId, "");

        assertSame(expected, result);
        verify(articleRepository).findByFolder(folderId, "");
        verify(articleRepository, never()).findByFolder(anyInt(), anyLong(), anyString(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFolder_withPagination_withQ_forwardsQ() {
        int folderId = 7;
        long pagId = 10L;
        String pagPublished = "2026-01-01T10:00:00Z";

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findByFolder(folderId, pagId, pagPublished, "news")).thenReturn(expected);

        List<Article> result = service.findByFolder(pagId, pagPublished, folderId, "news");

        assertSame(expected, result);
        verify(articleRepository).findByFolder(folderId, pagId, pagPublished, "news");
        verify(articleRepository, never()).findByFolder(anyInt(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFeed_noPagination_blankQ_callsRepoFindByFeed() {
        int feedId = 3;

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findByFeed(feedId, "")).thenReturn(expected);

        List<Article> result = service.findByFeed(-1L, "", feedId, "");

        assertSame(expected, result);
        verify(articleRepository).findByFeed(feedId, "");
        verify(articleRepository, never()).findByFeed(anyInt(), anyLong(), anyString(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFeed_withPagination_withQ_forwardsQ() {
        int feedId = 3;
        long pagId = 10L;
        String pagPublished = "2026-01-01T10:00:00Z";

        List<Article> expected = List.of(mock(Article.class));
        when(articleRepository.findByFeed(feedId, pagId, pagPublished, "rust")).thenReturn(expected);

        List<Article> result = service.findByFeed(pagId, pagPublished, feedId, "rust");

        assertSame(expected, result);
        verify(articleRepository).findByFeed(feedId, pagId, pagPublished, "rust");
        verify(articleRepository, never()).findByFeed(anyInt(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }
}
