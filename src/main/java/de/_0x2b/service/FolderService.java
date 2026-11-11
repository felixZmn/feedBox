package de._0x2b.service;

import de._0x2b.exception.DataAccessException;
import de._0x2b.exception.NotFoundException;
import de._0x2b.model.Folder;
import de._0x2b.repository.ArticleRepository;
import de._0x2b.repository.FeedRepository;
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
    FeedRepository feedRepository;

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
     * Advance the read horizon on every feed in this folder.
     * When {@code throughId} is set, only articles up to that id (capped at
     * each feed's max) are marked.
     *
     * @param folderId folder primary key
     * @throws NotFoundException when the folder does not exist
     */
    public void markRead(int folderId, Long throughId) {
        try {
            int updated = articleRepository.markReadByFolder(folderId, throughId);
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
     * Delete a folder. When {@code keepFeeds} is true (the default for the
     * HTTP API), feeds are moved to "No folder" first so the CASCADE does
     * not remove them. When false, feeds (and their articles) go with the
     * folder.
     *
     * @param folderId folder primary key
     * @param keepFeeds true to unlink feeds; false to delete them
     * @throws NotFoundException when the folder does not exist
     */
    @Transactional
    public void delete(int folderId, boolean keepFeeds) {
        if (keepFeeds) {
            feedRepository.clearFolderId(folderId);
        }
        int rows = folderRepository.delete(folderId);
        if (rows == 0) {
            throw new NotFoundException("Folder not found: " + folderId);
        }
    }
}
