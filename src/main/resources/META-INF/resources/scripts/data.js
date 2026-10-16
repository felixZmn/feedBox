"use strict";

import { apiFetch } from "./auth.js";

export const FOLDER_TREE_CACHE_KEY = "folder-tree-cache";
export const FOLDER_STATE_KEY = "folder-state";
export const FOLDER_TREE_CACHE_VERSION = 4;

class DataService {
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
  /**
   * Start (or join) an async refresh. Returns a job snapshot (HTTP 202).
   * @returns {Promise<{id: string, type: string, status: string, total: number, completed: number, failed: number, error?: string}>}
   */
  async refreshFeeds() {
    return this._request("./api/feed/refresh", { method: "POST" });
  }

  /**
   * @param {string} jobId
   * @returns {Promise<{id: string, type: string, status: string, total: number, completed: number, failed: number, error?: string}>}
   */
  async getJob(jobId) {
    return this._request(`./api/jobs/${encodeURIComponent(jobId)}`);
  }

  /**
   * Poll until the job reaches COMPLETED or FAILED.
   * @param {string} jobId
   * @param {{ intervalMs?: number, maxIntervalMs?: number, timeoutMs?: number, signal?: AbortSignal }} [options]
   */
  async waitForJob(
    jobId,
    {
      intervalMs = 500,
      maxIntervalMs = 5000,
      timeoutMs = 10 * 60 * 1000,
      signal,
    } = {},
  ) {
    const startedAt = Date.now();
    let delay = intervalMs;
    for (;;) {
      if (signal?.aborted) {
        const err = new Error("Aborted");
        err.name = "AbortError";
        throw err;
      }
      if (Date.now() - startedAt > timeoutMs) {
        const err = new Error(`Job ${jobId} timed out after ${timeoutMs}ms`);
        err.name = "TimeoutError";
        throw err;
      }
      const job = await this.getJob(jobId);
      if (job.status === "COMPLETED" || job.status === "FAILED") {
        return job;
      }
      await new Promise((resolve) => setTimeout(resolve, delay));
      delay = Math.min(maxIntervalMs, Math.floor(delay * 1.5));
    }
  }

  async loadArticles(params, { signal } = {}) {
    const queryString = new URLSearchParams(params).toString();
    return this._request(`./api/article?${queryString}`, { signal });
  }

  /**
   * Full article for the reader (list endpoints omit content/description).
   * @param {number|string} articleId
   * @param {{ signal?: AbortSignal }} [options]
   */
  async getArticle(articleId, { signal } = {}) {
    return this._request(`./api/article/${articleId}`, { signal });
  }

  async markArticleRead(articleId) {
    return this._request(`./api/article/${articleId}/read`, { method: "POST" });
  }

  async markAllArticlesRead() {
    return this._request("./api/article/read", { method: "POST" });
  }

  async markFeedRead(feedId) {
    return this._request(`./api/feed/${feedId}/read`, { method: "POST" });
  }

  async markFolderRead(folderId) {
    return this._request(`./api/folder/${folderId}/read`, { method: "POST" });
  }

  /**
   * Unified request handler.
   * Uses same-origin session cookie via auth.js.
   */
  async _request(url, options = {}) {
    const {
      method = "GET",
      body,
      headers: extraHeaders = {},
      signal,
      ...restOptions
    } = options;

    // Only set Content-Type if sending a body. Session cookie is sent automatically.
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
      const response = await apiFetch(url, fetchOptions);

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
    unreadCount: Number(feed.unreadCount) || 0,
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
 * @returns {{ version: number, folders: Folder[], unfiledFeeds: Feed[] }|null}
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
      !Array.isArray(parsed.unfiledFeeds)
    ) {
      return null;
    }
    return {
      version: parsed.version,
      folders: parsed.folders.map(normalizeFolder),
      unfiledFeeds: parsed.unfiledFeeds.map(normalizeFeed),
    };
  } catch (err) {
    console.warn("Could not load folder tree cache", err);
    return null;
  }
}

/**
 * Persist folder tree JSON only (never rendered HTML — avoids stored XSS).
 * @param {Folder[]} folders
 * @param {Feed[]} unfiledFeeds
 */
export function saveFolderTreeCache(folders, unfiledFeeds) {
  try {
    localStorage.setItem(
      FOLDER_TREE_CACHE_KEY,
      JSON.stringify({
        version: FOLDER_TREE_CACHE_VERSION,
        folders: folders.map(normalizeFolder),
        unfiledFeeds: unfiledFeeds.map(normalizeFeed),
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

export const dataService = new DataService();
