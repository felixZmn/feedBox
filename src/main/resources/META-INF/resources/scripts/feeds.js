"use strict";

import {
  dataService,
  buildFolderTree,
  loadFolderTreeCache,
  saveFolderTreeCache,
  folderTreesEqual,
} from "./data.js";
import { modal } from "./modal.js";
import {
  renderFoldersList,
  showFeedsSpinner,
  hideFeedsSpinner,
  updateNavSelection,
  updateSavedBadge,
  setFeedUnreadBadge,
  setFolderUnreadBadge,
  updateAllFeedsUnreadBadge,
  totalUnreadCount,
  loadFeedIcons,
  replaceArticlesList,
  setFolderOpenState,
} from "./dom.js";
import { html } from "./util.js";
import { apiFetch } from "./auth.js";
import {
  state,
  itemType,
  dom,
  folderTreeCacheSaveTimer,
  setFolderTreeCacheSaveTimer,
  columns,
} from "./state.js";

/**
 * Short sanitized summary of an OPML import job for the completion modal.
 * @param {{ total?: number, completed?: number, failed?: number, results?: Array<{name?: string, status?: string, reason?: string, xmlUrl?: string}> }} job
 */
function importJobSummaryHtml(job) {
  const total = job.total ?? 0;
  const ok = job.completed ?? 0;
  const failed = job.failed ?? 0;
  const results = Array.isArray(job.results) ? job.results : [];
  const failRows = results.filter((r) => r.status === "fail").slice(0, 8);
  const parts = [
    html`<p>Imported ${ok} of ${total} feeds${failed ? ` (${failed} failed)` : ""}.</p>`,
  ];
  if (failRows.length > 0) {
    parts.push(
      "<ul>" +
        failRows
          .map(
            (r) =>
              html`<li>${r.name || r.xmlUrl || "feed"}${r.reason ? ` — ${r.reason}` : ""}</li>`,
          )
          .join("") +
        "</ul>",
    );
  }
  if (failed > failRows.length) {
    parts.push(
      html`<p>Showing ${failRows.length} of ${failed} failures.</p>`,
    );
  }
  return parts.join("");
}

/**
 * Restore cached folders into state once. Skip DOM replace if the inline
 * injector already painted the renderer HTML.
 * @returns {boolean}
 */
export function restoreFolderCache() {
  const cached = loadFolderTreeCache();
  if (!cached) return false;

  state.folders = cached.folders;
  state.unfiledFeeds = cached.unfiledFeeds;
  state.restoredFromCache = true;
  renderFoldersList(cached.folders, cached.unfiledFeeds);
  hideFeedsSpinner();
  updateNavSelection(state.lastClickedItem.type, state.lastClickedItem.obj);
  return true;
}

export function hydrateFeedIcons() {
  loadFeedIcons(apiFetch);
}

export function findFeedById(id) {
  for (const folder of state.folders) {
    const feed = (folder.feeds ?? []).find((f) => f.id === id);
    if (feed) return feed;
  }
  return state.unfiledFeeds.find((f) => f.id === id) ?? null;
}

export function findFolderById(id) {
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
  renderFoldersList(folders, unfiledFeeds);
  updateNavSelection(state.lastClickedItem.type, state.lastClickedItem.obj);
  hydrateFeedIcons();
  return { selectionInvalidated };
}

async function selectAndLoadArticlesAfterInvalidation() {
  const { selectAndLoadArticles } = await import("./articles.js");
  await selectAndLoadArticles(itemType.ALL, null, {
    syncUrl: "replace",
    column: columns.FEEDS,
  });
}

async function refreshSavedCount() {
  try {
    const result = await dataService.loadSavedCount();
    const count = Number(result?.count) || 0;
    state.savedCount = count;
    updateSavedBadge(count);
  } catch (error) {
    console.error("[app] Failed to load saved count:", error);
  }
}

/**
 * Network refresh for folders/feeds.
 * @param {{ background?: boolean }} [options]
 */
export async function refreshFolders({ background = true } = {}) {
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
      const { selectionInvalidated: invalidated } = applyFolderTree(
        tree.folders,
        tree.unfiledFeeds,
      );
      selectionInvalidated = invalidated;
      saveFolderTreeCache(tree.folders, tree.unfiledFeeds);
    } else {
      state.folders = tree.folders;
      state.unfiledFeeds = tree.unfiledFeeds;
      selectionInvalidated = resolveLastClickedItem();
      updateNavSelection(state.lastClickedItem.type, state.lastClickedItem.obj);
    }
    state.restoredFromCache = true;
    await refreshSavedCount();
    if (selectionInvalidated) {
      await selectAndLoadArticlesAfterInvalidation();
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

/**
 * Debounced localStorage write of the folder tree JSON without rebuilding the DOM.
 */
export function scheduleFolderTreeCacheSave() {
  if (folderTreeCacheSaveTimer != null) {
    clearTimeout(folderTreeCacheSaveTimer);
  }
  setFolderTreeCacheSaveTimer(
    setTimeout(() => {
      setFolderTreeCacheSaveTimer(null);
      saveFolderTreeCache(state.folders, state.unfiledFeeds);
    }, 400),
  );
}

/**
 * Update every visible unread badge from state without re-rendering the tree.
 */
export function syncUnreadBadgesFromState() {
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
export function findFeedMutable(feedId) {
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
export function decrementFeedUnreadCount(feedId) {
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

export async function createFolder(folder) {
  await withErrorModal(async () => {
    await dataService.createFolder(folder);
    await refreshFolders({ background: false });
  }, "Error saving folder: " + folder.name);
}

export async function editFolder(folder) {
  await withErrorModal(async () => {
    await dataService.updateFolder(folder);
    await refreshFolders({ background: false });
  }, "Error updating folder: " + folder.name);
}

export async function deleteFolder(folder) {
  await withErrorModal(async () => {
    const wasSelected =
      state.lastClickedItem.type === itemType.FOLDER &&
      state.lastClickedItem.obj?.id === folder.id;
    await dataService.deleteFolder(folder.id);
    await refreshFolders({ background: false });
    if (wasSelected || !state.lastClickedItem.obj) {
      const { selectAndLoadArticles } = await import("./articles.js");
      await selectAndLoadArticles(itemType.ALL, null, {
        syncUrl: "replace",
        column: columns.FEEDS,
      });
    }
  }, "Error deleting folder: " + folder.name);
}

export async function createFeed(feed) {
  await withErrorModal(async () => {
    await dataService.createFeed(feed);
    await refreshFolders({ background: false });
  }, (error) => {
    const isDuplicate = error.status === 409 || error.cause?.status === 409;
    return isDuplicate ? "This feed already exists." : "Error saving feed.";
  });
}

export async function editFeed(feed) {
  await withErrorModal(async () => {
    await dataService.updateFeed(feed);
    await refreshFolders({ background: false });
  }, "Error updating feed: " + feed.feedUrl);
}

export async function deleteFeed(feed) {
  await withErrorModal(async () => {
    const wasSelected =
      state.lastClickedItem.type === itemType.FEED &&
      state.lastClickedItem.obj?.id === feed.id;
    await dataService.deleteFeed(feed.id);
    await refreshFolders({ background: false });
    if (wasSelected || !state.lastClickedItem.obj) {
      const { selectAndLoadArticles } = await import("./articles.js");
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

export async function importFeeds() {
  const fileInput = document.createElement("input");
  fileInput.type = "file";
  fileInput.classList.add("d-none");
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

        const response = await apiFetch("./api/opml", {
          method: "POST",
          body: file,
          headers: {
            "Content-Type": file.type || "application/xml",
          },
        });

        if (!response.ok) {
          await modal.show({
            title: "Error",
            content: "Error importing feeds: " + response.statusText,
            type: "alert",
          });
          return;
        }

        const started = await response.json();
        const job = await dataService.waitForJob(started.id);
        if (job.status === "FAILED") {
          await modal.show({
            title: "Error",
            content: html`<p>${job.error || "OPML import failed."}</p>`,
            type: "alert",
          });
          return;
        }

        await modal.show({
          title: "Import complete",
          content: importJobSummaryHtml(job),
          type: "alert",
        });

        await refreshFolders({ background: false });
        await refreshFeeds();
      } catch (error) {
        console.error("[app] OPML import failed:", error);
        await modal.show({
          title: "Error",
          content: "Error importing feeds. Please try again.",
          type: "alert",
        });
      } finally {
        cleanup();
      }
    },
    { once: true },
  );

  fileInput.addEventListener("cancel", cleanup, { once: true });
  fileInput.click();
}

export async function exportFeeds() {
  try {
    const response = await apiFetch("./api/opml");

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

export async function refreshFeeds() {
  if (state.status.isRefreshing) {
    return;
  }

  state.status.isRefreshing = true;
  dom.refreshSpinner.classList.add("spinner");
  try {
    const started = await dataService.refreshFeeds();
    const job = await dataService.waitForJob(started.id);
    if (job.status === "FAILED") {
      await modal.show({
        title: "Error",
        content: html`<p>${job.error || "Feed refresh failed."}</p>`,
        type: "alert",
      });
      return;
    }
    await refreshFolders({ background: false });
    const { selectAndLoadArticles } = await import("./articles.js");
    await selectAndLoadArticles(itemType.ALL, null, {
      syncUrl: "replace",
      column: columns.ARTICLES,
    });
  } catch (error) {
    console.error("[app] Feed refresh failed:", error);
    await modal.show({
      title: "Error",
      content: "Feed refresh failed. Please try again.",
      type: "alert",
    });
  } finally {
    dom.refreshSpinner.classList.remove("spinner");
    state.status.isRefreshing = false;
  }
}

/**
 * One delegated event layer for boot-painted and live-rendered feed trees.
 * @param {{
 *   onSelectAll: () => void,
 *   onSelectFeed: (feed: object) => void,
 *   onSelectFolder: (folder: object) => void,
 *   onAllFeedsContextMenu: (x: number, y: number) => void,
 *   onFeedContextMenu: (x: number, y: number, feed: object) => void,
 *   onFolderContextMenu: (x: number, y: number, folder: object) => void,
 * }} handlers
 */
export function registerFeedsDelegatedEvents(handlers) {
  const feedsList = document.getElementById("feeds-list");

  feedsList?.addEventListener("click", (e) => {
    const options = e.target.closest(".tree-options");
    if (options) {
      e.preventDefault();
      e.stopPropagation();
      if (options.closest("#trigger-show-all-feeds")) {
        handlers.onAllFeedsContextMenu(e.clientX, e.clientY);
        return;
      }
      const feedLi = options.closest("li[data-feed-id]");
      if (feedLi) {
        const feed = findFeedById(Number(feedLi.dataset.feedId));
        if (feed) handlers.onFeedContextMenu(e.clientX, e.clientY, feed);
        return;
      }
      const folderDetails = options.closest("details[data-folder-id]");
      if (folderDetails) {
        const folder = findFolderById(Number(folderDetails.dataset.folderId));
        if (folder) handlers.onFolderContextMenu(e.clientX, e.clientY, folder);
      }
      return;
    }

    const feedLi = e.target.closest("li[data-feed-id]");
    if (feedLi && feedsList.contains(feedLi)) {
      const feed = findFeedById(Number(feedLi.dataset.feedId));
      if (feed) handlers.onSelectFeed(feed);
      return;
    }

    const folderName = e.target.closest(
      "details[data-folder-id] > summary > .tree-name",
    );
    if (folderName) {
      e.preventDefault();
      const details = folderName.closest("details[data-folder-id]");
      const folder = findFolderById(Number(details.dataset.folderId));
      if (folder) handlers.onSelectFolder(folder);
    }
  });

  feedsList?.addEventListener("keydown", (e) => {
    if (e.key !== "Enter" && e.key !== " ") return;
    const options = e.target.closest(".tree-options");
    if (!options || !feedsList.contains(options)) return;
    e.preventDefault();
    const rect = options.getBoundingClientRect();
    if (options.closest("#trigger-show-all-feeds")) {
      handlers.onAllFeedsContextMenu(rect.left, rect.bottom);
      return;
    }
    const feedLi = options.closest("li[data-feed-id]");
    if (feedLi) {
      const feed = findFeedById(Number(feedLi.dataset.feedId));
      if (feed) handlers.onFeedContextMenu(rect.left, rect.bottom, feed);
      return;
    }
    const folderDetails = options.closest("details[data-folder-id]");
    if (folderDetails) {
      const folder = findFolderById(Number(folderDetails.dataset.folderId));
      if (folder) handlers.onFolderContextMenu(rect.left, rect.bottom, folder);
    }
  });

  feedsList?.addEventListener(
    "toggle",
    (e) => {
      const details = e.target;
      if (!(details instanceof HTMLDetailsElement)) return;
      if (!details.dataset.folderId) return;
      setFolderOpenState(details.dataset.folderId, details.open);
    },
    true,
  );

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
}
