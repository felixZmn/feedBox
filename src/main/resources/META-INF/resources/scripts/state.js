"use strict";

import { NavigationService, columns } from "./nav.js";
import { buildPath } from "./router.js";

export const APP_TITLE = "feedBox";

/** Must match backend ArticleRepository LIMIT */
export const ARTICLES_PAGE_SIZE = 25;

/** Soft cap for in-memory article list (infinite scroll stops beyond this). */
export const ARTICLES_LIST_MAX = 500;

export const itemType = Object.freeze({
  ALL: "",
  UNREAD: "unread",
  SAVED: "saved",
  FEED: "feed",
  FOLDER: "folder",
});

export const state = {
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
  savedCount: 0,
  lastClickedItem: { type: itemType.ALL, obj: null },
  restoredFromCache: false,
};

export const dom = {
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
    showUnread: document.getElementById("trigger-show-unread"),
    showSaved: document.getElementById("trigger-show-saved"),
    saveArticle: document.getElementById("trigger-save-article"),
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

export const navigationService = new NavigationService();

/** True when the current article URL was pushed (close uses history.back). */
export let articleHistoryPushed = false;

export function setArticleHistoryPushed(value) {
  articleHistoryPushed = value;
}

/** @type {AbortController|null} */
export let articlesAbortController = null;

export function setArticlesAbortController(controller) {
  articlesAbortController = controller;
}

export let articlesRequestToken = 0;

export function nextArticlesRequestToken() {
  return ++articlesRequestToken;
}

/** @type {Map<number, object>} full articles fetched for the reader */
export const articleDetailCache = new Map();

export const ARTICLE_DETAIL_CACHE_MAX = 24;

/** @type {ReturnType<typeof setTimeout>|null} */
export let folderTreeCacheSaveTimer = null;

export function setFolderTreeCacheSaveTimer(timer) {
  folderTreeCacheSaveTimer = timer;
}

/** @type {{ pause: () => void, resume: () => void } | null} */
export let lazyLoadObserver = null;

export function setLazyLoadObserver(observer) {
  lazyLoadObserver = observer;
}

/** @type {() => Promise<void>} */
export let reloadArticlesForCurrentContext = async () => {};

export function setReloadArticlesForCurrentContext(fn) {
  reloadArticlesForCurrentContext = fn;
}

export function isAuthCallback() {
  const params = new URLSearchParams(window.location.search);
  return (
    params.has("code") || params.has("state") || params.has("session_state")
  );
}

/**
 * @param {number | null} [articleId]
 * @returns {string}
 */
export function pathForSelection(articleId = null) {
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
  if (type === itemType.UNREAD) {
    return buildPath({ context: "unread", articleId });
  }
  if (type === itemType.SAVED) {
    return buildPath({ context: "saved", articleId });
  }
  return buildPath({ articleId });
}

/**
 * @param {string} path
 * @param {string} column
 */
export function pushAppUrl(path, column) {
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
export function replaceAppUrl(path, column) {
  if (isAuthCallback()) return;
  navigationService.navigateTo(column);
  history.replaceState({ column }, "", path);
}

/**
 * @param {import('./router.js').AppRoute} route
 * @returns {string}
 */
export function columnForRoute(route) {
  if (route.articleId != null) return columns.READER;
  if (route.context === "all") return columns.FEEDS;
  return columns.ARTICLES;
}

export function updateDocumentTitle() {
  if (state.selectedArticle?.title) {
    document.title = `${state.selectedArticle.title} · ${APP_TITLE}`;
    return;
  }
  const { type, obj } = state.lastClickedItem;
  if ((type === itemType.FEED || type === itemType.FOLDER) && obj?.name) {
    document.title = `${obj.name} · ${APP_TITLE}`;
    return;
  }
  if (type === itemType.UNREAD) {
    document.title = `Unread · ${APP_TITLE}`;
    return;
  }
  if (type === itemType.SAVED) {
    document.title = `Saved · ${APP_TITLE}`;
    return;
  }
  document.title = APP_TITLE;
}

export { columns };
