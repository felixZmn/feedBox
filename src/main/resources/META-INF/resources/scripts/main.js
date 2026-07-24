"use strict";

import {
  dataService,
  buildFolderTree,
  loadFolderTreeCache,
  saveFolderTreeCache,
  folderTreesEqual,
  hasReturningSession,
} from "./data.js";
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
  replaceArticlesList,
  renderFoldersList,
  buildFoldersHtml,
  syncFolderOpenStatesFromStorage,
  renderReaderView,
  clearArticlesList,
  renderSkeletons,
  removeSkeletons,
  renderEmptyState,
  showFeedsSpinner,
  hideFeedsSpinner,
  setSelectedArticleHighlight,
  updateNavSelection,
  setFolderOpenState,
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

/** Must match backend ArticleRepository LIMIT */
const ARTICLES_PAGE_SIZE = 25;

const itemType = Object.freeze({
  ALL: "",
  FEED: "feed",
  FOLDER: "folder",
});

const state = {
  articles: [],
  folders: [],
  unfiledFeeds: [],
  pagination: { id: null, published: null },
  filter: { isActive: false, lastSearchTerm: "" },
  status: {
    isRefreshing: false,
    isLoadingArticles: false,
    hasMoreArticles: true,
  },
  selectedArticle: null,
  lastClickedItem: { type: itemType.ALL, obj: null },
  restoredFromCache: false,
};

const dom = {
  contextMenu: document.getElementById("context-menu"),
  refreshSpinner: document.getElementById("refresh-spinner"),
  searchInput: document.getElementById("search-input"),
  button: {
    export: document.getElementById("trigger-export"),
    import: document.getElementById("trigger-import"),
    refresh: document.getElementById("trigger-refresh"),
    previous: document.getElementById("trigger-previous"),
    next: document.getElementById("trigger-next"),
    close: document.getElementById("trigger-close"),
    backToFeeds: document.getElementById("trigger-back-feeds"),
    showAllFeeds: document.getElementById("trigger-show-all-feeds"),
    add: document.getElementById("trigger-add"),
    addFeed: document.getElementById("trigger-feed-add"),
    editFeed: document.getElementById("trigger-feed-edit"),
    deleteFeed: document.getElementById("trigger-feed-delete"),
    openWebsite: document.getElementById("trigger-open-website"),
    addFolder: document.getElementById("trigger-folder-add"),
    editFolder: document.getElementById("trigger-folder-edit"),
    deleteFolder: document.getElementById("trigger-folder-delete"),
    logout: document.getElementById("trigger-logout"),
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
}

/**
 * Restore cached folders into state once. Skip DOM replace if the inline
 * injector already painted the renderer HTML.
 * @returns {boolean}
 */
function restoreFolderCache() {
  if (!hasReturningSession()) return false;
  const cached = loadFolderTreeCache();
  if (!cached) return false;

  state.folders = cached.folders;
  state.unfiledFeeds = cached.unfiledFeeds;
  state.restoredFromCache = true;

  const container = document.getElementById("folder-container");
  if (container?.dataset.cachePainted === "1") {
    syncFolderOpenStatesFromStorage();
    hideFeedsSpinner();
  } else {
    renderFoldersList(cached.folders, cached.unfiledFeeds);
    hideFeedsSpinner();
  }
  updateNavSelection(state.lastClickedItem.type, state.lastClickedItem.obj);
  return true;
}

window.addEventListener("DOMContentLoaded", async () => {
  restoreFolderCache();

  try {
    await initSSOConfig();
    await initializeAuth();

    if (!isAuthenticated()) {
      await redirectToAuthProvider();
      return;
    }

    initEventListeners();
    registerDelegatedEvents();
    lazyLoadObserver = setupScrollObserver();

    // Folder network refresh must not block article startup.
    await Promise.all([
      refreshFolders({ background: true }),
      loadArticles(),
    ]);
  } catch (error) {
    console.error("[app] Error during initialization:", error);
    hideFeedsSpinner();
    removeSkeletons();
    await modal.show({
      title: "Something went wrong",
      content:
        "The app failed to load. Please check your connection and reload the page.",
      type: "alert",
    });
  }
});

function initEventListeners() {
  document.addEventListener("click", () => {
    hideContextMenu();
  });

  dom.contextMenu.addEventListener("keydown", (e) => {
    const items = getVisibleContextMenuItems();
    if (items.length === 0) return;
    const currentIndex = items.indexOf(document.activeElement);
    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        items[currentIndex < 0 ? 0 : (currentIndex + 1) % items.length].focus();
        break;
      case "ArrowUp":
        e.preventDefault();
        items[
          currentIndex <= 0 ? items.length - 1 : currentIndex - 1
        ].focus();
        break;
      case "Enter":
      case " ":
        e.preventDefault();
        document.activeElement?.click();
        break;
      case "Escape":
        e.preventDefault();
        hideContextMenu();
        break;
    }
  });

  dom.button.next.addEventListener("click", () => navigateArticle(1));
  dom.button.previous.addEventListener("click", () => navigateArticle(-1));
  if (dom.button.backToFeeds) {
    dom.button.backToFeeds.addEventListener("click", (e) => {
      e.preventDefault();
      navigationService.navigateTo(columns.FEEDS);
    });
  }
  dom.button.showAllFeeds.addEventListener("click", () =>
    selectAndLoadArticles(itemType.ALL, null),
  );
  dom.button.add.addEventListener("click", (e) => {
    e.stopPropagation();
    openAddContextMenu(e.clientX, e.clientY);
  });
  dom.button.refresh.addEventListener("click", () => refreshFeeds());
  dom.button.export.addEventListener("click", async (event) => {
    event.preventDefault();
    await exportFeeds();
  });
  dom.button.import.addEventListener("click", () => importFeeds());
  dom.button.addFolder.addEventListener("click", async () => {
    const newFolder = await showAddFolderDialog();
    if (newFolder) await createFolder(newFolder);
  });
  dom.button.editFolder.addEventListener("click", async () => {
    const editedFolder = await showEditFolderDialog(state.lastClickedItem.obj);
    if (editedFolder) {
      editedFolder.id = state.lastClickedItem.obj.id;
      await editFolder(editedFolder);
    }
  });
  dom.button.deleteFolder.addEventListener("click", async () => {
    const headline = "Delete";
    const message = `Are you sure you want to delete the folder "${escapeHtml(state.lastClickedItem.obj.name)}"? All contained feeds will be deleted.`;
    const response = await showConfirmDialog(headline, message);
    if (response) await deleteFolder(state.lastClickedItem.obj);
  });

  dom.button.addFeed.addEventListener("click", async () => {
    const newFeed = await showAddFeedDialog(state.folders);
    if (newFeed) {
      newFeed.folderId = newFeed.folderId || null;
      await createFeed(newFeed);
    }
  });
  dom.button.editFeed.addEventListener("click", async () => {
    const response = await showEditFeedDialog(
      state.folders,
      state.lastClickedItem.obj,
    );
    if (response) {
      const editedFeed = {
        ...state.lastClickedItem.obj,
        feedUrl: response.feedUrl,
        folderId: response.folderId || null,
      };
      await editFeed(editedFeed);
    }
  });
  dom.button.openWebsite.addEventListener("click", async () => {
    if (!state.lastClickedItem.obj) return;
    const websiteUrl = state.lastClickedItem.obj.url;
    if (websiteUrl) {
      window.open(websiteUrl, "_blank", "noopener,noreferrer");
    }
  });
  dom.button.deleteFeed.addEventListener("click", async () => {
    const headline = "Delete";
    const message = `Are you sure you want to delete the feed "${escapeHtml(state.lastClickedItem.obj.name)}"?`;
    const response = await showConfirmDialog(headline, message);
    if (response) await deleteFeed(state.lastClickedItem.obj);
  });
  dom.button.close.addEventListener("click", (e) => {
    e.preventDefault();
    state.selectedArticle = null;
    clearReaderView();
    navigationService.navigateTo(columns.ARTICLES);
  });
  if (dom.button.logout) {
    dom.button.logout.addEventListener("click", () => {
      logout();
    });
  }
  dom.searchInput.addEventListener("input", (e) => {
    const searchTerm = e.target.value.trim().toLowerCase();
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
 * One delegated event layer for boot-painted and live-rendered trees/articles.
 */
function registerDelegatedEvents() {
  const feedsList = document.getElementById("feeds-list");
  const articlesList = document.getElementById("articles-list");

  feedsList?.addEventListener("click", (e) => {
    const options = e.target.closest(".tree-options");
    if (options) {
      e.preventDefault();
      e.stopPropagation();
      const feedLi = options.closest("li[data-feed-id]");
      if (feedLi) {
        const feed = findFeedById(Number(feedLi.dataset.feedId));
        if (feed) feedContextMenu(e.clientX, e.clientY, feed);
        return;
      }
      const folderDetails = options.closest("details[data-folder-id]");
      if (folderDetails) {
        const folder = findFolderById(Number(folderDetails.dataset.folderId));
        if (folder) folderContextMenu(e.clientX, e.clientY, folder);
      }
      return;
    }

    const feedLi = e.target.closest("li[data-feed-id]");
    if (feedLi && feedsList.contains(feedLi)) {
      const feed = findFeedById(Number(feedLi.dataset.feedId));
      if (feed) selectAndLoadArticles(itemType.FEED, feed);
      return;
    }

    const folderName = e.target.closest(
      "details[data-folder-id] > summary > .tree-name",
    );
    if (folderName) {
      e.preventDefault();
      const details = folderName.closest("details[data-folder-id]");
      const folder = findFolderById(Number(details.dataset.folderId));
      if (folder) selectAndLoadArticles(itemType.FOLDER, folder);
    }
  });

  feedsList?.addEventListener("keydown", (e) => {
    if (e.key !== "Enter" && e.key !== " ") return;
    const options = e.target.closest(".tree-options");
    if (!options || !feedsList.contains(options)) return;
    e.preventDefault();
    const rect = options.getBoundingClientRect();
    const feedLi = options.closest("li[data-feed-id]");
    if (feedLi) {
      const feed = findFeedById(Number(feedLi.dataset.feedId));
      if (feed) feedContextMenu(rect.left, rect.bottom, feed);
      return;
    }
    const folderDetails = options.closest("details[data-folder-id]");
    if (folderDetails) {
      const folder = findFolderById(Number(folderDetails.dataset.folderId));
      if (folder) folderContextMenu(rect.left, rect.bottom, folder);
    }
  });

  feedsList?.addEventListener("toggle", (e) => {
    const details = e.target;
    if (!(details instanceof HTMLDetailsElement)) return;
    if (!details.dataset.folderId) return;
    setFolderOpenState(details.dataset.folderId, details.open);
  }, true);

  feedsList?.addEventListener(
    "error",
    (e) => {
      const img = e.target;
      if (!(img instanceof HTMLImageElement)) return;
      const fallback = img.dataset.fallbackIcon;
      if (!fallback || img.src.endsWith(fallback)) return;
      img.src = fallback;
    },
    true,
  );

  articlesList?.addEventListener("click", (e) => {
    const articleEl = e.target.closest(".article[data-article-id]");
    if (!articleEl || !articlesList.contains(articleEl)) return;
    const article = state.articles.find(
      (a) => a.id === Number(articleEl.dataset.articleId),
    );
    if (article) articleClickListener(article);
  });

  articlesList?.addEventListener("keydown", (e) => {
    if (e.key !== "Enter" && e.key !== " ") return;
    const articleEl = e.target.closest(".article[data-article-id]");
    if (!articleEl || !articlesList.contains(articleEl)) return;
    e.preventDefault();
    const article = state.articles.find(
      (a) => a.id === Number(articleEl.dataset.articleId),
    );
    if (article) articleClickListener(article);
  });
}

function findFeedById(id) {
  for (const folder of state.folders) {
    const feed = (folder.feeds ?? []).find((f) => f.id === id);
    if (feed) return feed;
  }
  return state.unfiledFeeds.find((f) => f.id === id) ?? null;
}

function findFolderById(id) {
  return state.folders.find((f) => f.id === id) ?? null;
}

function resolveLastClickedItem() {
  const { type, obj } = state.lastClickedItem;
  if (type === itemType.FEED && obj) {
    state.lastClickedItem.obj = findFeedById(obj.id);
    if (!state.lastClickedItem.obj) {
      state.lastClickedItem = { type: itemType.ALL, obj: null };
    }
  } else if (type === itemType.FOLDER && obj) {
    state.lastClickedItem.obj = findFolderById(obj.id);
    if (!state.lastClickedItem.obj) {
      state.lastClickedItem = { type: itemType.ALL, obj: null };
    }
  }
}

/**
 * @param {Folder[]} folders
 * @param {Feed[]} unfiledFeeds
 * @param {{ render?: boolean }} [opts]
 */
function applyFolderTree(folders, unfiledFeeds, { render = true } = {}) {
  state.folders = folders;
  state.unfiledFeeds = unfiledFeeds;
  resolveLastClickedItem();
  let html;
  if (render) {
    html = renderFoldersList(folders, unfiledFeeds);
  } else {
    html = buildFoldersHtml(folders, unfiledFeeds);
  }
  updateNavSelection(state.lastClickedItem.type, state.lastClickedItem.obj);
  return html;
}

/**
 * Network refresh for folders/feeds.
 * @param {{ background?: boolean }} [options]
 */
async function refreshFolders({ background = true } = {}) {
  const hadCache = background && state.restoredFromCache;
  if (!hadCache) {
    showFeedsSpinner();
  }

  try {
    const [folders, feeds] = await Promise.all([
      dataService.getFolders(),
      dataService.getFeeds(),
    ]);
    const tree = buildFolderTree(folders, feeds);
    const changed = !folderTreesEqual(
      { folders: state.folders, unfiledFeeds: state.unfiledFeeds },
      tree,
    );

    if (changed || !hadCache) {
      const html = applyFolderTree(tree.folders, tree.unfiledFeeds, {
        render: true,
      });
      saveFolderTreeCache(tree.folders, tree.unfiledFeeds, html);
    } else {
      state.folders = tree.folders;
      state.unfiledFeeds = tree.unfiledFeeds;
      resolveLastClickedItem();
    }
    state.restoredFromCache = true;
  } catch (error) {
    if (hadCache) {
      console.error("[app] Failed to refresh folders:", error);
    } else {
      throw error;
    }
  } finally {
    hideFeedsSpinner();
  }
}

function loadArticle(article) {
  const result = state.articles.find((a) => a.id === article.id);
  if (!result) {
    console.error("Article not found:", article);
    return;
  }
  state.selectedArticle = result;
  setSelectedArticleHighlight(result.id);
  renderReaderView(result);
}

function setupScrollObserver() {
  const sentinel = document.getElementById("articles-sentinel");

  const observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0];
      if (!entry) return;
      if (
        entry.isIntersecting &&
        !state.filter.isActive &&
        !state.status.isLoadingArticles &&
        state.status.hasMoreArticles
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

function showContextMenuGroup(groupClass, x, y) {
  document.querySelectorAll(".context-menu-item").forEach((element) => {
    element.style.display = "none";
  });
  document.querySelectorAll(`.${groupClass}`).forEach((element) => {
    element.style.display = "block";
  });
  openContextMenu(x, y);
}

function openAddContextMenu(x, y) {
  showContextMenuGroup("context-add", x, y);
}

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

function feedContextMenu(x, y, feed) {
  state.lastClickedItem.type = itemType.FEED;
  state.lastClickedItem.obj = feed;
  showContextMenuGroup("context-feed", x, y);
}

function folderContextMenu(x, y, folder) {
  state.lastClickedItem.type = itemType.FOLDER;
  state.lastClickedItem.obj = folder;
  showContextMenuGroup("context-folder", x, y);
}

function openContextMenu(x, y) {
  const menu = dom.contextMenu;
  menu.style.display = "block";
  const { innerWidth, innerHeight } = window;
  const menuRect = menu.getBoundingClientRect();
  menu.style.left = `${Math.min(x, innerWidth - menuRect.width)}px`;
  menu.style.top = `${Math.min(y, innerHeight - menuRect.height)}px`;
  getVisibleContextMenuItems()[0]?.focus();
}

function hideContextMenu() {
  dom.contextMenu.style.display = "none";
}

function getVisibleContextMenuItems() {
  return [...dom.contextMenu.querySelectorAll(".context-menu-item")].filter(
    (el) => el.style.display !== "none",
  );
}

async function selectAndLoadArticles(type, obj) {
  navigationService.navigateTo(columns.ARTICLES);
  resetPagination();
  state.lastClickedItem = { type, obj };
  updateNavSelection(type, obj);
  lazyLoadObserver?.pause();
  clearArticles();
  await loadArticles();
  lazyLoadObserver?.resume();
}

function articleClickListener(article) {
  loadArticle(article);
  navigationService.navigateTo(columns.READER);
}

function clearArticles() {
  state.articles = [];
  dataService.clearArticles();
  clearArticlesList();
  removeSkeletons();
  document.querySelector("#articles-list .column").scrollTop = 0;
}

function resetPagination() {
  state.pagination.id = null;
  state.pagination.published = null;
  state.status.hasMoreArticles = true;
}

async function createFolder(folder) {
  try {
    await dataService.createFolder(folder);
    await refreshFolders({ background: false });
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
    await refreshFolders({ background: false });
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
    const wasSelected =
      state.lastClickedItem.type === itemType.FOLDER &&
      state.lastClickedItem.obj?.id === folder.id;
    await dataService.deleteFolder(folder.id);
    await refreshFolders({ background: false });
    if (wasSelected || state.lastClickedItem.type === itemType.ALL) {
      await selectAndLoadArticles(itemType.ALL, null);
    } else if (
      state.lastClickedItem.type === itemType.FOLDER &&
      !state.lastClickedItem.obj
    ) {
      await selectAndLoadArticles(itemType.ALL, null);
    }
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
    await refreshFolders({ background: false });
  } catch (error) {
    console.error(error);
    const isDuplicate =
      error.status === 409 || error.cause?.status === 409;
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
    await refreshFolders({ background: false });
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
    const wasSelected =
      state.lastClickedItem.type === itemType.FEED &&
      state.lastClickedItem.obj?.id === feed.id;
    await dataService.deleteFeed(feed.id);
    await refreshFolders({ background: false });
    if (wasSelected || !state.lastClickedItem.obj) {
      await selectAndLoadArticles(itemType.ALL, null);
    } else {
      state.articles = state.articles.filter(
        (article) => article.feedId !== feed.id,
      );
      replaceArticlesList(state.articles);
    }
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

  const cleanup = () => {
    fileInput.remove();
  };

  fileInput.addEventListener(
    "change",
    async () => {
      try {
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
          await refreshFolders({ background: false });
          await refreshFeeds();
        } else {
          await modal.show({
            title: "Error",
            content: "Error importing feeds: " + response.statusText,
            type: "alert",
          });
        }
      } finally {
        cleanup();
      }
    },
    { once: true },
  );

  fileInput.addEventListener("cancel", cleanup, { once: true });
  fileInput.click();
}

async function exportFeeds() {
  try {
    const response = await fetchWithAuth("./api/opml");

    if (!response.ok) {
      throw new Error(
        response.statusText || response.status || "Unknown error",
      );
    }

    downloadBlob(await response.blob(), "feed-export.opml");
  } catch (error) {
    const isHttpError = error.message !== "Failed to fetch";
    await modal.show({
      title: "Error",
      content: isHttpError
        ? `Error exporting feeds: ${error.message}`
        : "Error exporting feeds. Please try again.",
      type: "alert",
    });
    console.error("Error exporting feeds:", error);
  }
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = Object.assign(document.createElement("a"), {
    href: url,
    download: filename,
  });

  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

async function refreshFeeds() {
  if (state.status.isRefreshing) {
    return;
  }

  state.status.isRefreshing = true;
  dom.refreshSpinner.classList.add("spinner");
  try {
    await dataService.refreshFeeds();
    await selectAndLoadArticles(itemType.ALL, null);
  } finally {
    dom.refreshSpinner.classList.remove("spinner");
    state.status.isRefreshing = false;
  }
}

async function loadArticles() {
  if (state.status.isLoadingArticles) return;
  state.status.isLoadingArticles = true;

  removeSkeletons();
  renderSkeletons(6);

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
      default:
        break;
    }
    if (state.pagination.id != null) {
      params.pagination_id = state.pagination.id;
    }
    if (state.pagination.published != null) {
      params.pagination_date = state.pagination.published;
    }

    const newArticles = await dataService.loadArticles(params);

    removeSkeletons();

    if (!newArticles || newArticles.length === 0) {
      state.status.hasMoreArticles = false;
      if (dataService.getArticles().length === 0) {
        renderEmptyState();
      }
      return;
    }

    state.articles = dataService.getArticles();
    state.status.hasMoreArticles = newArticles.length >= ARTICLES_PAGE_SIZE;

    const lastArticle = newArticles[newArticles.length - 1];
    state.pagination.id = lastArticle.id;
    state.pagination.published = lastArticle.published;

    appendArticlesList(newArticles, state.selectedArticle?.id ?? null);
  } finally {
    removeSkeletons();
    state.status.isLoadingArticles = false;
  }
}
