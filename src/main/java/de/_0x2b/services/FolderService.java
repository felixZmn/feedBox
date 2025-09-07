package de._0x2b.services;

import de._0x2b.models.Folder;
import de._0x2b.repositories.FolderRepository;

import java.util.List;

public class FolderService {
    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }


    public List<Folder> findAll() {
        return folderRepository.findAll();
    }

    public List<Folder> findByName(String name) {
        return folderRepository.findByName(name);
    }

    public int create(Folder folder) {
        return folderRepository.create(folder);
    }

    public int update(Folder folder) {
        return folderRepository.update(folder);
    }

    public int delete(int folderId) {
        return folderRepository.delete(folderId);
    }
}
