package de._0x2b.service;

import de._0x2b.model.Folder;
import de._0x2b.repository.FolderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock
    FolderRepository folderRepository;

    @InjectMocks
    FolderService sut;

    @Test
    void findAll_delegatesToRepository() {
        List<Folder> expected = List.of(
                new Folder(1, "Tech", List.of(), ""),
                new Folder(2, "News", List.of(), "")
        );
        when(folderRepository.findAll()).thenReturn(expected);

        List<Folder> result = sut.findAll();

        assertSame(expected, result);
        verify(folderRepository).findAll();
        verifyNoMoreInteractions(folderRepository);
    }

    @Test
    void findByName_delegatesToRepository() {
        String name = "Tech";
        List<Folder> expected = List.of(new Folder(1, "Tech", List.of(), ""));
        when(folderRepository.findByName(name)).thenReturn(expected);

        List<Folder> result = sut.findByName(name);

        assertSame(expected, result);
        verify(folderRepository).findByName(name);
        verifyNoMoreInteractions(folderRepository);
    }

    @Test
    void create_delegatesToRepository() {
        Folder folder = new Folder(-1, "New Folder", List.of(), "");
        when(folderRepository.create(folder)).thenReturn(123);

        int id = sut.create(folder);

        assertEquals(123, id);
        verify(folderRepository).create(folder);
        verifyNoMoreInteractions(folderRepository);
    }

    @Test
    void update_delegatesToRepository() {
        Folder folder = new Folder(10, "Renamed", List.of(), "");
        when(folderRepository.update(folder)).thenReturn(1);

        int updated = sut.update(folder);

        assertEquals(1, updated);
        verify(folderRepository).update(folder);
        verifyNoMoreInteractions(folderRepository);
    }

    @Test
    void delete_whenRepositorySucceeds_returnsResult() throws Exception {
        int folderId = 5;
        when(folderRepository.delete(folderId)).thenReturn(1);

        int result = sut.delete(folderId);

        assertEquals(1, result);
        verify(folderRepository).delete(folderId);
        verifyNoMoreInteractions(folderRepository);
    }

    @Test
    void delete_whenRepositoryThrowsSQLException_wrapsInRuntimeException() throws Exception {
        int folderId = 5;
        SQLException sqlEx = new SQLException("DB error");
        when(folderRepository.delete(folderId)).thenThrow(sqlEx);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> sut.delete(folderId));
        assertSame(sqlEx, ex.getCause());

        verify(folderRepository).delete(folderId);
        verifyNoMoreInteractions(folderRepository);
    }
}
