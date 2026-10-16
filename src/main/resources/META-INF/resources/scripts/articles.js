"use strict";

import { dataService } from "./data.js";
import { modal } from "./modal.js";
import {
  appendArticlesList,
  clearArticlesList,
  removeSkeletons,
  renderSkeletons,
  renderEmptyState,
  setArticleReadState,
  updateNavSelection,
} from "./dom.js";
import {
  state,
  itemType,
  dom,
  ARTICLES_PAGE_SIZE,
  ARTICLES_LIST_MAX,
  articleDetailCache,
  articlesAbortController,
  setArticlesAbortController,
  articlesRequestToken,
  nextArticlesRequestToken,
  reloadArticlesForCurrentContext,
  navigationService,
  pushAppUrl,
  replaceAppUrl,
  pathForSelection,
  updateDocumentTitle,
  setArticleHistoryPushed,
  columns,
} from "./state.js";
import {
  findFeedMutable,
  decrementFeedUnreadCount,
  syncUnreadBadgesFromState,
} from "./feeds.js";

export function clearSearchFilter() {
  state.filter.isActive = false;
  state.filter.term = "";
  if (dom.searchInput) {
    dom.searchInput.value = "";
  }
}

/**
 * Run a server-side search for the current selection (or clear it when empty).
 */
export async function submitSearch() {
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

export function clearArticles() {
  state.articles = [];
  articleDetailCache.clear();
  clearArticlesList();
  removeSkeletons();
  document.querySelector("#articles-list .column").scrollTop = 0;
}

export function resetPagination() {
  state.pagination.id = null;
  state.pagination.published = null;
  state.status.hasMoreArticles = true;
}

export async function showArticleLoadError(error) {
  console.error("[app] Failed to load articles:", error);
  await modal.show({
    title: "Could not load articles",
    content: "Please check your connection and try again.",
    type: "alert",
  });
}

/**
 * @param {string} type
 * @param {object | null} obj
 * @param {{ syncUrl?: 'push' | 'replace' | 'none', column?: string }} [options]
 */
export async function selectAndLoadArticles(type, obj, options = {}) {
  const { syncUrl = "push", column = columns.ARTICLES } = options;
  clearSearchFilter();
  state.lastClickedItem = { type, obj };
  updateNavSelection(type, obj);
  setArticleHistoryPushed(false);

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

/**
 * Mark a single article read on the server and update local UI.
 * @param {Article} article
 */
export async function markArticleAsRead(article) {
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

export async function markAllAsRead() {
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
export async function markFeedAsRead(feed) {
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
export async function markFolderAsRead(folder) {
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

export function setupScrollObserver() {
  const sentinel = document.getElementById("articles-sentinel");

  const observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0];
      if (!entry) return;
      if (
        entry.isIntersecting &&
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

export async function loadArticles() {
  articlesAbortController?.abort();
  const controller = new AbortController();
  setArticlesAbortController(controller);
  const requestToken = nextArticlesRequestToken();

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
    state.status.hasMoreArticles =
      newArticles.length >= ARTICLES_PAGE_SIZE &&
      state.articles.length < ARTICLES_LIST_MAX;

    const lastArticle = newArticles[newArticles.length - 1];
    state.pagination.id = lastArticle.id;
    state.pagination.published = lastArticle.published;

    appendArticlesList(newArticles, state.selectedArticle?.id ?? null);
    if (state.status.hasMoreArticles) {
      scheduleVisiblePageLoad();
    }
  } catch (error) {
    if (error.name === "AbortError" || error.cause?.name === "AbortError") {
      return;
    }
    throw error;
  } finally {
    if (requestToken === articlesRequestToken) {
      removeSkeletons();
      state.status.isLoadingArticles = false;
      setArticlesAbortController(null);
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

/**
 * @param {(article: object) => void} onArticleActivate
 */
export function registerArticlesDelegatedEvents(onArticleActivate) {
  const articlesList = document.getElementById("articles-list");

  articlesList?.addEventListener("click", (e) => {
    const articleEl = e.target.closest(".article[data-article-id]");
    if (!articleEl || !articlesList.contains(articleEl)) return;
    const article = state.articles.find(
      (a) => a.id === Number(articleEl.dataset.articleId),
    );
    if (article) onArticleActivate(article);
  });

  articlesList?.addEventListener("keydown", (e) => {
    if (e.key !== "Enter" && e.key !== " ") return;
    const articleEl = e.target.closest(".article[data-article-id]");
    if (!articleEl || !articlesList.contains(articleEl)) return;
    e.preventDefault();
    const article = state.articles.find(
      (a) => a.id === Number(articleEl.dataset.articleId),
    );
    if (article) onArticleActivate(article);
  });
}
