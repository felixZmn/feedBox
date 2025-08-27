package de._0x2b.services;

import java.util.List;

import de._0x2b.models.Folder;
import de._0x2b.repositories.FolderRepository;

public class FolderService {
    private final FolderRepository folderRepository = new FolderRepository();

    public List<Folder> getAll() {
        return folderRepository.findAll();
    }

    public int insertOne(Folder folder) {
        return folderRepository.save(folder);
    }

}
