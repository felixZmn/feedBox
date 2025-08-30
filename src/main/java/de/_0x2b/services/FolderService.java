package de._0x2b.services;

import java.util.List;

import de._0x2b.models.Folder;
import de._0x2b.repositories.FolderRepository;

public class FolderService {
    private final FolderRepository folderRepository = new FolderRepository();

    public List<Folder> getAll() {
        return folderRepository.findAll();
    }

    public int create(Folder folder) {
        return folderRepository.save(folder);
    }

    public int saveIgnoreDuplicates(Folder folder) {
        return folderRepository.saveIgnoreDuplicates(folder);
    }

    public int update(Folder folder) {
        return folderRepository.update(folder);
    }

    public int delete(int folderId) {
        return folderRepository.delete(folderId);
    }
}
