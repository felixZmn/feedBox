"use strict";

import {
  dataService,
  buildFolderTree,
  loadFolderTreeCache,
  saveFolderTreeCache,
  folderTreesEqual,
  parseApiError,
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
import { toast } from "./toast.js";
import { jobProgressText, refreshSummaryText } from "./feed-health.js";
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

const JOB_PROGRESS_TOAST = "job-progress";

/**
 * In-place progress toast for a background job ("Refreshing 120 of 300…").
 * @param {"Refreshing"|"Importing"} verb
 * @param {{ total?: number, completed?: number, failed?: number }} initial
 * @returns {{ update: (job: object) => void, dismiss: () => void }}
 */
function showJobProgress(verb, initial) {
  const show = (job) =>
    toast(jobProgressText(verb, job), { id: JOB_PROGRESS_TOAST, timeoutMs: 0 });
  const handle = show(initial);
  return {
    update: (job) => {
      if (job.status !== "COMPLETED" && job.status !== "FAILED") show(job);
    },
    dismiss: () => handle.dismiss(),
  };
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
      scheduleFolderTreeCacheSave();
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

/**
 * @param {import("./types.js").Folder} folder
 * @param {"keep"|"delete"} [feeds="keep"]
 */
export async function deleteFolder(folder, feeds = "keep") {
  await withErrorModal(async () => {
    const wasSelected =
      state.lastClickedItem.type === itemType.FOLDER &&
      state.lastClickedItem.obj?.id === folder.id;
    await dataService.deleteFolder(folder.id, feeds);
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

/**
 * Reveal a feed the Add Feed dialog just created: open its folder, refresh
 * the tree, and select it so its first articles show right away.
 * @param {{ id: number, folderId: number|null }} created
 */
export async function showAddedFeed({ id, folderId }) {
  await withErrorModal(async () => {
    if (folderId != null) setFolderOpenState(folderId, true);
    await refreshFolders({ background: false });
    const feed = findFeedById(id);
    if (!feed) return;
    const { selectAndLoadArticles } = await import("./articles.js");
    await selectAndLoadArticles(itemType.FEED, feed);
  }, "The feed was added, but the list could not be refreshed.");
}

/** Pick up a rename, move, or new address saved by the Edit Feed dialog. */
export async function feedSaved() {
  await withErrorModal(
    () => refreshFolders({ background: false }),
    "The feed was saved, but the list could not be refreshed.",
  );
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
          const { detail } = parseApiError(
            await response.text().catch(() => ""),
          );
          await modal.show({
            title: "Error",
            content: html`<p>Error importing feeds: ${detail || response.statusText || response.status}</p>`,
            type: "alert",
          });
          return;
        }

        const started = await response.json();
        const progress = showJobProgress("Importing", started);
        let job;
        try {
          job = await dataService.waitForJob(started.id, {
            onProgress: progress.update,
          });
        } finally {
          progress.dismiss();
        }
        if (job.status === "FAILED") {
          await modal.show({
            title: "Error",
            content: html`<p>${job.error || "OPML import failed."}</p>`,
            type: "alert",
          });
          return;
        }

        await refreshFolders({ background: false });
        const { reloadArticlesForCurrentContext } = await import("./state.js");
        await reloadArticlesForCurrentContext();

        if ((job.failed ?? 0) > 0) {
          await modal.show({
            title: "Import finished with problems",
            content: importJobSummaryHtml(job),
            type: "alert",
          });
        } else {
          const n = job.completed ?? 0;
          toast(`Imported ${n} ${n === 1 ? "feed" : "feeds"}.`);
        }
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
    const progress = showJobProgress("Refreshing", started);
    let job;
    try {
      job = await dataService.waitForJob(started.id, {
        onProgress: progress.update,
      });
    } finally {
      progress.dismiss();
    }
    if (job.status === "FAILED") {
      await modal.show({
        title: "Error",
        content: html`<p>${job.error || "Feed refresh failed."}</p>`,
        type: "alert",
      });
      return;
    }
    await refreshFolders({ background: true });
    const { checkForNewArticles } = await import("./articles.js");
    await checkForNewArticles();
    const summary = refreshSummaryText(job);
    if (summary) toast(summary, { timeoutMs: 8000 });
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
 * True for clicks that should keep the browser's default link behaviour
 * (new tab / window / modified click).
 * @param {MouseEvent} e
 */
function isModifiedLinkClick(e) {
  return (
    e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey
  );
}

/**
 * One delegated event layer for boot-painted and live-rendered feed trees.
 * @param {{
 *   onSelectAll: () => void,
 *   onSelectUnread: () => void,
 *   onSelectSaved: () => void,
 *   onSelectFeed: (feed: object) => void,
 *   onSelectFolder: (folder: object) => void,
 *   onAllFeedsContextMenu: (trigger?: EventTarget | null) => void,
 *   onFeedContextMenu: (feed: object, trigger?: EventTarget | null) => void,
 *   onFolderContextMenu: (folder: object, trigger?: EventTarget | null) => void,
 * }} handlers
 */
export function registerFeedsDelegatedEvents(handlers) {
  const feedsList = document.getElementById("feeds-list");

  feedsList?.addEventListener("click", (e) => {
    const options = e.target.closest(".tree-options");
    if (options && feedsList.contains(options)) {
      e.preventDefault();
      e.stopPropagation();
      if (options.closest("#trigger-show-all-feeds")) {
        handlers.onAllFeedsContextMenu(options);
        return;
      }
      const feedLi = options.closest("li[data-feed-id]");
      if (feedLi) {
        const feed = findFeedById(Number(feedLi.dataset.feedId));
        if (feed) handlers.onFeedContextMenu(feed, options);
        return;
      }
      const folderEl = options.closest("[data-folder-id]");
      if (folderEl) {
        const folder = findFolderById(Number(folderEl.dataset.folderId));
        if (folder) {
          handlers.onFolderContextMenu(folder, options);
        }
      }
      return;
    }

    const toggle = e.target.closest(".folder-toggle");
    if (toggle && feedsList.contains(toggle)) {
      e.preventDefault();
      const folderEl = toggle.closest("[data-folder-id]");
      if (!folderEl) return;
      const nextOpen = toggle.getAttribute("aria-expanded") !== "true";
      toggle.setAttribute("aria-expanded", String(nextOpen));
      toggle.setAttribute(
        "aria-label",
        nextOpen ? "Collapse folder" : "Expand folder",
      );
      const panelId = toggle.getAttribute("aria-controls");
      const panel = panelId ? document.getElementById(panelId) : null;
      if (panel) panel.hidden = !nextOpen;
      setFolderOpenState(folderEl.dataset.folderId, nextOpen);
      return;
    }

    const link = e.target.closest("a.tree-link");
    if (!link || !feedsList.contains(link)) return;
    if (isModifiedLinkClick(e)) return;
    e.preventDefault();

    const feedLi = link.closest("li[data-feed-id]");
    if (feedLi) {
      const feed = findFeedById(Number(feedLi.dataset.feedId));
      if (feed) handlers.onSelectFeed(feed);
      return;
    }

    const folderEl = link.closest("[data-folder-id]");
    if (folderEl) {
      const folder = findFolderById(Number(folderEl.dataset.folderId));
      if (folder) handlers.onSelectFolder(folder);
      return;
    }

    if (link.closest("#trigger-show-all-feeds")) {
      handlers.onSelectAll();
      return;
    }
    if (link.closest("#trigger-show-unread")) {
      handlers.onSelectUnread();
      return;
    }
    if (link.closest("#trigger-show-saved")) {
      handlers.onSelectSaved();
    }
  });

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
