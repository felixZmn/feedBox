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
  /**
   * @param {number} folderId
   * @param {"keep"|"delete"} [feeds="keep"]
   */
  async deleteFolder(folderId, feeds = "keep") {
    const mode = feeds === "delete" ? "delete" : "keep";
    return this._request(
      `./api/folder/${folderId}?feeds=${encodeURIComponent(mode)}`,
      { method: "DELETE" },
    );
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
   * `onProgress` receives every snapshot, the final one included.
   * @param {string} jobId
   * @param {{ intervalMs?: number, maxIntervalMs?: number, timeoutMs?: number, signal?: AbortSignal, onProgress?: (job: {status: string, total: number, completed: number, failed: number}) => void }} [options]
   */
  async waitForJob(
    jobId,
    {
      intervalMs = 500,
      maxIntervalMs = 5000,
      timeoutMs = 10 * 60 * 1000,
      signal,
      onProgress,
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
      onProgress?.(job);
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
   * Count dated articles with id greater than {@code sinceId} in the list scope.
   * @param {{ sinceId: number, feed?: number, folder?: number, view?: string }} args
   * @returns {Promise<{ count: number }>}
   */
  async countArticles({ sinceId, feed, folder, view } = {}) {
    const params = new URLSearchParams();
    params.set("since_id", String(sinceId));
    if (feed != null) params.set("feed", String(feed));
    if (folder != null) params.set("folder", String(folder));
    if (view) params.set("view", String(view));
    return this._request(`./api/article/count?${params}`);
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

  /**
   * @param {number|null|undefined} throughId
   * @param {{ keepalive?: boolean }} [options]
   */
  async markAllArticlesRead(throughId, { keepalive = false } = {}) {
    return this._request("./api/article/read", {
      method: "POST",
      body: markReadBody(throughId),
      keepalive,
    });
  }

  /**
   * @param {number} feedId
   * @param {number|null|undefined} throughId
   * @param {{ keepalive?: boolean }} [options]
   */
  async markFeedRead(feedId, throughId, { keepalive = false } = {}) {
    return this._request(`./api/feed/${feedId}/read`, {
      method: "POST",
      body: markReadBody(throughId),
      keepalive,
    });
  }

  /**
   * @param {number} folderId
   * @param {number|null|undefined} throughId
   * @param {{ keepalive?: boolean }} [options]
   */
  async markFolderRead(folderId, throughId, { keepalive = false } = {}) {
    return this._request(`./api/folder/${folderId}/read`, {
      method: "POST",
      body: markReadBody(throughId),
      keepalive,
    });
  }

  async saveArticle(articleId) {
    return this._request(`./api/article/${articleId}/saved`, { method: "POST" });
  }

  async unsaveArticle(articleId) {
    return this._request(`./api/article/${articleId}/saved`, { method: "DELETE" });
  }

  /**
   * @param {{ feed?: number, folder?: number }} [scope]
   * @returns {Promise<{ count: number }>}
   */
  async loadSavedCount(scope = {}) {
    const params = new URLSearchParams();
    if (scope.feed != null) params.set("feed", String(scope.feed));
    if (scope.folder != null) params.set("folder", String(scope.folder));
    const qs = params.toString();
    return this._request(
      qs ? `./api/article/saved/count?${qs}` : "./api/article/saved/count",
    );
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
        const { code, detail } = parseApiError(errorText);
        const error = new Error(
          `HTTP ${response.status}: ${detail || response.statusText}`,
        );
        error.status = response.status;
        error.code = code;
        error.detail = detail;
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
      if (error.code != null) wrapped.code = error.code;
      if (error.detail != null) wrapped.detail = error.detail;
      throw wrapped;
    }
  }
}

/**
 * Read the server's `{ code, message }` error body. Non-JSON bodies (proxies,
 * older servers) yield no code and the raw text as detail.
 * @param {string} text
 * @returns {{ code: string|null, detail: string|null }}
 */
export function parseApiError(text) {
  if (!text) return { code: null, detail: null };
  try {
    const body = JSON.parse(text);
    if (body && typeof body === "object") {
      return {
        code: typeof body.code === "string" ? body.code : null,
        detail: typeof body.message === "string" ? body.message : null,
      };
    }
  } catch {
    // Not JSON.
  }
  return { code: null, detail: text.length > 200 ? null : text };
}

/**
 * Normalize a feed to the fields the UI uses.
 * @param {Feed} feed
 */
export function normalizeFeed(feed) {
  return {
    id: feed.id,
    folderId: feed.folderId ?? null,
    name: feed.name ?? "",
    url: feed.url ?? null,
    feedUrl: feed.feedUrl ?? "",
    lastRefreshedAt: feed.lastRefreshedAt ?? null,
    lastError: feed.lastError ?? null,
    unreadCount: Number(feed.unreadCount) || 0,
    latestArticleId: Number(feed.latestArticleId) || 0,
  };
}

/**
 * Feed fields that change the rendered tree. `lastRefreshedAt` moves on
 * every refresh but is only read when the feed menu opens.
 * @param {Feed} feed
 */
function renderedFeedFields(feed) {
  const {
    lastRefreshedAt: _refreshed,
    latestArticleId: _latest,
    ...rest
  } = normalizeFeed(feed);
  return rest;
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
 * Stable fingerprint for "does the tree need re-rendering" checks
 * (ignores lastRefreshedAt).
 * @param {{ folders?: Folder[], unfiledFeeds?: Feed[] }} tree
 */
export function folderTreeFingerprint(tree) {
  return JSON.stringify({
    folders: (tree?.folders ?? []).map((folder) => ({
      ...normalizeFolder(folder),
      feeds: (folder.feeds ?? []).map(renderedFeedFields),
    })),
    unfiledFeeds: (tree?.unfiledFeeds ?? []).map(renderedFeedFields),
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

/**
 * @param {number|null|undefined} throughId
 * @returns {{ throughId: number } | undefined}
 */
function markReadBody(throughId) {
  if (throughId == null || !Number.isFinite(Number(throughId))) {
    return undefined;
  }
  return { throughId: Number(throughId) };
}

export const dataService = new DataService();
