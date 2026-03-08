/*
 * Following Idea:
 * This class is responsible for loading data from the backend.
 * Additionally, this class cares about things like caching, so that this is centrally managed.
 */

import { authService } from "./auth.js";

class DataService {
  constructor() {
    this.cache = [];
  }

  async getFolders() {
    return this._getRequest("./api/folders");
  }

  async createFolder(folder) {
    return this._postRequest("./api/folders", folder);
  }

  async updateFolder(folder) {
    return this._putRequest(`./api/folders/${folder.id}`, folder);
  }

  async deleteFolder(folderId) {
    return this._deleteRequest(`./api/folders/${folderId}`);
  }

  async createFeed(feed) {
    return this._postRequest("./api/feeds", feed);
  }

  async updateFeed(feed) {
    return this._putRequest(`./api/feeds/${feed.id}`, feed);
  }

  async deleteFeed(feedId) {
    return this._deleteRequest(`./api/feeds/${feedId}`);
  }

  async checkFeed(feedUrl) {
    return this._getRequest(
      `./api/feeds/check?url=${encodeURIComponent(feedUrl)}`,
    );
  }

  async refreshFeeds() {
    return this._postRequest("./api/feeds/refresh");
  }

  getArticles() {
    return this.cache;
  }

  clearArticles() {
    this.cache = [];
  }

  async loadArticles(params) {
    const queryString = new URLSearchParams(params).toString();
    const url = `./api/articles?${queryString}`;
    let data = await this._getRequest(url);
    this.cache = this.cache.concat(data);
    return data;
  }

  /**
   * Add authentication headers to request options
   */
  async _getAuthHeaders() {
    const accessToken = await authService.getValidAccessToken();
    if (!accessToken) {
      throw new Error("No valid access token available");
    }
    return {
      Authorization: `Bearer ${accessToken}`,
    };
  }

  /**
   * Handle 401 response - refresh token and retry
   */
  async _handleUnauthorized() {
    console.warn("Received 401 Unauthorized, attempting token refresh");
    const refreshed = await authService.refreshAccessToken();
    if (!refreshed) {
      // Refresh failed, redirect to login
      authService.login();
      throw new Error("Session expired, redirecting to login");
    }
    return true;
  }

  async _getRequest(url, shouldRetry = true) {
    try {
      const headers = await this._getAuthHeaders();
      const response = await fetch(url, {
        headers,
      });

      if (response.status === 401 && shouldRetry) {
        await this._handleUnauthorized();
        // Retry the request with new token
        return this._getRequest(url, false);
      }

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

  async _postRequest(url, body, shouldRetry = true) {
    try {
      const headers = {
        "Content-Type": "application/json",
        ...(await this._getAuthHeaders()),
      };

      const response = await fetch(url, {
        method: "POST",
        headers,
        body: JSON.stringify(body),
      });

      if (response.status === 401 && shouldRetry) {
        await this._handleUnauthorized();
        // Retry the request with new token
        return this._postRequest(url, body, false);
      }

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

  async _putRequest(url, body, shouldRetry = true) {
    try {
      const headers = {
        "Content-Type": "application/json",
        ...(await this._getAuthHeaders()),
      };

      const response = await fetch(url, {
        method: "PUT",
        headers,
        body: JSON.stringify(body),
      });

      if (response.status === 401 && shouldRetry) {
        await this._handleUnauthorized();
        // Retry the request with new token
        return this._putRequest(url, body, false);
      }

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

  async _deleteRequest(url, shouldRetry = true) {
    try {
      const headers = await this._getAuthHeaders();

      const response = await fetch(url, {
        method: "DELETE",
        headers,
      });

      if (response.status === 401 && shouldRetry) {
        await this._handleUnauthorized();
        // Retry the request with new token
        return this._deleteRequest(url, false);
      }

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
