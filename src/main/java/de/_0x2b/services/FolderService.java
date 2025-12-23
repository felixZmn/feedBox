package de._0x2b.services;

import de._0x2b.models.Folder;
import de._0x2b.repositories.FolderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class FolderService {
    private static final Logger logger = LoggerFactory.getLogger(FolderService.class);
    private final FolderRepository folderRepository;

    public FolderService(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }


    public List<Folder> findAll() {
        logger.debug("findAll");
        return folderRepository.findAll();
    }

    public List<Folder> findByName(String name) {
        logger.debug("findByName");
        return folderRepository.findByName(name);
    }

    public int create(Folder folder) {
        logger.debug("create");
        return folderRepository.create(folder);
    }

    public int update(Folder folder) {
        logger.debug("update");
        return folderRepository.update(folder);
    }

    public int delete(int folderId) {
        logger.debug("delete");
        try {
            return folderRepository.delete(folderId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
