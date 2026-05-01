"use strict";

import { fetchWithAuth } from "./pkce.js";

class DataService {
  constructor() {
    this.articleCache = [];
  }

  async getFolders() {
    return this._request("./api/folder");
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
      // Delegated to pkce.js: handles expiry check, refresh, and 401 retry
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

      throw new Error(`Failed to ${method} ${url}: ${error.message}`, {
        cause: error,
      });
    }
  }
}

export const dataService = new DataService();
