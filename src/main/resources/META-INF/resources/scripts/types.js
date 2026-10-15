"use strict";

/**
 * @typedef {Object} Article
 * @property {number} id - The unique identifier of the article.
 * @property {number} feedId - The unique identifier of the feed the article belongs to.
 * @property {string} feedName - The name of the feed the article belongs to.
 * @property {string} title - The title of the article.
 * @property {string} description - A short HTML description of the article.
 * @property {string} content - The full HTML content of the article.
 * @property {string|null} link - The URL link to the original article (null when missing).
 * @property {string} guid - Per-feed stable identity (RSS guid, else link, else hash).
 * @property {string|null} published - Publication instant (ISO-8601); null when the feed omitted it.
 * @property {string} authors - The author(s) of the article.
 * @property {string} imageUrl - The URL of the article's image (empty string if none).
 * @property {string} categories - JSON array of category strings, e.g. `["news","tech"]`.
 * @property {boolean} [read] - Whether the article has been marked read.
 */

/**
 * Represents a feed source with metadata.
 * @typedef {Object} Feed
 * @property {number} id - Unique identifier for the feed source
 * @property {number} folderId - ID of the folder this feed belongs to
 * @property {string} name - Name of the feed source (may contain special characters)
 * @property {string} url - Primary URL of the feed source
 * @property {string} feedUrl - URL of the RSS/Atom feed
 * @property {string|null} icon - URL of the feed icon, or null if not available
 * @property {string|null} lastRefreshedAt - ISO timestamp of last successful refresh
 * @property {string|null} lastError - Last refresh error message, or null if healthy
 * @property {number} [unreadCount] - Number of unread dated articles in this feed
 */

/**
 * @typedef {Object} Folder
 * @property {number} id - Unique identifier for the folder
 * @property {string} name - Name of the folder
 * @property {string} color - CSS class to influence the folder icon color
 * @property {Feed[]} feeds - Feeds in the folder
 */
