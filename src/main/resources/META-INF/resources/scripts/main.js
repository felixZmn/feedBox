"use strict";

import { dataService } from "./data.js";
import { modal } from "./modal.js";
import {
  showAddFeedDialog,
  showAddFolderDialog,
  showConfirmDialog,
  showEditFeedDialog,
  showEditFolderDialog,
} from "./dialog.js";
import {
  appendArticlesList,
  clearReaderView,
  removeFeedElement,
  replaceArticlesList,
  renderFoldersList,
  renderReaderView,
  clearArticlesList,
} from "./dom.js";
import { NavigationService, columns } from "./nav.js";
import { escapeHtml } from "./util.js";
import {
  initializeAuth,
  initSSOConfig,
  redirectToAuthProvider,
  isAuthenticated,
  logout,
  fetchWithAuth,
} from "./pkce.js";

const itemType = Object.freeze({
  ALL: "",
  FEED: "feed",
  FOLDER: "folder",
});

// Application state
const state = {
  articles: [],
  folders: [],
  pagination: { id: null, published: null },
  filter: { isActive: false, lastSearchTerm: "" },
  status: { isRefreshing: false, isLoadingArticles: false },
  selectedArticle: null,
  lastClickedItem: { type: itemType.ALL, obj: null },
};

// Cache DOM elements for later use
const dom = {
  contextMenu: document.getElementById("context-menu"),
  refreshSpinner: document.getElementById("refresh-spinner"),
  searchInput: document.getElementById("search-input"),
  button: {
    import: document.getElementById("trigger-import"),
    refresh: document.getElementById("trigger-refresh"),
    previous: document.getElementById("trigger-previous"),
    next: document.getElementById("trigger-next"),
    close: document.getElementById("trigger-close"),
    showAllFeeds: document.getElementById("trigger-show-all-feeds"),
    addFeed: document.getElementById("trigger-feed-add"),
    editFeed: document.getElementById("trigger-feed-edit"),
    deleteFeed: document.getElementById("trigger-feed-delete"),
    openWebsite: document.getElementById("trigger-open-website"),
    addFolder: document.getElementById("trigger-folder-add"),
    editFolder: document.getElementById("trigger-folder-edit"),
    deleteFolder: document.getElementById("trigger-folder-delete"),
    logout: document.getElementById("trigger-logout"),
    profile: document.getElementById("trigger-profile"),
  },
};

let lazyLoadObserver = null;
const navigationService = new NavigationService();

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/sw.js").catch((err) => {
      console.error("Service worker registration failed:", err);
    });
  });
} else {
  console.error("Service workers are not supported.");
}

window.addEventListener("DOMContentLoaded", async () => {
  try {
    // Initialize authentication (handles OAuth callback if present)
    await initSSOConfig();
    await initializeAuth();

    // Check if user is authenticated
    if (!isAuthenticated()) {
      // Redirect to login provider
      await redirectToAuthProvider();
      return;
    }

    await loadFolders();
    await loadArticles();
    lazyLoadObserver = setupScrollObserver();
    initEventListeners();
  } catch (error) {
    console.error("[app] Error during initialization:", error);
    throw error;
  }
});

/**
 * Helper to set up all event listeners in a single place
 */
function initEventListeners() {
  document.addEventListener("click", () => {
    dom.contextMenu.style.display = "none";
  });

  dom.button.next.addEventListener("click", () => navigateArticle(1));
  dom.button.previous.addEventListener("click", () => navigateArticle(-1));
  dom.button.showAllFeeds.addEventListener("click", () =>
    allFeedsClickListener(),
  );
  dom.button.refresh.addEventListener("click", () => refreshFeeds());
  dom.button.import.addEventListener("click", () => importFeeds());
  dom.button.addFolder.addEventListener("click", async () => {
    let newFolder = await showAddFolderDialog();
    if (newFolder) await createFolder(newFolder);
  });
  dom.button.editFolder.addEventListener("click", async () => {
    let editedFolder = await showEditFolderDialog(state.lastClickedItem.obj);
    if (editedFolder) {
      editedFolder.id = state.lastClickedItem.obj.id;
      await editFolder(editedFolder);
    }
  });
  dom.button.deleteFolder.addEventListener("click", async () => {
    let headline = "Delete";
    let message = `Are you sure you want to delete the folder "${escapeHtml(state.lastClickedItem.obj.name)}"? All contained feeds will be deleted.`;
    let response = await showConfirmDialog(headline, message);
    if (response) await deleteFolder(state.lastClickedItem.obj);
  });

  dom.button.addFeed.addEventListener("click", async () => {
    let newFeed = await showAddFeedDialog(state.folders);
    if (newFeed) await createFeed(newFeed);
  });
  dom.button.editFeed.addEventListener("click", async () => {
    let response = await showEditFeedDialog(
      state.folders,
      state.lastClickedItem.obj,
    );
    if (response) {
      let editedFeed = state.lastClickedItem.obj;
      editedFeed.feedUrl = response.feedUrl;
      editedFeed.folderId = response.folderId;
      await editFeed(editedFeed);
    }
  });
  dom.button.openWebsite.addEventListener("click", async () => {
    if (!state.lastClickedItem.obj) return;
    let websiteUrl = state.lastClickedItem.obj.url;
    if (websiteUrl) {
      window.open(websiteUrl, "_blank");
    }
  });
  dom.button.deleteFeed.addEventListener("click", async () => {
    let headline = "Delete";
    let message = `Are you sure you want to delete the feed "${escapeHtml(state.lastClickedItem.obj.name)}"?`;
    let response = await showConfirmDialog(headline, message);
    if (response) await deleteFeed(state.lastClickedItem.obj);
  });
  dom.button.close.addEventListener("click", () => {
    clearReaderView();
    navigationService.navigateTo(columns.ARTICLES);
  });
  if (dom.button.logout) {
    dom.button.logout.addEventListener("click", () => {
      logout();
    });
  }
  if (dom.button.profile) {
    dom.button.profile.addEventListener("click", () => {
      // placeholder, no use atm
    });
  }
  dom.searchInput.addEventListener("input", (e) => {
    let searchTerm = e.target.value.trim().toLowerCase();
    // filter empty -> reset
    if (searchTerm === "") {
      state.filter.isActive = false;
      state.filter.lastSearchTerm = "";
      state.articles = dataService.getArticles();
      replaceArticlesList(state.articles);
      return;
    }
    if (!searchTerm.startsWith(state.filter.lastSearchTerm)) {
      state.articles = dataService.getArticles();
    }
    state.filter.lastSearchTerm = searchTerm;
    state.articles = state.articles.filter((article) =>
      (article.title ?? "").toLowerCase().includes(searchTerm),
    );
    state.filter.isActive = true;
    replaceArticlesList(state.articles);
  });
}

/**
 * searches the selected article by id in the global articles array and renders it
 * ToDo: add lazy loading of missing articles - currently not an issue
 * @param {Article} article
 * @returns
 */
export function loadArticle(article) {
  // article should be stored in global articles array
  const result = state.articles.find((a) => a.id === article.id);
  if (!result) {
    console.error("Article not found:", article);
    return;
  }
  state.selectedArticle = result;
  renderReaderView(result);
}

/**
 * Sets up the scroll observer for infinite scrolling in the articles list
 */
function setupScrollObserver() {
  const sentinel = document.getElementById("articles-sentinel");

  const observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0];
      if (!entry) return;
      if (
        entry.isIntersecting &&
        !state.filter.isActive &&
        !state.status.isLoadingArticles
      ) {
        loadArticles();
      }
    },
    {
      root: document.querySelector("#articles-list .column"),
      rootMargin: "325px",
      threshold: 0.1,
    },
  );
  observer.observe(sentinel);
  return {
    pause: () => observer.disconnect(),
    resume: () => observer.observe(sentinel),
  };
}

/**
 * Click listener for the "Add"-Element
 */
export function openAddContextMenu(x, y) {
  // hide all items first, then show the add menu items
  document.querySelectorAll(".context-menu-item").forEach((element) => {
    element.style.display = "none";
  });
  document.querySelectorAll(".context-add").forEach((element) => {
    element.style.display = "block";
  });
  openContextMenu(x, y);
}

/**
 * helper to navigate to the next/previous article
 * @param {*} direction
 * @returns
 */
function navigateArticle(direction) {
  if (!state.selectedArticle) return;

  const idx = state.articles.findIndex(
    (a) => a.id === state.selectedArticle.id,
  );
  if (idx === -1) return;

  const nextIdx = idx + direction;
  if (nextIdx >= 0 && nextIdx < state.articles.length) {
    loadArticle(state.articles[nextIdx]);
  }
}

export function feedContextMenu(x, y, feed) {
  state.lastClickedItem.type = itemType.FEED;
  state.lastClickedItem.obj = feed;
  // hide all items first, then show the feed menu items
  document.querySelectorAll(".context-menu-item").forEach((element) => {
    element.style.display = "none";
  });
  document.querySelectorAll(".context-feed").forEach((element) => {
    element.style.display = "block";
  });
  openContextMenu(x, y);
}

export function folderContextMenu(x, y, folder) {
  state.lastClickedItem.type = itemType.FOLDER;
  state.lastClickedItem.obj = folder;
  // hide all items first, then show the folder menu items
  document.querySelectorAll(".context-menu-item").forEach((element) => {
    element.style.display = "none";
  });
  document.querySelectorAll(".context-folder").forEach((element) => {
    element.style.display = "block";
  });
  openContextMenu(x, y);
}

function openContextMenu(x, y) {
  const menu = dom.contextMenu;
  menu.style.display = "block";
  const { innerWidth, innerHeight } = window;
  const menuRect = menu.getBoundingClientRect();
  menu.style.left = `${Math.min(x, innerWidth - menuRect.width)}px`;
  menu.style.top = `${Math.min(y, innerHeight - menuRect.height)}px`;
}

/**
 * Click listener for an click on the "All Feeds"-Element
 */
export async function allFeedsClickListener() {
  navigationService.navigateTo(columns.ARTICLES);
  resetPagination();
  state.lastClickedItem.type = itemType.ALL;
  state.lastClickedItem.obj = null;
  lazyLoadObserver.pause();
  clearArticles();
  await loadArticles();
  lazyLoadObserver.resume();
}

/**
 * Click listener for a click on a single feed in the left-side list
 * @param {Feed} feed the clicked feed
 */
export async function feedClickListener(feed) {
  navigationService.navigateTo(columns.ARTICLES);
  resetPagination();
  state.lastClickedItem.type = itemType.FEED;
  state.lastClickedItem.obj = feed;
  lazyLoadObserver.pause();
  clearArticles();
  await loadArticles();
  lazyLoadObserver.resume();
}

/**
 * Click listener for a click on a single folder in the left-side list
 * @param {Folder} folder id of the clicked folder
 */
export async function folderClickListener(folder) {
  navigationService.navigateTo(columns.ARTICLES);
  resetPagination();
  state.lastClickedItem.type = itemType.FOLDER;
  state.lastClickedItem.obj = folder;
  lazyLoadObserver.pause();
  clearArticles();
  await loadArticles();
  lazyLoadObserver.resume();
}

/**
 * Click listener for a click on a single article in the middle list
 * @param {Article} article the clicked article
 */
export function articleClickListener(article) {
  loadArticle(article);
  navigationService.navigateTo(columns.READER);
}

function clearArticles() {
  state.articles = [];
  dataService.clearArticles();
  clearArticlesList();
}

function resetPagination() {
  state.pagination.id = null;
  state.pagination.published = null;
}

async function loadFolders() {
  const foldersWithFeeds = await dataService.getFolders();
  renderFoldersList(foldersWithFeeds);
  state.folders = foldersWithFeeds;
}

async function createFolder(folder) {
  try {
    await dataService.createFolder(folder);
    await loadFolders();
  } catch (error) {
    console.error(error.message);
    await modal.show({
      title: "Error",
      content: "Error saving folder: " + folder.name,
      type: "alert",
    });
  }
}

async function editFolder(folder) {
  try {
    await dataService.updateFolder(folder);
    await loadFolders();
    state.lastClickedItem.obj.name = folder.name;
    state.lastClickedItem.obj.color = folder.color;
  } catch (error) {
    console.error(error.message);
    await modal.show({
      title: "Error",
      content: "Error updating folder: " + folder.name,
      type: "alert",
    });
  }
}

async function deleteFolder(folder) {
  try {
    await dataService.deleteFolder(folder.id);
    await loadFolders();
  } catch (error) {
    console.error(error.message);
    await modal.show({
      title: "Error",
      content: "Error deleting folder: " + folder.name,
      type: "alert",
    });
  }
}

async function createFeed(feed) {
  try {
    await dataService.createFeed(feed);
    await loadFolders();
  } catch (error) {
    console.error(error);
    const isDuplicate = error.originalError?.status === 409;
    await modal.show({
      title: "Error",
      content: isDuplicate ? "This feed already exists." : "Error saving feed.",
      type: "alert",
    });
  }
}

async function editFeed(feed) {
  try {
    await dataService.updateFeed(feed);
    await loadFolders();
    state.lastClickedItem.obj.url = feed.feedUrl;
    state.lastClickedItem.obj.folderId = feed.folderId;
  } catch (error) {
    console.error(error.message);
    await modal.show({
      title: "Error",
      content: "Error updating feed: " + feed.feedUrl,
      type: "alert",
    });
  }
}

async function deleteFeed(feed) {
  try {
    await dataService.deleteFeed(feed.id);
    removeFeedElement(feed.id);
    state.articles = state.articles.filter(
      (article) => article.feedId !== feed.id,
    );
    replaceArticlesList(state.articles);
  } catch (error) {
    console.error(error.message);
    await modal.show({
      title: "Error",
      content: "Error deleting feed: " + feed.name,
      type: "alert",
    });
  }
}

async function importFeeds() {
  const fileInput = document.createElement("input");
  fileInput.type = "file";
  fileInput.style.display = "none";
  fileInput.accept = ".opml,.xml,application/xml,text/xml";
  document.body.appendChild(fileInput);

  fileInput.addEventListener("change", async () => {
    if (fileInput.files.length === 0) {
      await modal.show({
        title: "Error",
        content: "Please select a file to import.",
        type: "alert",
      });
      return;
    }
    const file = fileInput.files[0];

    const response = await fetchWithAuth("./api/opml", {
      method: "POST",
      body: file,
      headers: {
        "Content-Type": file.type,
      },
    });

    if (response.ok) {
      await loadFolders();
      await refreshFeeds();
    } else {
      await modal.show({
        title: "Error",
        content: "Error importing feeds: " + response.statusText,
        type: "alert",
      });
    }

    document.body.removeChild(fileInput);
  });

  fileInput.click();
}

async function refreshFeeds() {
  if (state.status.isRefreshing) {
    return;
  }

  state.status.isRefreshing = true;
  dom.refreshSpinner.classList.add("spinner");
  try {
    await dataService.refreshFeeds();
    await allFeedsClickListener();
  } finally {
    dom.refreshSpinner.classList.remove("spinner");
    state.status.isRefreshing = false;
  }
}

async function loadArticles() {
  if (state.status.isLoadingArticles) return;
  state.status.isLoadingArticles = true;
  try {
    const params = {};
    switch (state.lastClickedItem.type) {
      case itemType.FEED:
        if (!state.lastClickedItem.obj) return;
        params.feed = state.lastClickedItem.obj.id;
        break;
      case itemType.FOLDER:
        if (!state.lastClickedItem.obj) return;
        params.folder = state.lastClickedItem.obj.id;
        break;
      case itemType.ALL:
      // no additional param
      default:
        // no additional param
        break;
    }
    if (state.pagination.id != null) {
      params.pagination_id = state.pagination.id;
    }
    if (state.pagination.published != null) {
      params.pagination_date = state.pagination.published;
    }

    const newArticles = await dataService.loadArticles(params);
    if (!newArticles || newArticles.length === 0) return;

    state.articles = dataService.getArticles();

    // update pagination
    const lastArticle = newArticles[newArticles.length - 1];
    state.pagination.id = lastArticle.id;
    state.pagination.published = lastArticle.published;

    appendArticlesList(newArticles);
  } finally {
    state.status.isLoadingArticles = false;
  }
}
