/*
 * Following Idea:
 * This class is responsible for loading data from the backend.
 * Additionally, this class cares about things like caching, so that this is centrally managed.
 */

class DataService {
  constructor() {
    this.cache = new Map();
  }

  async getFolders() {
    return await this._getRequest("./api/folders");
  }

  async createFolder(folder) {
    return await this._postRequest("./api/folders", folder);
  }

  async updateFolder(folder) {
    return await this._putRequest(`./api/folders/${folder.id}`, folder);
  }

  async deleteFolder(folderId) {
    return await this._deleteRequest(`./api/folders/${folderId}`);
  }

  async createFeed(feed) {
    return await this._postRequest("./api/feeds", feed);
  }

  async updateFeed(feed) {
    return await this._putRequest(`./api/feeds/${feed.id}`, feed);
  }

  async deleteFeed(feedId) {
    return await this._deleteRequest(`./api/feeds/${feedId}`);
  }

  async refreshFeeds() {
    return await this._getRequest("./api/feeds/refresh");
  }

  async getArticles(params) {
    const queryString = new URLSearchParams(params).toString();
    const url = `./api/articles?${queryString}`;
    return await this._getRequest(url);
  }

  async _getRequest(url) {
    try {
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Error fetching data from ${url}: ${response.status}`);
      }
      if (response.status === 204) {
        return [];
      }
      const data = await response.json();
      return data;
    } catch (error) {
      throw new Error(`Error fetching data from ${url}: ${error.message}`);
    }
  }

  async _postRequest(url, body) {
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        throw new Error(`Error posting data to ${url}: ${response.status}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      throw new Error(`Error posting data to ${url}: ${error.message}`);
    }
  }

  async _putRequest(url, body) {
    try {
      const response = await fetch(url, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });
      if (!response.ok) {
        throw new Error(`Error putting data to ${url}: ${response.status}`);
      }
      const data = await response.json();
      return data;
    } catch (error) {
      throw new Error(`Error putting data to ${url}: ${error.message}`);
    }
  }

  async _deleteRequest(url) {
    try {
      const response = await fetch(url, {
        method: "DELETE",
      });
      if (!response.ok) {
        throw new Error(`Error deleting data from ${url}: ${response.status}`);
      }
      return;
    } catch (error) {
      throw new Error(`Error deleting data from ${url}: ${error.message}`);
    }
  }
}

const dataService = new DataService();
export { dataService };
