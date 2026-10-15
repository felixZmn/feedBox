package de._0x2b.service;

import de._0x2b.exception.NotFoundException;
import de._0x2b.model.Article;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.resource.dto.ArticleSummary;
import de._0x2b.resource.dto.PaginationCursor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    ArticleRepository articleRepository;

    @InjectMocks
    ArticleService service;

    @Test
    void findById_forwardsToRepository() {
        Article expected = mock(Article.class);
        when(articleRepository.findById(42L)).thenReturn(expected);

        Article result = service.findById(42L);

        assertSame(expected, result);
        verify(articleRepository).findById(42L);
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void markRead_whenUpdateAffectsRow_doesNotCheckExists() throws Exception {
        when(articleRepository.markRead(42L)).thenReturn(1);

        service.markRead(42L);

        verify(articleRepository).markRead(42L);
        verify(articleRepository, never()).exists(anyLong());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void markRead_whenAlreadyRead_isNoOp() throws Exception {
        when(articleRepository.markRead(42L)).thenReturn(0);
        when(articleRepository.exists(42L)).thenReturn(true);

        service.markRead(42L);

        verify(articleRepository).markRead(42L);
        verify(articleRepository).exists(42L);
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void markRead_whenMissing_throwsNotFound() throws Exception {
        when(articleRepository.markRead(99L)).thenReturn(0);
        when(articleRepository.exists(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.markRead(99L));

        verify(articleRepository).markRead(99L);
        verify(articleRepository).exists(99L);
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void markAllRead_delegatesToRepository() throws Exception {
        when(articleRepository.markAllRead()).thenReturn(12);

        service.markAllRead();

        verify(articleRepository).markAllRead();
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getAll_noPagination_blankQ_callsRepoFindAll() {
        List<ArticleSummary> expected = List.of(mock(ArticleSummary.class));
        when(articleRepository.findAll("")).thenReturn(expected);

        List<ArticleSummary> result = service.getAll(PaginationCursor.from(null, null), "");

        assertSame(expected, result);
        verify(articleRepository).findAll("");
        verify(articleRepository, never()).findAll(anyLong(), any(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getAll_noPagination_withQ_forwardsQ() {
        List<ArticleSummary> expected = List.of(mock(ArticleSummary.class));
        when(articleRepository.findAll("postgres")).thenReturn(expected);

        List<ArticleSummary> result = service.getAll(new PaginationCursor(null, null), "postgres");

        assertSame(expected, result);
        verify(articleRepository).findAll("postgres");
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getAll_withPagination_callsRepoFindAllWithArgs() {
        Instant published = Instant.parse("2026-01-01T10:00:00Z");
        PaginationCursor cursor = new PaginationCursor(123L, published);

        List<ArticleSummary> expected = List.of(mock(ArticleSummary.class));
        when(articleRepository.findAll(123L, published, "")).thenReturn(expected);

        List<ArticleSummary> result = service.getAll(cursor, "");

        assertSame(expected, result);
        verify(articleRepository).findAll(123L, published, "");
        verify(articleRepository, never()).findAll(anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void getAll_withPagination_withQ_forwardsQ() {
        Instant published = Instant.parse("2026-01-01T10:00:00Z");
        PaginationCursor cursor = new PaginationCursor(123L, published);

        List<ArticleSummary> expected = List.of(mock(ArticleSummary.class));
        when(articleRepository.findAll(123L, published, "index")).thenReturn(expected);

        List<ArticleSummary> result = service.getAll(cursor, "index");

        assertSame(expected, result);
        verify(articleRepository).findAll(123L, published, "index");
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFolder_noPagination_blankQ_callsRepoFindByFolder() {
        int folderId = 7;

        List<ArticleSummary> expected = List.of(mock(ArticleSummary.class));
        when(articleRepository.findByFolder(folderId, "")).thenReturn(expected);

        List<ArticleSummary> result = service.findByFolder(folderId, new PaginationCursor(null, null), "");

        assertSame(expected, result);
        verify(articleRepository).findByFolder(folderId, "");
        verify(articleRepository, never()).findByFolder(anyInt(), anyLong(), any(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFolder_withPagination_withQ_forwardsQ() {
        int folderId = 7;
        Instant published = Instant.parse("2026-01-01T10:00:00Z");
        PaginationCursor cursor = new PaginationCursor(10L, published);

        List<ArticleSummary> expected = List.of(mock(ArticleSummary.class));
        when(articleRepository.findByFolder(folderId, 10L, published, "news")).thenReturn(expected);

        List<ArticleSummary> result = service.findByFolder(folderId, cursor, "news");

        assertSame(expected, result);
        verify(articleRepository).findByFolder(folderId, 10L, published, "news");
        verify(articleRepository, never()).findByFolder(anyInt(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFeed_noPagination_blankQ_callsRepoFindByFeed() {
        int feedId = 3;

        List<ArticleSummary> expected = List.of(mock(ArticleSummary.class));
        when(articleRepository.findByFeed(feedId, "")).thenReturn(expected);

        List<ArticleSummary> result = service.findByFeed(feedId, new PaginationCursor(null, null), "");

        assertSame(expected, result);
        verify(articleRepository).findByFeed(feedId, "");
        verify(articleRepository, never()).findByFeed(anyInt(), anyLong(), any(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }

    @Test
    void findByFeed_withPagination_withQ_forwardsQ() {
        int feedId = 3;
        Instant published = Instant.parse("2026-01-01T10:00:00Z");
        PaginationCursor cursor = new PaginationCursor(10L, published);

        List<ArticleSummary> expected = List.of(mock(ArticleSummary.class));
        when(articleRepository.findByFeed(feedId, 10L, published, "rust")).thenReturn(expected);

        List<ArticleSummary> result = service.findByFeed(feedId, cursor, "rust");

        assertSame(expected, result);
        verify(articleRepository).findByFeed(feedId, 10L, published, "rust");
        verify(articleRepository, never()).findByFeed(anyInt(), anyString());
        verifyNoMoreInteractions(articleRepository);
    }
}
