"use strict";

import { isConnectivityFailure } from "./connectivity.js";
import { dataService } from "./data.js";
import { modal } from "./modal.js";
import { toast } from "./toast.js";
import {
  appendArticlesList,
  clearArticlesList,
  clearListFooter,
  removeSkeletons,
  renderSkeletons,
  renderEmptyState,
  renderListFooter,
  setArticleReadState,
  setArticleSavedState,
  syncReaderSaveButton,
  updateNavSelection,
  updateSavedBadge,
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
  refreshFolders,
  refreshFeeds,
  importFeeds,
  showAddedFeed,
} from "./feeds.js";
import { showAddFeedDialog } from "./dialog.js";
import { contextDisplayName, updateSearchChrome } from "./search-ui.js";

function hasAnyFeeds() {
  if ((state.unfiledFeeds ?? []).length > 0) return true;
  return (state.folders ?? []).some((folder) => (folder.feeds ?? []).length > 0);
}

const NEW_ARTICLES_CHECK_THROTTLE_MS = 5 * 60 * 1000;
let lastNewArticlesCheckAt = 0;

export function clearSearchFilter() {
  state.filter.isActive = false;
  state.filter.term = "";
  if (dom.searchInput) {
    dom.searchInput.value = "";
  }
  updateSearchChrome();
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
  updateSearchChrome();
  await reloadArticlesForCurrentContext();
}

export function clearArticles() {
  state.articles = [];
  state.seenMaxArticleId = 0;
  hideNewArticlesPill();
  articleDetailCache.clear();
  clearArticlesList();
  clearListFooter();
  removeSkeletons();
  document.querySelector("#articles-list .column").scrollTop = 0;
}

/** Max latestArticleId across the feeds in the current list context. */
function latestArticleIdForCurrentContext() {
  const { type, obj } = state.lastClickedItem;
  if (type === itemType.FEED && obj) {
    const live = findFeedMutable(obj.id) ?? obj;
    return Number(live.latestArticleId) || 0;
  }
  if (type === itemType.FOLDER && obj) {
    const folder = state.folders.find((f) => f.id === obj.id) ?? obj;
    return maxLatestArticleId(folder.feeds ?? []);
  }
  return maxLatestArticleId(allFeedsInState());
}

/**
 * Raise {@link state.seenMaxArticleId} from the open list and feed
 * {@code latestArticleId} values. Call only after a list load — not after a
 * background folder refresh, or the new-articles probe would go blind.
 */
export function updateSeenMaxArticleId() {
  let max = Number(state.seenMaxArticleId) || 0;
  for (const article of state.articles) {
    const id = Number(article.id) || 0;
    if (id > max) max = id;
  }
  max = Math.max(max, latestArticleIdForCurrentContext());
  state.seenMaxArticleId = max;
}

function showNewArticlesPill(count) {
  const pill = document.getElementById("new-articles-pill");
  if (!pill) return;
  const capped = count >= 1000;
  const labelCount = capped ? "1000+" : String(count);
  const noun = count === 1 ? "article" : "articles";
  pill.textContent = `${labelCount} new ${noun}`;
  pill.hidden = false;
}

export function hideNewArticlesPill() {
  const pill = document.getElementById("new-articles-pill");
  if (pill) pill.hidden = true;
}

/** Build the count API scope from the current sidebar selection. */
function newArticlesCountParams(sinceId) {
  const params = { sinceId };
  switch (state.lastClickedItem.type) {
    case itemType.FEED:
      if (!state.lastClickedItem.obj) return null;
      params.feed = state.lastClickedItem.obj.id;
      break;
    case itemType.FOLDER:
      if (!state.lastClickedItem.obj) return null;
      params.folder = state.lastClickedItem.obj.id;
      break;
    case itemType.UNREAD:
      params.view = "unread";
      break;
    case itemType.SAVED:
      params.view = "saved";
      break;
    case itemType.ALL:
    default:
      break;
  }
  return params;
}

/**
 * Ask the server how many articles arrived after {@link state.seenMaxArticleId}.
 * Shows the pill when the count is positive; never inserts into the open list.
 */
export async function checkForNewArticles() {
  const sinceId = Number(state.seenMaxArticleId) || 0;
  if (sinceId <= 0) return;
  const params = newArticlesCountParams(sinceId);
  if (!params) return;
  try {
    const result = await dataService.countArticles(params);
    const count = Number(result?.count) || 0;
    if (count > 0) {
      showNewArticlesPill(count);
    } else {
      hideNewArticlesPill();
    }
  } catch (error) {
    console.error("[app] Failed to count new articles:", error);
  }
}

/**
 * Refresh sidebar badges, then probe for new articles.
 * @param {{ force?: boolean }} [options] force skips the 5-minute throttle
 */
export async function probeNewArticles({ force = false } = {}) {
  const now = Date.now();
  if (!force && now - lastNewArticlesCheckAt < NEW_ARTICLES_CHECK_THROTTLE_MS) {
    return;
  }
  lastNewArticlesCheckAt = now;
  await refreshFolders({ background: true });
  await checkForNewArticles();
}

/** Wire the pill click and visibility-based new-articles probe. */
export function initNewArticlesProbe() {
  document
    .getElementById("new-articles-pill")
    ?.addEventListener("click", () => {
      hideNewArticlesPill();
      void reloadArticlesForCurrentContext({ keepReader: true });
    });
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState !== "visible") return;
    void probeNewArticles();
  });
}

export function resetPagination() {
  state.pagination.id = null;
  state.pagination.published = null;
  state.status.hasMoreArticles = true;
  state.status.listSoftCap = ARTICLES_LIST_MAX;
}

/**
 * Raise the soft cap and fetch the next page (used by the list footer and reader next).
 * @returns {Promise<boolean>} true when at least one new article was loaded
 */
export async function loadMoreArticles() {
  if (state.status.isLoadingArticles) return false;
  if (!state.status.hasMoreArticles) return false;
  const before = state.articles.length;
  if (before >= state.status.listSoftCap) {
    state.status.listSoftCap = before + ARTICLES_LIST_MAX;
  }
  clearListFooter();
  try {
    await loadArticles();
  } catch (error) {
    await showArticleLoadError(error);
    return false;
  }
  return state.articles.length > before;
}

export async function showArticleLoadError(error) {
  console.error("[app] Failed to load articles:", error);
  // Offline toast already covered this outage; don't stack a blocking dialog.
  if (isConnectivityFailure(error)) {
    return;
  }
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
  updateSearchChrome();
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
    toast("Couldn't mark as read. Check your connection.");
  }
}

const MARK_READ_UNDO_MS = 6000;
const MARK_READ_TOAST_ID = "mark-read";

/**
 * @typedef {{
 *   timer: ReturnType<typeof setTimeout>,
 *   commit: (opts?: { keepalive?: boolean }) => Promise<void>,
 *   rollback: () => void,
 * }} PendingMarkRead
 */

/** @type {PendingMarkRead | null} */
let pendingMarkRead = null;

/** Flush a deferred bulk mark-read before the page goes away. */
export function initMarkReadFlush() {
  window.addEventListener("pagehide", () => {
    flushPendingMarkRead({ keepalive: true });
  });
}

/**
 * @param {{ keepalive?: boolean }} [options]
 */
function flushPendingMarkRead({ keepalive = false } = {}) {
  if (!pendingMarkRead) return;
  const pending = pendingMarkRead;
  pendingMarkRead = null;
  clearTimeout(pending.timer);
  void pending.commit({ keepalive });
}

/**
 * Commit any prior deferred mark-read, then schedule a new one with Undo.
 * @param {{
 *   count: number,
 *   apply: () => void,
 *   rollback: () => void,
 *   commit: (opts?: { keepalive?: boolean }) => Promise<void>,
 * }} args
 */
function deferMarkRead({ count, apply, rollback, commit }) {
  flushPendingMarkRead();
  apply();
  syncUnreadBadgesFromState();

  const runCommit = async ({ keepalive = false } = {}) => {
    try {
      await commit({ keepalive });
    } catch (error) {
      console.error("[app] Failed to mark articles read:", error);
      rollback();
      syncUnreadBadgesFromState();
      toast("Couldn't mark as read. Check your connection.");
    }
  };

  const timer = setTimeout(() => {
    if (pendingMarkRead?.timer !== timer) return;
    pendingMarkRead = null;
    void runCommit();
  }, MARK_READ_UNDO_MS);

  pendingMarkRead = {
    timer,
    commit: runCommit,
    rollback,
  };

  toast(`Marked ${count} as read`, {
    action: "Undo",
    onAction: () => {
      if (!pendingMarkRead || pendingMarkRead.timer !== timer) return;
      clearTimeout(pendingMarkRead.timer);
      pendingMarkRead = null;
      rollback();
      syncUnreadBadgesFromState();
    },
    timeoutMs: MARK_READ_UNDO_MS,
    id: MARK_READ_TOAST_ID,
  });
}

/** @returns {Iterable<import("./types.js").Feed>} */
function allFeedsInState() {
  const feeds = [];
  for (const folder of state.folders) {
    for (const feed of folder.feeds ?? []) feeds.push(feed);
  }
  for (const feed of state.unfiledFeeds) feeds.push(feed);
  return feeds;
}

/** Max latestArticleId across the given feeds (0 when empty). */
function maxLatestArticleId(feeds) {
  let max = 0;
  for (const feed of feeds) {
    const id = Number(feed.latestArticleId) || 0;
    if (id > max) max = id;
  }
  return max;
}

export async function markAllAsRead() {
  const feeds = [...allFeedsInState()];
  const count = feeds.reduce((sum, f) => sum + (Number(f.unreadCount) || 0), 0);
  if (count === 0) return;

  const previousCounts = feeds.map((feed) => ({
    feed,
    unreadCount: Number(feed.unreadCount) || 0,
  }));
  const previousArticleReads = state.articles.map((a) => ({
    article: a,
    read: !!a.read,
  }));
  const selectedWasUnread = state.selectedArticle
    ? !state.selectedArticle.read
    : false;
  const throughId = maxLatestArticleId(feeds);

  deferMarkRead({
    count,
    apply: () => {
      for (const feed of feeds) feed.unreadCount = 0;
      for (const article of state.articles) {
        article.read = true;
        setArticleReadState(article.id, true);
      }
      if (state.selectedArticle) state.selectedArticle.read = true;
    },
    rollback: () => {
      for (const { feed, unreadCount } of previousCounts) {
        feed.unreadCount = unreadCount;
      }
      for (const { article, read } of previousArticleReads) {
        article.read = read;
        setArticleReadState(article.id, read);
      }
      if (state.selectedArticle && selectedWasUnread) {
        state.selectedArticle.read = false;
      }
    },
    commit: ({ keepalive } = {}) =>
      dataService.markAllArticlesRead(throughId, { keepalive }),
  });
}

/**
 * @param {import("./types.js").Feed} feed
 */
export async function markFeedAsRead(feed) {
  if (!feed) return;
  const target = findFeedMutable(feed.id) ?? feed;
  const count = Number(target.unreadCount) || 0;
  if (count === 0) return;

  const previousCount = count;
  const previousArticleReads = state.articles
    .filter((a) => a.feedId === feed.id)
    .map((a) => ({ article: a, read: !!a.read }));
  const selectedWasUnread =
    state.selectedArticle?.feedId === feed.id && !state.selectedArticle.read;
  const throughId = Number(target.latestArticleId) || 0;

  deferMarkRead({
    count,
    apply: () => {
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
    },
    rollback: () => {
      target.unreadCount = previousCount;
      for (const { article, read } of previousArticleReads) {
        article.read = read;
        setArticleReadState(article.id, read);
      }
      if (state.selectedArticle && selectedWasUnread) {
        state.selectedArticle.read = false;
      }
    },
    commit: ({ keepalive } = {}) =>
      dataService.markFeedRead(feed.id, throughId, { keepalive }),
  });
}

/**
 * @param {import("./types.js").Folder} folder
 */
export async function markFolderAsRead(folder) {
  if (!folder) return;
  const live = state.folders.find((f) => f.id === folder.id) ?? folder;
  const feeds = live.feeds ?? [];
  const count = feeds.reduce((sum, f) => sum + (Number(f.unreadCount) || 0), 0);
  if (count === 0) return;

  const previousCounts = feeds.map((f) => ({
    feed: f,
    unreadCount: Number(f.unreadCount) || 0,
  }));
  const feedIds = new Set(feeds.map((f) => f.id));
  const previousArticleReads = state.articles
    .filter((a) => feedIds.has(a.feedId))
    .map((a) => ({ article: a, read: !!a.read }));
  const selectedWasUnread =
    !!state.selectedArticle &&
    feedIds.has(state.selectedArticle.feedId) &&
    !state.selectedArticle.read;
  const throughId = maxLatestArticleId(feeds);

  deferMarkRead({
    count,
    apply: () => {
      for (const feed of feeds) feed.unreadCount = 0;
      for (const article of state.articles) {
        if (feedIds.has(article.feedId)) {
          article.read = true;
          setArticleReadState(article.id, true);
        }
      }
      if (state.selectedArticle && feedIds.has(state.selectedArticle.feedId)) {
        state.selectedArticle.read = true;
      }
    },
    rollback: () => {
      for (const { feed, unreadCount } of previousCounts) {
        feed.unreadCount = unreadCount;
      }
      for (const { article, read } of previousArticleReads) {
        article.read = read;
        setArticleReadState(article.id, read);
      }
      if (state.selectedArticle && selectedWasUnread) {
        state.selectedArticle.read = false;
      }
    },
    commit: ({ keepalive } = {}) =>
      dataService.markFolderRead(folder.id, throughId, { keepalive }),
  });
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
      case itemType.UNREAD:
        params.view = "unread";
        break;
      case itemType.SAVED:
        params.view = "saved";
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
      updateSeenMaxArticleId();
      if (state.articles.length === 0) {
        const scope = contextDisplayName(
          state.lastClickedItem.type,
          state.lastClickedItem.obj,
        );
        if (!state.filter.isActive && !hasAnyFeeds()) {
          renderEmptyState("Add a feed to get started", {
            actions: [
              {
                label: "Add a feed",
                onClick: () => {
                  void (async () => {
                    const created = await showAddFeedDialog(state.folders);
                    if (created) await showAddedFeed(created);
                  })();
                },
              },
              {
                label: "Import OPML",
                primary: false,
                onClick: () => importFeeds(),
              },
            ],
          });
        } else if (
          !state.filter.isActive &&
          state.lastClickedItem.type === itemType.FEED
        ) {
          renderEmptyState("No articles yet", {
            inlineAction: {
              label: "Refresh now",
              onClick: () => {
                void refreshFeeds();
              },
            },
          });
        } else if (state.filter.isActive) {
          renderEmptyState(
            `No articles match “${state.filter.term}” in ${scope}`,
          );
        } else if (state.lastClickedItem.type === itemType.UNREAD) {
          renderEmptyState("No unread articles");
        } else if (state.lastClickedItem.type === itemType.SAVED) {
          renderEmptyState("No saved articles");
        } else {
          renderEmptyState(`No articles in ${scope}`);
        }
      } else {
        renderListFooter({ kind: "caught-up" });
      }
      return;
    }

    state.articles.push(...newArticles);
    state.status.hasMoreArticles = newArticles.length >= ARTICLES_PAGE_SIZE;
    updateSeenMaxArticleId();

    const lastArticle = newArticles[newArticles.length - 1];
    state.pagination.id = lastArticle.id;
    state.pagination.published = lastArticle.published;

    appendArticlesList(newArticles, state.selectedArticle?.id ?? null);

    if (!state.status.hasMoreArticles) {
      renderListFooter({ kind: "caught-up" });
      return;
    }

    if (state.articles.length >= state.status.listSoftCap) {
      renderListFooter({
        kind: "load-more",
        count: state.articles.length,
        onLoadMore: () => {
          void loadMoreArticles();
        },
      });
      return;
    }

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
/**
 * @param {HTMLElement} root
 * @param {Element} target
 * @returns {import("./types.js").Article | undefined}
 */
function articleFromRow(root, target) {
  const articleEl = target.closest(".article[data-article-id]");
  if (!articleEl || !root.contains(articleEl)) return undefined;
  return state.articles.find((a) => a.id === Number(articleEl.dataset.articleId));
}

/**
 * Save or unsave. Does not open, close, or change the selected article's read state.
 * The on-screen row stays put when Saved is the current list.
 * @param {import("./types.js").Article} article
 */
export async function toggleArticleSaved(article) {
  if (!article) return;
  const next = !article.saved;
  const previousCount = state.savedCount ?? 0;
  applySavedLocally(article, next);
  const nextCount = Math.max(0, previousCount + (next ? 1 : -1));
  state.savedCount = nextCount;
  updateSavedBadge(nextCount);
  try {
    if (next) {
      await dataService.saveArticle(article.id);
    } else {
      await dataService.unsaveArticle(article.id);
    }
  } catch (error) {
    console.error("[app] Failed to update saved article:", error);
    applySavedLocally(article, !next);
    state.savedCount = previousCount;
    updateSavedBadge(previousCount);
    toast("Couldn't save. Check your connection.");
  }
}

/**
 * @param {import("./types.js").Article} article
 * @param {boolean} saved
 */
function applySavedLocally(article, saved) {
  article.saved = saved;
  if (state.selectedArticle?.id === article.id) {
    state.selectedArticle.saved = saved;
  }
  const cached = articleDetailCache.get(article.id);
  if (cached) cached.saved = saved;
  const listed = state.articles.find((a) => a.id === article.id);
  if (listed && listed !== article) listed.saved = saved;
  setArticleSavedState(article.id, saved);
  if (state.selectedArticle?.id === article.id) {
    syncReaderSaveButton(state.selectedArticle);
  }
}

/**
 * True for clicks that should keep the browser's default link behaviour.
 * @param {MouseEvent} e
 */
function isModifiedLinkClick(e) {
  return (
    e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey
  );
}

export function registerArticlesDelegatedEvents(onArticleActivate) {
  const articlesList = document.getElementById("articles-list");

  articlesList?.addEventListener("click", (e) => {
    const mark = e.target.closest(".article-mark");
    if (mark && articlesList.contains(mark)) {
      e.preventDefault();
      e.stopPropagation();
      const article = articleFromRow(articlesList, mark);
      if (article && !article.read) void markArticleAsRead(article);
      return;
    }
    const star = e.target.closest(".article-star");
    if (star && articlesList.contains(star)) {
      e.preventDefault();
      e.stopPropagation();
      const article = articleFromRow(articlesList, star);
      if (article) void toggleArticleSaved(article);
      return;
    }
    if (e.target.closest("button")) return;

    const link = e.target.closest("a.article-link");
    if (link && articlesList.contains(link)) {
      if (isModifiedLinkClick(e)) return;
      e.preventDefault();
      const article = articleFromRow(articlesList, link);
      if (article) onArticleActivate(article);
      return;
    }

    const article = articleFromRow(articlesList, e.target);
    if (article) onArticleActivate(article);
  });

  articlesList?.addEventListener("keydown", (e) => {
    if (e.key !== " ") return;
    if (e.target.closest("button")) return;
    const link = e.target.closest("a.article-link");
    if (!link || !articlesList.contains(link)) return;
    e.preventDefault();
    const article = articleFromRow(articlesList, link);
    if (article) onArticleActivate(article);
  });
}
