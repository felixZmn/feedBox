"use strict";

import { dataService } from "./data.js";
import {
  clearReaderView,
  renderReaderView,
  setSelectedArticleHighlight,
  updateNavSelection,
} from "./dom.js";
import { parseRoute } from "./router.js";
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
import { markArticleAsRead, clearSearchFilter } from "./articles.js";

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

export function articleClickListener(article) {
  void loadArticle(article);
  pushAppUrl(pathForSelection(article.id), columns.READER);
  setArticleHistoryPushed(true);
  updateDocumentTitle();
}

export function closeReader() {
  if (articleHistoryPushed) {
    setArticleHistoryPushed(false);
    history.back();
    return;
  }
  state.selectedArticle = null;
  clearReaderView();
  replaceAppUrl(pathForSelection(null), columns.ARTICLES);
  updateDocumentTitle();
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
export async function applyRouteFromLocation({ replaceHistory = false } = {}) {
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
      const { selectAndLoadArticles } = await import("./articles.js");
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
      const { selectAndLoadArticles } = await import("./articles.js");
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
  setArticleHistoryPushed(false);

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
export async function onPopState(event) {
  if (isAuthCallback()) return;
  setArticleHistoryPushed(false);

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
