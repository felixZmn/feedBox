package de._0x2b.service;

import de._0x2b.exception.DataAccessException;
import de._0x2b.exception.NotFoundException;
import de._0x2b.model.Folder;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.repository.FolderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.sql.SQLException;
import java.util.List;

@ApplicationScoped
public class FolderService {

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
        return folderRepository.findAll();
    }

    /**
     * Mark every article in feeds belonging to a folder as read.
     *
     * @param folderId folder primary key
     * @throws NotFoundException when the folder does not exist
     */
    public void markRead(int folderId) {
        try {
            int updated = articleRepository.markReadByFolder(folderId);
            if (updated == 0 && !folderRepository.exists(folderId)) {
                throw new NotFoundException("Folder not found: " + folderId);
            }
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
        int rows = folderRepository.delete(folderId);
        if (rows == 0) {
            throw new NotFoundException("Folder not found: " + folderId);
        }
    }
}
