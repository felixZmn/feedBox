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
  syncFolderOpenStatesFromStorage,
  renderReaderView,
  clearArticlesList,
  renderSkeletons,
  removeSkeletons,
  renderEmptyState,
  showFeedsSpinner,
  hideFeedsSpinner,
  setSelectedArticleHighlight,
  setArticleReadState,
  updateNavSelection,
  setFolderOpenState,
  updateAllFeedsUnreadBadge,
  totalUnreadCount,
  setFeedUnreadBadge,
  setFolderUnreadBadge,
  buildFoldersHtml,
  loadFeedIcons,
  prepareCachedIconsForHydration,
} from "./dom.js";
import { NavigationService, columns } from "./nav.js";
import { buildPath, parseRoute } from "./router.js";
import { createArticleContextReloader } from "./article-context.js";
import { escapeHtml, isSafeHttpUrl } from "./util.js";
import {
  initializeAuth,
  initSSOConfig,
  redirectToAuthProvider,
  isAuthenticated,
  hasSession,
  logout,
  fetchWithAuth,
} from "./pkce.js";

const APP_TITLE = "feedBox";

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
  filter: { isActive: false, term: "" },
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
  searchForm: document.getElementById("search-form"),
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
    markFeedRead: document.getElementById("trigger-feed-mark-read"),
    markAllRead: document.getElementById("trigger-all-mark-read"),
    openWebsite: document.getElementById("trigger-open-website"),
    addFolder: document.getElementById("trigger-folder-add"),
    editFolder: document.getElementById("trigger-folder-edit"),
    deleteFolder: document.getElementById("trigger-folder-delete"),
    markFolderRead: document.getElementById("trigger-folder-mark-read"),
    logout: document.getElementById("trigger-logout"),
  },
};

let lazyLoadObserver = null;
const navigationService = new NavigationService();
/** True when the current article URL was pushed (close uses history.back). */
let articleHistoryPushed = false;
/** @type {AbortController|null} */
let articlesAbortController = null;
let articlesRequestToken = 0;
const reloadArticlesForCurrentContext = createArticleContextReloader({
  pauseObserver: () => lazyLoadObserver?.pause(),
  resumeObserver: () => lazyLoadObserver?.resume(),
  clearReader: () => {
    state.selectedArticle = null;
    clearReaderView();
  },
  resetPagination,
  clearArticles,
  loadArticles,
  showError: showArticleLoadError,
});

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
    updateAllFeedsUnreadBadge(
      totalUnreadCount(cached.folders, cached.unfiledFeeds),
    );
    prepareCachedIconsForHydration();
    hideFeedsSpinner();
  } else {
    renderFoldersList(cached.folders, cached.unfiledFeeds);
    hideFeedsSpinner();
  }
  updateNavSelection(state.lastClickedItem.type, state.lastClickedItem.obj);
  return true;
}

function hydrateFeedIcons() {
  loadFeedIcons(fetchWithAuth);
}

window.addEventListener("DOMContentLoaded", async () => {
  restoreFolderCache();
  // Wire UI handlers before auth network calls so cache-painted folders
  // are clickable immediately (otherwise <details> only toggles).
  initEventListeners();
  registerDelegatedEvents();

  try {
    await initSSOConfig();
    await initializeAuth();

    if (!isAuthenticated()) {
      await redirectToAuthProvider();
      return;
    }

    lazyLoadObserver = setupScrollObserver();
    window.addEventListener("popstate", onPopState);

    // Resolve the folder tree before applying a deep link.
    await refreshFolders({ background: true });
    hydrateFeedIcons();
    await applyRouteFromLocation({ replaceHistory: true });
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
  dom.button.showAllFeeds.addEventListener("click", (e) => {
    if (e.target.closest(".tree-options")) return;
    selectAndLoadArticles(itemType.ALL, null);
  });
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
    if (websiteUrl && isSafeHttpUrl(websiteUrl)) {
      window.open(websiteUrl, "_blank", "noopener,noreferrer");
    }
  });
  dom.button.markAllRead?.addEventListener("click", async () => {
    await markAllAsRead();
  });
  dom.button.markFeedRead?.addEventListener("click", async () => {
    if (state.lastClickedItem.type === itemType.FEED && state.lastClickedItem.obj) {
      await markFeedAsRead(state.lastClickedItem.obj);
    }
  });
  dom.button.markFolderRead?.addEventListener("click", async () => {
    if (
      state.lastClickedItem.type === itemType.FOLDER &&
      state.lastClickedItem.obj
    ) {
      await markFolderAsRead(state.lastClickedItem.obj);
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
    closeReader();
  });
  if (dom.button.logout) {
    dom.button.logout.addEventListener("click", () => {
      logout();
    });
  }
  dom.searchForm?.addEventListener("submit", (e) => {
    e.preventDefault();
    submitSearch();
  });
}

/**
 * Run a server-side search for the current selection (or clear it when empty).
 */
async function submitSearch() {
  const term = dom.searchInput.value.trim();
  if (term === "") {
    if (!state.filter.isActive) return;
    clearSearchFilter();
  } else {
    state.filter.isActive = true;
    state.filter.term = term;
  }
  await reloadArticlesForCurrentContext();
}

function clearSearchFilter() {
  state.filter.isActive = false;
  state.filter.term = "";
  if (dom.searchInput) {
    dom.searchInput.value = "";
  }
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
      if (options.closest("#trigger-show-all-feeds")) {
        allFeedsContextMenu(e.clientX, e.clientY);
        return;
      }
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
    if (options.closest("#trigger-show-all-feeds")) {
      allFeedsContextMenu(rect.left, rect.bottom);
      return;
    }
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
      return true;
    }
  } else if (type === itemType.FOLDER && obj) {
    state.lastClickedItem.obj = findFolderById(obj.id);
    if (!state.lastClickedItem.obj) {
      state.lastClickedItem = { type: itemType.ALL, obj: null };
      return true;
    }
  }
  return false;
}

/**
 * @param {Folder[]} folders
 * @param {Feed[]} unfiledFeeds
 * @returns {{ html: string, selectionInvalidated: boolean }}
 */
function applyFolderTree(folders, unfiledFeeds) {
  state.folders = folders;
  state.unfiledFeeds = unfiledFeeds;
  const selectionInvalidated = resolveLastClickedItem();
  const html = renderFoldersList(folders, unfiledFeeds);
  updateNavSelection(state.lastClickedItem.type, state.lastClickedItem.obj);
  hydrateFeedIcons();
  return { html, selectionInvalidated };
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

    let selectionInvalidated = false;
    if (changed || !hadCache) {
      const { html, selectionInvalidated: invalidated } = applyFolderTree(
        tree.folders,
        tree.unfiledFeeds,
      );
      selectionInvalidated = invalidated;
      saveFolderTreeCache(tree.folders, tree.unfiledFeeds, html);
    } else {
      state.folders = tree.folders;
      state.unfiledFeeds = tree.unfiledFeeds;
      selectionInvalidated = resolveLastClickedItem();
      updateNavSelection(state.lastClickedItem.type, state.lastClickedItem.obj);
    }
    state.restoredFromCache = true;
    if (selectionInvalidated) {
      await selectAndLoadArticles(itemType.ALL, null, {
        syncUrl: "replace",
        column: columns.FEEDS,
      });
    }
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

/** @type {Map<number, object>} full articles fetched for the reader */
const articleDetailCache = new Map();
const ARTICLE_DETAIL_CACHE_MAX = 24;
/** @type {ReturnType<typeof setTimeout>|null} */
let folderTreeCacheSaveTimer = null;

/**
 * @param {object} article
 */
function cacheArticleDetail(article) {
  if (!article?.id) return;
  articleDetailCache.set(article.id, article);
  while (articleDetailCache.size > ARTICLE_DETAIL_CACHE_MAX) {
    const oldest = articleDetailCache.keys().next().value;
    articleDetailCache.delete(oldest);
  }
}

/**
 * List rows omit content; resolve a full article for the reader.
 * @param {object} summary
 * @returns {Promise<object>}
 */
async function resolveFullArticle(summary) {
  if (summary?.content || summary?.description) {
    cacheArticleDetail(summary);
    return summary;
  }
  const cached = articleDetailCache.get(summary.id);
  if (cached) return cached;
  const full = await dataService.getArticle(summary.id);
  if (summary.read) full.read = true;
  cacheArticleDetail(full);
  return full;
}

/**
 * Prefetch neighboring list rows for snappy prev/next navigation.
 * @param {number} articleId
 */
function prefetchAdjacentArticles(articleId) {
  const idx = state.articles.findIndex((a) => a.id === articleId);
  if (idx < 0) return;
  for (const offset of [-1, 1]) {
    const neighbor = state.articles[idx + offset];
    if (!neighbor || articleDetailCache.has(neighbor.id)) continue;
    void dataService
      .getArticle(neighbor.id)
      .then((full) => cacheArticleDetail(full))
      .catch(() => {});
  }
}

/**
 * @param {object} article list summary or full article
 */
async function loadArticle(article) {
  const listed = state.articles.find((a) => a.id === article.id);
  if (!listed && !article?.id) {
    console.error("Article not found:", article);
    return;
  }
  const summary = listed ?? article;
  try {
    const full = await resolveFullArticle(summary);
    state.selectedArticle = full;
    setSelectedArticleHighlight(full.id);
    renderReaderView(full);
    document.querySelector("#reader .column").scrollTop = 0;
    void markArticleAsRead(summary);
    prefetchAdjacentArticles(full.id);
  } catch (error) {
    console.error("[app] Failed to open article:", error);
  }
}

/**
 * Re-render the sidebar from current state and refresh the folder-tree cache.
 * Prefer {@link syncUnreadBadgesFromState} for badge-only updates.
 */
function rerenderFolderTree() {
  const html = renderFoldersList(state.folders, state.unfiledFeeds);
  saveFolderTreeCache(state.folders, state.unfiledFeeds, html);
  updateNavSelection(state.lastClickedItem.type, state.lastClickedItem.obj);
  hydrateFeedIcons();
}

/**
 * Debounced localStorage write of the folder tree (HTML + data) without
 * rebuilding the live DOM.
 */
function scheduleFolderTreeCacheSave() {
  if (folderTreeCacheSaveTimer != null) {
    clearTimeout(folderTreeCacheSaveTimer);
  }
  folderTreeCacheSaveTimer = setTimeout(() => {
    folderTreeCacheSaveTimer = null;
    const html = buildFoldersHtml(state.folders, state.unfiledFeeds);
    saveFolderTreeCache(state.folders, state.unfiledFeeds, html);
  }, 400);
}

/**
 * Update every visible unread badge from state without re-rendering the tree.
 */
function syncUnreadBadgesFromState() {
  for (const folder of state.folders) {
    let folderUnread = 0;
    for (const feed of folder.feeds ?? []) {
      const n = Number(feed.unreadCount) || 0;
      folderUnread += n;
      setFeedUnreadBadge(feed.id, n);
    }
    setFolderUnreadBadge(folder.id, folderUnread);
  }
  for (const feed of state.unfiledFeeds) {
    setFeedUnreadBadge(feed.id, Number(feed.unreadCount) || 0);
  }
  updateAllFeedsUnreadBadge(totalUnreadCount(state.folders, state.unfiledFeeds));
  scheduleFolderTreeCacheSave();
}

/**
 * @param {number} feedId
 * @returns {Feed|null}
 */
function findFeedMutable(feedId) {
  for (const folder of state.folders) {
    const feed = (folder.feeds ?? []).find((f) => f.id === feedId);
    if (feed) return feed;
  }
  return state.unfiledFeeds.find((f) => f.id === feedId) ?? null;
}

/**
 * Decrement a feed's unread badge after marking one article read.
 * @param {number} feedId
 */
function decrementFeedUnreadCount(feedId) {
  const feed = findFeedMutable(feedId);
  if (!feed || !feed.unreadCount) return;
  feed.unreadCount = Math.max(0, (Number(feed.unreadCount) || 0) - 1);
  setFeedUnreadBadge(feedId, feed.unreadCount);
  const folder = state.folders.find((f) =>
    (f.feeds ?? []).some((x) => x.id === feedId),
  );
  if (folder) {
    const folderUnread = (folder.feeds ?? []).reduce(
      (sum, f) => sum + (Number(f.unreadCount) || 0),
      0,
    );
    setFolderUnreadBadge(folder.id, folderUnread);
  }
  updateAllFeedsUnreadBadge(totalUnreadCount(state.folders, state.unfiledFeeds));
  scheduleFolderTreeCacheSave();
}

/**
 * Mark a single article read on the server and update local UI.
 * @param {Article} article
 */
async function markArticleAsRead(article) {
  if (!article || article.read) return;

  const previousCount = findFeedMutable(article.feedId)?.unreadCount ?? 0;
  article.read = true;
  if (state.selectedArticle?.id === article.id) {
    state.selectedArticle.read = true;
  }
  const cached = articleDetailCache.get(article.id);
  if (cached) cached.read = true;
  setArticleReadState(article.id, true);
  decrementFeedUnreadCount(article.feedId);

  try {
    await dataService.markArticleRead(article.id);
  } catch (error) {
    console.error("[app] Failed to mark article read:", error);
    article.read = false;
    if (state.selectedArticle?.id === article.id) {
      state.selectedArticle.read = false;
    }
    if (cached) cached.read = false;
    setArticleReadState(article.id, false);
    const feed = findFeedMutable(article.feedId);
    if (feed) {
      feed.unreadCount = previousCount;
      syncUnreadBadgesFromState();
    }
  }
}

async function markAllAsRead() {
  const previousCounts = [];
  for (const folder of state.folders) {
    for (const feed of folder.feeds ?? []) {
      previousCounts.push({ feed, unreadCount: Number(feed.unreadCount) || 0 });
      feed.unreadCount = 0;
    }
  }
  for (const feed of state.unfiledFeeds) {
    previousCounts.push({ feed, unreadCount: Number(feed.unreadCount) || 0 });
    feed.unreadCount = 0;
  }
  const previousArticleReads = state.articles.map((a) => ({
    article: a,
    read: !!a.read,
  }));

  for (const article of state.articles) {
    article.read = true;
    setArticleReadState(article.id, true);
  }
  if (state.selectedArticle) state.selectedArticle.read = true;
  syncUnreadBadgesFromState();

  try {
    await dataService.markAllArticlesRead();
  } catch (error) {
    console.error("[app] Failed to mark all articles read:", error);
    for (const { feed, unreadCount } of previousCounts) {
      feed.unreadCount = unreadCount;
    }
    for (const { article, read } of previousArticleReads) {
      article.read = read;
      setArticleReadState(article.id, read);
    }
    syncUnreadBadgesFromState();
  }
}

/**
 * @param {Feed} feed
 */
async function markFeedAsRead(feed) {
  if (!feed) return;
  const target = findFeedMutable(feed.id) ?? feed;
  const previousCount = Number(target.unreadCount) || 0;
  const previousArticleReads = state.articles
    .filter((a) => a.feedId === feed.id)
    .map((a) => ({ article: a, read: !!a.read }));

  target.unreadCount = 0;
  for (const article of state.articles) {
    if (article.feedId === feed.id) {
      article.read = true;
      setArticleReadState(article.id, true);
    }
  }
  if (state.selectedArticle?.feedId === feed.id) {
    state.selectedArticle.read = true;
  }
  syncUnreadBadgesFromState();

  try {
    await dataService.markFeedRead(feed.id);
  } catch (error) {
    console.error("[app] Failed to mark feed read:", error);
    target.unreadCount = previousCount;
    for (const { article, read } of previousArticleReads) {
      article.read = read;
      setArticleReadState(article.id, read);
    }
    syncUnreadBadgesFromState();
  }
}

/**
 * @param {Folder} folder
 */
async function markFolderAsRead(folder) {
  if (!folder) return;
  const live = state.folders.find((f) => f.id === folder.id) ?? folder;
  const feeds = live.feeds ?? [];
  const previousCounts = feeds.map((f) => ({
    feed: f,
    unreadCount: Number(f.unreadCount) || 0,
  }));
  const feedIds = new Set(feeds.map((f) => f.id));
  const previousArticleReads = state.articles
    .filter((a) => feedIds.has(a.feedId))
    .map((a) => ({ article: a, read: !!a.read }));

  for (const feed of feeds) {
    feed.unreadCount = 0;
  }
  for (const article of state.articles) {
    if (feedIds.has(article.feedId)) {
      article.read = true;
      setArticleReadState(article.id, true);
    }
  }
  if (state.selectedArticle && feedIds.has(state.selectedArticle.feedId)) {
    state.selectedArticle.read = true;
  }
  syncUnreadBadgesFromState();

  try {
    await dataService.markFolderRead(folder.id);
  } catch (error) {
    console.error("[app] Failed to mark folder read:", error);
    for (const { feed, unreadCount } of previousCounts) {
      feed.unreadCount = unreadCount;
    }
    for (const { article, read } of previousArticleReads) {
      article.read = read;
      setArticleReadState(article.id, read);
    }
    syncUnreadBadgesFromState();
  }
}

function setupScrollObserver() {
  const sentinel = document.getElementById("articles-sentinel");

  const observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0];
      if (!entry) return;
      if (
        entry.isIntersecting &&
        hasSession() &&
        !state.status.isLoadingArticles &&
        state.status.hasMoreArticles
      ) {
        void loadArticles().catch(showArticleLoadError);
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

async function navigateArticle(direction) {
  if (!state.selectedArticle) return;

  const idx = state.articles.findIndex(
    (a) => a.id === state.selectedArticle.id,
  );
  if (idx === -1) return;

  const nextIdx = idx + direction;
  if (nextIdx >= 0 && nextIdx < state.articles.length) {
    const next = state.articles[nextIdx];
    await loadArticle(next);
    replaceAppUrl(pathForSelection(next.id), columns.READER);
    updateDocumentTitle();
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

function allFeedsContextMenu(x, y) {
  state.lastClickedItem.type = itemType.ALL;
  state.lastClickedItem.obj = null;
  showContextMenuGroup("context-all", x, y);
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

/**
 * @param {string} type
 * @param {object | null} obj
 * @param {{ syncUrl?: 'push' | 'replace' | 'none', column?: string }} [options]
 */
async function selectAndLoadArticles(type, obj, options = {}) {
  const { syncUrl = "push", column = columns.ARTICLES } = options;
  clearSearchFilter();
  state.lastClickedItem = { type, obj };
  updateNavSelection(type, obj);
  articleHistoryPushed = false;

  const path = pathForSelection(null);
  if (syncUrl === "push") {
    pushAppUrl(path, column);
  } else if (syncUrl === "replace") {
    replaceAppUrl(path, column);
  } else {
    navigationService.navigateTo(column);
  }

  await reloadArticlesForCurrentContext();
  updateDocumentTitle();
}

async function showArticleLoadError(error) {
  console.error("[app] Failed to load articles:", error);
  await modal.show({
    title: "Could not load articles",
    content: "Please check your connection and try again.",
    type: "alert",
  });
}

function articleClickListener(article) {
  void loadArticle(article);
  pushAppUrl(pathForSelection(article.id), columns.READER);
  articleHistoryPushed = true;
  updateDocumentTitle();
}

function closeReader() {
  if (articleHistoryPushed) {
    articleHistoryPushed = false;
    history.back();
    return;
  }
  state.selectedArticle = null;
  clearReaderView();
  replaceAppUrl(pathForSelection(null), columns.ARTICLES);
  updateDocumentTitle();
}

function isAuthCallback() {
  const params = new URLSearchParams(window.location.search);
  return (
    params.has("code") || params.has("state") || params.has("session_state")
  );
}

/**
 * @param {number | null} [articleId]
 * @returns {string}
 */
function pathForSelection(articleId = null) {
  const { type, obj } = state.lastClickedItem;
  if (type === itemType.FEED && obj) {
    return buildPath({
      context: "feed",
      contextId: obj.id,
      articleId,
    });
  }
  if (type === itemType.FOLDER && obj) {
    return buildPath({
      context: "folder",
      contextId: obj.id,
      articleId,
    });
  }
  return buildPath({ articleId });
}

/**
 * @param {string} path
 * @param {string} column
 */
function pushAppUrl(path, column) {
  if (isAuthCallback()) return;
  navigationService.navigateTo(column);
  if (
    window.location.pathname === path &&
    history.state?.column === column
  ) {
    return;
  }
  history.pushState({ column }, "", path);
}

/**
 * @param {string} path
 * @param {string} column
 */
function replaceAppUrl(path, column) {
  if (isAuthCallback()) return;
  navigationService.navigateTo(column);
  history.replaceState({ column }, "", path);
}

/**
 * @param {import('./router.js').AppRoute} route
 * @returns {string}
 */
function columnForRoute(route) {
  if (route.articleId != null) return columns.READER;
  if (route.context === "all") return columns.FEEDS;
  return columns.ARTICLES;
}

function updateDocumentTitle() {
  if (state.selectedArticle?.title) {
    document.title = `${state.selectedArticle.title} · ${APP_TITLE}`;
    return;
  }
  const { type, obj } = state.lastClickedItem;
  if ((type === itemType.FEED || type === itemType.FOLDER) && obj?.name) {
    document.title = `${obj.name} · ${APP_TITLE}`;
    return;
  }
  document.title = APP_TITLE;
}

/**
 * @param {object} article
 * @param {string} type
 * @param {object | null} obj
 */
function articleBelongsToContext(article, type, obj) {
  if (!article) return false;
  if (type === itemType.ALL) return true;
  if (type === itemType.FEED && obj) return article.feedId === obj.id;
  if (type === itemType.FOLDER && obj) {
    return (obj.feeds ?? []).some((feed) => feed.id === article.feedId);
  }
  return false;
}

/**
 * @param {number} articleId
 * @returns {Promise<boolean>}
 */
async function openArticleById(articleId) {
  let summary = state.articles.find((a) => a.id === articleId) ?? null;
  let full = null;
  if (!summary) {
    try {
      full = await dataService.getArticle(articleId);
      cacheArticleDetail(full);
      summary = full;
    } catch (error) {
      if (error.status === 404 || error.cause?.status === 404) {
        return false;
      }
      throw error;
    }
  }

  if (
    !articleBelongsToContext(
      summary,
      state.lastClickedItem.type,
      state.lastClickedItem.obj,
    )
  ) {
    return false;
  }

  const listed = state.articles.find((a) => a.id === summary.id);
  await loadArticle(listed ?? summary);
  return true;
}

/**
 * Apply the current pathname to app state (selection, articles, reader).
 * @param {{ replaceHistory?: boolean }} [options]
 */
async function applyRouteFromLocation({ replaceHistory = false } = {}) {
  if (isAuthCallback()) return;

  let route = parseRoute(window.location.pathname);
  if (!route.valid) {
    route = { valid: true, context: "all", contextId: null, articleId: null };
    replaceAppUrl("/", columns.FEEDS);
  }

  let type = itemType.ALL;
  let obj = null;
  if (route.context === "feed") {
    obj = findFeedById(route.contextId);
    if (!obj) {
      await selectAndLoadArticles(itemType.ALL, null, {
        syncUrl: "replace",
        column: columns.FEEDS,
      });
      return;
    }
    type = itemType.FEED;
  } else if (route.context === "folder") {
    obj = findFolderById(route.contextId);
    if (!obj) {
      await selectAndLoadArticles(itemType.ALL, null, {
        syncUrl: "replace",
        column: columns.FEEDS,
      });
      return;
    }
    type = itemType.FOLDER;
  }

  clearSearchFilter();
  state.lastClickedItem = { type, obj };
  updateNavSelection(type, obj);
  articleHistoryPushed = false;

  await reloadArticlesForCurrentContext();

  let column = columnForRoute(route);
  if (route.articleId != null) {
    const opened = await openArticleById(route.articleId);
    if (!opened) {
      state.selectedArticle = null;
      clearReaderView();
      column = type === itemType.ALL ? columns.FEEDS : columns.ARTICLES;
      replaceAppUrl(pathForSelection(null), column);
      updateDocumentTitle();
      return;
    }
    column = columns.READER;
  } else {
    state.selectedArticle = null;
    clearReaderView();
  }

  if (replaceHistory) {
    replaceAppUrl(pathForSelection(state.selectedArticle?.id ?? null), column);
  } else {
    navigationService.navigateTo(column);
  }
  updateDocumentTitle();
}

/**
 * @param {PopStateEvent} event
 */
async function onPopState(event) {
  if (isAuthCallback()) return;
  articleHistoryPushed = false;

  const route = parseRoute(window.location.pathname);
  if (!route.valid) {
    await applyRouteFromLocation({ replaceHistory: true });
    return;
  }

  const sameContext =
    (route.context === "all" &&
      state.lastClickedItem.type === itemType.ALL) ||
    (route.context === "feed" &&
      state.lastClickedItem.type === itemType.FEED &&
      state.lastClickedItem.obj?.id === route.contextId) ||
    (route.context === "folder" &&
      state.lastClickedItem.type === itemType.FOLDER &&
      state.lastClickedItem.obj?.id === route.contextId);

  const currentArticleId = state.selectedArticle?.id ?? null;
  const routeArticleId = route.articleId ?? null;

  if (sameContext && currentArticleId === routeArticleId) {
    const column =
      event.state?.column && Object.values(columns).includes(event.state.column)
        ? event.state.column
        : columnForRoute(route);
    navigationService.navigateTo(column);
    updateDocumentTitle();
    return;
  }

  if (sameContext) {
    if (routeArticleId != null) {
      const opened = await openArticleById(routeArticleId);
      if (!opened) {
        replaceAppUrl(pathForSelection(null), columns.ARTICLES);
        state.selectedArticle = null;
        clearReaderView();
        updateDocumentTitle();
        return;
      }
      navigationService.navigateTo(columns.READER);
    } else {
      state.selectedArticle = null;
      clearReaderView();
      const column =
        event.state?.column && Object.values(columns).includes(event.state.column)
          ? event.state.column
          : columnForRoute(route);
      navigationService.navigateTo(column);
    }
    updateDocumentTitle();
    return;
  }

  await applyRouteFromLocation({ replaceHistory: false });
}

function clearArticles() {
  state.articles = [];
  articleDetailCache.clear();
  clearArticlesList();
  removeSkeletons();
  document.querySelector("#articles-list .column").scrollTop = 0;
}

function resetPagination() {
  state.pagination.id = null;
  state.pagination.published = null;
  state.status.hasMoreArticles = true;
}

/**
 * Run an async action; on failure show an alert modal.
 * @param {() => Promise<void>} action
 * @param {string|((error: Error) => string)} messageOrFn
 */
async function withErrorModal(action, messageOrFn) {
  try {
    await action();
  } catch (error) {
    console.error(error);
    const content =
      typeof messageOrFn === "function" ? messageOrFn(error) : messageOrFn;
    await modal.show({ title: "Error", content, type: "alert" });
  }
}

async function createFolder(folder) {
  await withErrorModal(async () => {
    await dataService.createFolder(folder);
    await refreshFolders({ background: false });
  }, "Error saving folder: " + folder.name);
}

async function editFolder(folder) {
  await withErrorModal(async () => {
    await dataService.updateFolder(folder);
    await refreshFolders({ background: false });
  }, "Error updating folder: " + folder.name);
}

async function deleteFolder(folder) {
  await withErrorModal(async () => {
    const wasSelected =
      state.lastClickedItem.type === itemType.FOLDER &&
      state.lastClickedItem.obj?.id === folder.id;
    await dataService.deleteFolder(folder.id);
    await refreshFolders({ background: false });
    if (wasSelected || !state.lastClickedItem.obj) {
      await selectAndLoadArticles(itemType.ALL, null, {
        syncUrl: "replace",
        column: columns.FEEDS,
      });
    }
  }, "Error deleting folder: " + folder.name);
}

async function createFeed(feed) {
  await withErrorModal(async () => {
    await dataService.createFeed(feed);
    await refreshFolders({ background: false });
  }, (error) => {
    const isDuplicate = error.status === 409 || error.cause?.status === 409;
    return isDuplicate ? "This feed already exists." : "Error saving feed.";
  });
}

async function editFeed(feed) {
  await withErrorModal(async () => {
    await dataService.updateFeed(feed);
    await refreshFolders({ background: false });
  }, "Error updating feed: " + feed.feedUrl);
}

async function deleteFeed(feed) {
  await withErrorModal(async () => {
    const wasSelected =
      state.lastClickedItem.type === itemType.FEED &&
      state.lastClickedItem.obj?.id === feed.id;
    await dataService.deleteFeed(feed.id);
    await refreshFolders({ background: false });
    if (wasSelected || !state.lastClickedItem.obj) {
      await selectAndLoadArticles(itemType.ALL, null, {
        syncUrl: "replace",
        column: columns.FEEDS,
      });
    } else {
      state.articles = state.articles.filter(
        (article) => article.feedId !== feed.id,
      );
      replaceArticlesList(state.articles);
    }
  }, "Error deleting feed: " + feed.name);
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
    await refreshFolders({ background: false });
    await selectAndLoadArticles(itemType.ALL, null, {
      syncUrl: "replace",
      column: columns.ARTICLES,
    });
  } finally {
    dom.refreshSpinner.classList.remove("spinner");
    state.status.isRefreshing = false;
  }
}

async function loadArticles() {
  if (!hasSession()) return;

  articlesAbortController?.abort();
  const controller = new AbortController();
  articlesAbortController = controller;
  const requestToken = ++articlesRequestToken;

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
    if (state.filter.isActive && state.filter.term) {
      params.q = state.filter.term;
    }

    const newArticles = await dataService.loadArticles(params, {
      signal: controller.signal,
    });

    if (requestToken !== articlesRequestToken) return;

    removeSkeletons();

    if (!newArticles || newArticles.length === 0) {
      state.status.hasMoreArticles = false;
      if (state.articles.length === 0) {
        const emptyMessage = state.filter.isActive
          ? `No articles match "${state.filter.term}"`
          : undefined;
        renderEmptyState(emptyMessage);
      }
      return;
    }

    state.articles.push(...newArticles);
    state.status.hasMoreArticles = newArticles.length >= ARTICLES_PAGE_SIZE;

    const lastArticle = newArticles[newArticles.length - 1];
    state.pagination.id = lastArticle.id;
    state.pagination.published = lastArticle.published;

    appendArticlesList(newArticles, state.selectedArticle?.id ?? null);
    scheduleVisiblePageLoad();
  } catch (error) {
    if (error.name === "AbortError" || error.cause?.name === "AbortError") {
      return;
    }
    throw error;
  } finally {
    if (requestToken === articlesRequestToken) {
      removeSkeletons();
      state.status.isLoadingArticles = false;
      articlesAbortController = null;
    }
  }
}

/**
 * IntersectionObserver only reports threshold crossings. When a page is too
 * short to push the sentinel outside the root margin, request another page
 * after layout settles instead of waiting for an incidental scroll event.
 */
function scheduleVisiblePageLoad() {
  requestAnimationFrame(() => {
    const sentinel = document.getElementById("articles-sentinel");
    const root = document.querySelector("#articles-list .column");
    if (
      !sentinel ||
      !root ||
      !hasSession() ||
      state.status.isLoadingArticles ||
      !state.status.hasMoreArticles
    ) {
      return;
    }

    const sentinelRect = sentinel.getBoundingClientRect();
    const rootRect = root.getBoundingClientRect();
    const rootMargin = 325;
    const isWithinRootMargin =
      sentinelRect.bottom >= rootRect.top - rootMargin &&
      sentinelRect.top <= rootRect.bottom + rootMargin;

    if (isWithinRootMargin) {
      void loadArticles().catch(showArticleLoadError);
    }
  });
}
