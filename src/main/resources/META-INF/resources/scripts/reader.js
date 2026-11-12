"use strict";

import { dataService } from "./data.js";
import {
  clearReaderView,
  renderReaderView,
  setReaderUnavailable,
  setSelectedArticleHighlight,
  updateNavSelection,
} from "./dom.js";
import { isConnectivityFailure, notifyOffline } from "./connectivity.js";
import { isSameContext, parseRoute, resolveRoute } from "./router.js";
import {
  state,
  itemType,
  articleDetailCache,
  ARTICLE_DETAIL_CACHE_MAX,
  articleHistoryPushed,
  setArticleHistoryPushed,
  reloadArticlesForCurrentContext,
  navigationService,
  pushAppUrl,
  replaceAppUrl,
  pathForSelection,
  columnForRoute,
  updateDocumentTitle,
  isAuthCallback,
  columns,
} from "./state.js";
import { findFeedById, findFolderById } from "./feeds.js";
import { markArticleAsRead, clearSearchFilter, toggleArticleSaved } from "./articles.js";

/**
 * Header star uses the same save toggle as the article row.
 */
export function registerSaveArticleButton() {
  document.getElementById("trigger-save-article")?.addEventListener("click", (event) => {
    event.preventDefault();
    event.stopPropagation();
    if (!state.selectedArticle) return;
    void toggleArticleSaved(state.selectedArticle);
  });
}

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
export async function loadArticle(article) {
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

export async function navigateArticle(direction) {
  if (!state.selectedArticle) return;

  let idx = state.articles.findIndex(
    (a) => a.id === state.selectedArticle.id,
  );
  if (idx === -1) return;

  let nextIdx = idx + direction;
  if (
    direction > 0 &&
    nextIdx >= state.articles.length &&
    state.status.hasMoreArticles
  ) {
    const { loadMoreArticles } = await import("./articles.js");
    const loaded = await loadMoreArticles();
    if (!loaded) return;
    idx = state.articles.findIndex(
      (a) => a.id === state.selectedArticle.id,
    );
    if (idx === -1) return;
    nextIdx = idx + direction;
  }

  if (nextIdx >= 0 && nextIdx < state.articles.length) {
    const next = state.articles[nextIdx];
    await loadArticle(next);
    replaceAppUrl(pathForSelection(next.id), columns.READER);
    updateDocumentTitle();
  }
}

export function articleClickListener(article) {
  void loadArticle(article);
  pushAppUrl(pathForSelection(article.id), columns.READER);
  setArticleHistoryPushed(true);
  updateDocumentTitle();
}

/**
 * @param {number | null | undefined} articleId
 */
function focusArticleRowLink(articleId) {
  if (articleId == null) return;
  document
    .querySelector(
      `#articles-container .article[data-article-id="${articleId}"] .article-link`,
    )
    ?.focus();
}

export function closeReader() {
  const articleId = state.selectedArticle?.id ?? null;
  if (articleHistoryPushed) {
    setArticleHistoryPushed(false);
    history.back();
    focusArticleRowLink(articleId);
    return;
  }
  state.selectedArticle = null;
  clearReaderView();
  replaceAppUrl(pathForSelection(null), columns.ARTICLES);
  updateDocumentTitle();
  focusArticleRowLink(articleId);
}

/**
 * @param {import('./router.js').AppRoute} route
 */
function placeFacts(route) {
  return {
    feedExists:
      route.context !== "feed" || findFeedById(route.contextId) != null,
    folderExists:
      route.context !== "folder" || findFolderById(route.contextId) != null,
    articleExists: true,
  };
}

/**
 * @param {ReturnType<typeof resolveRoute>} resolved
 */
function selectionFor(resolved) {
  if (resolved.context === "unread") return { type: itemType.UNREAD, obj: null };
  if (resolved.context === "saved") return { type: itemType.SAVED, obj: null };
  if (resolved.context === "feed") {
    return { type: itemType.FEED, obj: findFeedById(resolved.contextId) };
  }
  if (resolved.context === "folder") {
    return { type: itemType.FOLDER, obj: findFolderById(resolved.contextId) };
  }
  return { type: itemType.ALL, obj: null };
}

/** Replace the current entry with All Feeds and open no article. */
async function showReplacedHome() {
  const { selectAndLoadArticles } = await import("./articles.js");
  state.selectedArticle = null;
  clearReaderView();
  await selectAndLoadArticles(itemType.ALL, null, {
    syncUrl: "replace",
    column: columns.FEEDS,
  });
}

/**
 * @param {number} articleId
 * @returns {Promise<boolean>} false when the article does not exist
 */
async function openArticleById(articleId) {
  let summary = state.articles.find((a) => a.id === articleId) ?? null;
  if (!summary) {
    try {
      const full = await dataService.getArticle(articleId);
      cacheArticleDetail(full);
      summary = full;
    } catch (error) {
      if (error.status === 404 || error.cause?.status === 404) {
        return false;
      }
      throw error;
    }
  }

  if (!summary?.id) return false;
  const listed = state.articles.find((a) => a.id === summary.id);
  await loadArticle(listed ?? summary);
  return true;
}

/**
 * @param {unknown} error
 * @returns {boolean} true when the offline notice handled it
 */
function handledConnectivity(error) {
  if (!isConnectivityFailure(error)) return false;
  notifyOffline();
  return true;
}

/**
 * Apply the current pathname to app state (selection, articles, reader).
 * A resolved address is left as-is, including its query string. Only a missing
 * list or an unrecognized path replaces the history entry with All Feeds.
 */
export async function applyRouteFromLocation() {
  if (isAuthCallback()) return;

  const route = parseRoute(window.location.pathname);
  const resolved = resolveRoute(route, placeFacts(route));
  if (resolved.status === "replaced-home") {
    await showReplacedHome();
    return;
  }

  const { type, obj } = selectionFor(resolved);
  clearSearchFilter();
  state.lastClickedItem = { type, obj };
  updateNavSelection(type, obj);
  setArticleHistoryPushed(false);

  await reloadArticlesForCurrentContext();

  if (route.articleId == null) {
    state.selectedArticle = null;
    clearReaderView();
    navigationService.navigateTo(columnForRoute(route));
    updateDocumentTitle();
    return;
  }

  let opened = false;
  try {
    opened = await openArticleById(route.articleId);
  } catch (error) {
    if (handledConnectivity(error)) return;
    throw error;
  }

  if (!opened) {
    state.selectedArticle = null;
    clearReaderView();
    setReaderUnavailable(true);
    navigationService.navigateTo(columns.READER);
    updateDocumentTitle();
    return;
  }

  navigationService.navigateTo(columns.READER);
  updateDocumentTitle();
}

/**
 * @param {PopStateEvent} event
 */
export async function onPopState(event) {
  if (isAuthCallback()) return;
  setArticleHistoryPushed(false);

  const route = parseRoute(window.location.pathname);
  const resolved = resolveRoute(route, placeFacts(route));
  if (resolved.status === "replaced-home") {
    await applyRouteFromLocation();
    return;
  }

  const sameContext = isSameContext(route, state.lastClickedItem);
  if (!sameContext) {
    await applyRouteFromLocation();
    return;
  }

  const currentArticleId = state.selectedArticle?.id ?? null;
  const routeArticleId = route.articleId ?? null;

  if (sameContext && currentArticleId === routeArticleId) {
    if (routeArticleId == null) {
      state.selectedArticle = null;
      clearReaderView();
    }
    const column =
      event.state?.column && Object.values(columns).includes(event.state.column)
        ? event.state.column
        : columnForRoute(route);
    navigationService.navigateTo(column);
    updateDocumentTitle();
    return;
  }

  if (routeArticleId != null) {
    let opened = false;
    try {
      opened = await openArticleById(routeArticleId);
    } catch (error) {
      if (handledConnectivity(error)) return;
      throw error;
    }
    if (!opened) {
      state.selectedArticle = null;
      clearReaderView();
      setReaderUnavailable(true);
      navigationService.navigateTo(columns.READER);
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
}
