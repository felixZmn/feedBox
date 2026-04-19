"use strict";

/*
 * Following Idea:
 * This class is responsible for loading data from the backend.
 * Additionally, this class cares about things like caching, so that this is centrally managed.
 */

class DataService {
  constructor() {
    this.cache = [];
  }

  async getFolders() {
    return this._getRequest("./api/folder");
  }

  async createFolder(folder) {
    return this._postRequest("./api/folder", folder);
  }

  async updateFolder(folder) {
    return this._putRequest(`./api/folder/${folder.id}`, folder);
  }

  async deleteFolder(folderId) {
    return this._deleteRequest(`./api/folder/${folderId}`);
  }

  async createFeed(feed) {
    return this._postRequest("./api/feed", feed);
  }

  async updateFeed(feed) {
    return this._putRequest(`./api/feed/${feed.id}`, feed);
  }

  async deleteFeed(feedId) {
    return this._deleteRequest(`./api/feed/${feedId}`);
  }

  async checkFeed(feedUrl) {
    return this._getRequest(
      `./api/feed/check?url=${encodeURIComponent(feedUrl)}`,
    );
  }

  async refreshFeeds() {
    return this._postRequest("./api/feed/refresh");
  }

  getArticles() {
    return this.cache;
  }

  clearArticles() {
    this.cache = [];
  }

  async loadArticles(params) {
    const queryString = new URLSearchParams(params).toString();
    const url = `./api/article?${queryString}`;
    let data = await this._getRequest(url);
    this.cache = this.cache.concat(data);
    return data;
  }

  async _getRequest(url) {
    try {
      const response = await fetch(url, {
        method: "GET",
        credentials: "include",
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(
          `Request failed with status ${response.status}: ${errorText || response.statusText}`,
        );
      }
      if (response.status === 204) {
        return []; // No Content
      }

      return await response.json();
    } catch (error) {
      const enhancedError = new Error(
        `Failed to fetch data from ${url}: ${error.message}`,
      );
      enhancedError.originalError = error;
      throw enhancedError;
    }
  }

  async _postRequest(url, body) {
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const errorText = await response.text();
        const err = new Error(
          `Request failed with status ${response.status}: ${errorText || response.statusText}`,
        );
        err.status = response.status;
        err.serverMessage = errorText;
        throw err;
      }
      if (response.status === 204) {
        return null; // No Content
      }
      return await response.json();
    } catch (error) {
      const enhancedError = new Error(
        `Failed to fetch data from ${url}: ${error.message}`,
      );
      enhancedError.originalError = error;
      throw enhancedError;
    }
  }

  async _putRequest(url, body) {
    try {
      const response = await fetch(url, {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(
          `Request failed with status ${response.status}: ${errorText || response.statusText}`,
        );
      }
      return await response.json();
    } catch (error) {
      const enhancedError = new Error(
        `Failed to fetch data from ${url}: ${error.message}`,
      );
      enhancedError.originalError = error;
      throw enhancedError;
    }
  }

  async _deleteRequest(url) {
    try {
      const response = await fetch(url, {
        method: "DELETE",
        credentials: "include",
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(
          `Request failed with status ${response.status}: ${errorText || response.statusText}`,
        );
      }
    } catch (error) {
      const enhancedError = new Error(
        `Failed to fetch data from ${url}: ${error.message}`,
      );
      enhancedError.originalError = error;
      throw enhancedError;
    }
  }
}

const dataService = new DataService();
export { dataService };
