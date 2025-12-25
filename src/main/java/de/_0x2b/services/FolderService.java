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

    /**
     * Get all folders
     * 
     * @return
     */
    public List<Folder> findAll() {
        logger.debug("findAll");
        return folderRepository.findAll();
    }

    /**
     * Search folders by name
     * 
     * @param name
     * @return
     */
    public List<Folder> findByName(String name) {
        logger.debug("findByName");
        return folderRepository.findByName(name);
    }

    /**
     * Create a new folder
     * 
     * @param folder
     * @return
     */
    public int create(Folder folder) {
        logger.debug("create");
        return folderRepository.create(folder);
    }

    /**
     * Update an existing folder
     * 
     * @param folder
     * @return
     */
    public int update(Folder folder) {
        logger.debug("update");
        return folderRepository.update(folder);
    }

    /**
     * Delete a folder and its feeds by folder ID
     * 
     * @param folderId
     * @return
     */
    public int delete(int folderId) {
        logger.debug("delete");
        try {
            return folderRepository.delete(folderId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
