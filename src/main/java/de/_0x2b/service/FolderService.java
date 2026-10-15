package de._0x2b.service;

import de._0x2b.exception.DataAccessException;
import de._0x2b.exception.NotFoundException;
import de._0x2b.model.Folder;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.repository.FolderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

@ApplicationScoped
public class FolderService {
    private static final Logger logger = LoggerFactory.getLogger(FolderService.class);

    @Inject
    FolderRepository folderRepository;

    @Inject
    ArticleRepository articleRepository;

    /**
     * Get all folders
     *
     * @return all folders
     */
    public List<Folder> findAll() {
        logger.debug("findAll");
        return folderRepository.findAll();
    }

    /**
     * Mark every article in feeds belonging to a folder as read.
     *
     * @param folderId folder primary key
     * @throws NotFoundException when the folder does not exist
     */
    public void markRead(int folderId) {
        logger.debug("markRead folder {}", folderId);
        if (folderRepository.findById(folderId) == null) {
            throw new NotFoundException("Folder not found: " + folderId);
        }
        try {
            articleRepository.markReadByFolder(folderId);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark folder read: " + folderId, e);
        }
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
     * @return generated folder id
     */
    @Transactional
    public int create(Folder folder) {
        logger.debug("create");
        return folderRepository.create(folder);
    }

    /**
     * Update an existing folder
     *
     * @param folder
     * @return the folder id
     * @throws NotFoundException when the folder does not exist
     */
    @Transactional
    public int update(Folder folder) {
        logger.debug("update");
        int rows = folderRepository.update(folder);
        if (rows == 0) {
            throw new NotFoundException("Folder not found: " + folder.getId());
        }
        return folder.getId();
    }

    /**
     * Delete a folder and its feeds by folder ID
     *
     * @param folderId
     * @throws NotFoundException when the folder does not exist
     */
    @Transactional
    public void delete(int folderId) {
        logger.debug("delete");
        int rows = folderRepository.delete(folderId);
        if (rows == 0) {
            throw new NotFoundException("Folder not found: " + folderId);
        }
    }
}
