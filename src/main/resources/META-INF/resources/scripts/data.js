"use strict";

import { fetchWithAuth } from "./pkce.js";

export const FOLDER_TREE_CACHE_KEY = "folder-tree-cache";
export const FOLDER_STATE_KEY = "folder-state";
export const FOLDER_TREE_CACHE_VERSION = 1;

class DataService {
  constructor() {
    this.articleCache = [];
  }

  async getFolders() {
    return this._request("./api/folder");
  }
  async getFeeds() {
    return this._request("./api/feed");
  }
  async createFolder(folder) {
    return this._request("./api/folder", { method: "POST", body: folder });
  }
  async updateFolder(folder) {
    return this._request(`./api/folder/${folder.id}`, {
      method: "PUT",
      body: folder,
    });
  }
  async deleteFolder(folderId) {
    return this._request(`./api/folder/${folderId}`, { method: "DELETE" });
  }

  async createFeed(feed) {
    return this._request("./api/feed", { method: "POST", body: feed });
  }
  async updateFeed(feed) {
    return this._request(`./api/feed/${feed.id}`, {
      method: "PUT",
      body: feed,
    });
  }
  async deleteFeed(feedId) {
    return this._request(`./api/feed/${feedId}`, { method: "DELETE" });
  }

  async checkFeed(feedUrl) {
    return this._request(`./api/feed/check?url=${encodeURIComponent(feedUrl)}`);
  }
  async refreshFeeds() {
    return this._request("./api/feed/refresh", { method: "POST" });
  }

  getArticles() {
    return [...this.articleCache];
  }
  clearArticles() {
    this.articleCache = [];
  }

  async loadArticles(params) {
    const queryString = new URLSearchParams(params).toString();
    const data = await this._request(`./api/article?${queryString}`);
    if (Array.isArray(data)) {
      this.articleCache.push(...data);
    }
    return data;
  }

  /**
   * Unified request handler.
   * Delegates network execution to pkce.js for automatic token refresh.
   */
  async _request(url, options = {}) {
    const {
      method = "GET",
      body,
      headers: extraHeaders = {},
      signal,
      ...restOptions
    } = options;

    // Only set Content-Type if sending a body. Auth headers are injected by fetchWithAuth.
    const headers = { ...extraHeaders };
    if (body) headers["Content-Type"] = "application/json";

    const fetchOptions = {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal,
      ...restOptions,
    };

    try {
      const response = await fetchWithAuth(url, fetchOptions);

      if (response.status === 204) return null;

      if (!response.ok) {
        const errorText = await response.text().catch(() => "");
        const error = new Error(
          `HTTP ${response.status}: ${errorText || response.statusText}`,
        );
        error.status = response.status;
        error.serverMessage = errorText;
        throw error;
      }

      const text = await response.text();
      return text ? JSON.parse(text) : null;
    } catch (error) {
      if (error.name === "AbortError") throw error;

      const wrapped = new Error(
        `Failed to ${method} ${url}: ${error.message}`,
        { cause: error },
      );
      if (error.status != null) wrapped.status = error.status;
      throw wrapped;
    }
  }
}

/**
 * Normalize a feed to stable UI fields (excludes volatile lastRefreshedAt).
 * @param {Feed} feed
 */
export function normalizeFeed(feed) {
  return {
    id: feed.id,
    folderId: feed.folderId ?? null,
    name: feed.name ?? "",
    url: feed.url ?? null,
    feedUrl: feed.feedUrl ?? "",
    lastError: feed.lastError ?? null,
  };
}

/**
 * Normalize a folder with its nested feeds.
 * @param {Folder} folder
 */
export function normalizeFolder(folder) {
  return {
    id: folder.id,
    name: folder.name ?? "",
    color: folder.color ?? "f-base",
    feeds: (folder.feeds ?? []).map(normalizeFeed),
  };
}

/**
 * Build and normalize the folder tree from API payloads.
 * @param {Folder[]} folders
 * @param {Feed[]} feeds
 */
export function buildFolderTree(folders, feeds) {
  const foldersWithFeeds = (folders ?? []).map((folder) =>
    normalizeFolder({
      ...folder,
      feeds: (feeds ?? [])
        .filter((f) => f.folderId === folder.id)
        .sort((a, b) => a.name.localeCompare(b.name)),
    }),
  );
  const unfiledFeeds = (feeds ?? [])
    .filter((f) => f.folderId == null)
    .sort((a, b) => a.name.localeCompare(b.name))
    .map(normalizeFeed);
  return { folders: foldersWithFeeds, unfiledFeeds };
}

/**
 * Stable fingerprint for equality checks (ignores HTML cache payload).
 * @param {{ folders?: Folder[], unfiledFeeds?: Feed[] }} tree
 */
export function folderTreeFingerprint(tree) {
  return JSON.stringify({
    folders: (tree?.folders ?? []).map(normalizeFolder),
    unfiledFeeds: (tree?.unfiledFeeds ?? []).map(normalizeFeed),
  });
}

/**
 * @param {{ folders?: Folder[], unfiledFeeds?: Feed[] }} a
 * @param {{ folders?: Folder[], unfiledFeeds?: Feed[] }} b
 */
export function folderTreesEqual(a, b) {
  return folderTreeFingerprint(a) === folderTreeFingerprint(b);
}

/**
 * @returns {{ version: number, folders: Folder[], unfiledFeeds: Feed[], html: string }|null}
 */
export function loadFolderTreeCache() {
  try {
    const raw = localStorage.getItem(FOLDER_TREE_CACHE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (
      !parsed ||
      parsed.version !== FOLDER_TREE_CACHE_VERSION ||
      !Array.isArray(parsed.folders) ||
      !Array.isArray(parsed.unfiledFeeds) ||
      typeof parsed.html !== "string"
    ) {
      return null;
    }
    return {
      version: parsed.version,
      folders: parsed.folders.map(normalizeFolder),
      unfiledFeeds: parsed.unfiledFeeds.map(normalizeFeed),
      html: parsed.html,
    };
  } catch (err) {
    console.warn("Could not load folder tree cache", err);
    return null;
  }
}

/**
 * @param {Folder[]} folders
 * @param {Feed[]} unfiledFeeds
 * @param {string} html
 */
export function saveFolderTreeCache(folders, unfiledFeeds, html) {
  try {
    localStorage.setItem(
      FOLDER_TREE_CACHE_KEY,
      JSON.stringify({
        version: FOLDER_TREE_CACHE_VERSION,
        folders: folders.map(normalizeFolder),
        unfiledFeeds: unfiledFeeds.map(normalizeFeed),
        html,
      }),
    );
  } catch (err) {
    console.warn("Could not save folder tree cache", err);
  }
}

export function clearFolderTreeCache() {
  try {
    localStorage.removeItem(FOLDER_TREE_CACHE_KEY);
    localStorage.removeItem(FOLDER_STATE_KEY);
  } catch (err) {
    console.warn("Could not clear folder tree cache", err);
  }
}

/**
 * Returning session with a usable access or refresh token.
 * Used by the inline boot injector (keep key names in sync with pkce.js).
 */
export function hasReturningSession() {
  try {
    const access = sessionStorage.getItem("access_token");
    const expiresAt = Number(sessionStorage.getItem("expires_at")) || 0;
    if (access && expiresAt > Date.now()) return true;
    return !!localStorage.getItem("refresh_token");
  } catch {
    return false;
  }
}

export const dataService = new DataService();
