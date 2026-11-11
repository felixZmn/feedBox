"use strict";

import { modal } from "./modal.js";
import {
  showAddFeedDialog,
  showAddFolderDialog,
  showDeleteFeedDialog,
  showDeleteFolderDialog,
  showEditFeedDialog,
  showEditFolderDialog,
} from "./dialog.js";
import { removeSkeletons, hideFeedsSpinner, clearReaderView } from "./dom.js";
import { createArticleContextReloader } from "./article-context.js";
import { isSafeHttpUrl } from "./util.js";
import {
  logout,
  probeSession,
  enterLoggedIn,
  enterLoggedOut,
  onSessionLoss,
  isLoggedOut,
  initReturnToCapture,
  initCrossTabLogout,
  takeReturnTo,
} from "./auth.js";
import {
  state,
  itemType,
  dom,
  lazyLoadObserver,
  setLazyLoadObserver,
  setReloadArticlesForCurrentContext,
  navigationService,
  columns,
} from "./state.js";
import {
  restoreFolderCache,
  hydrateFeedIcons,
  refreshFolders,
  createFolder,
  editFolder,
  deleteFolder,
  showAddedFeed,
  feedSaved,
  deleteFeed,
  importFeeds,
  exportFeeds,
  refreshFeeds,
  registerFeedsDelegatedEvents,
} from "./feeds.js";
import {
  selectAndLoadArticles,
  submitSearch,
  markAllAsRead,
  markFeedAsRead,
  markFolderAsRead,
  initMarkReadFlush,
  initNewArticlesProbe,
  setupScrollObserver,
  loadArticles,
  clearArticles,
  resetPagination,
  showArticleLoadError,
  registerArticlesDelegatedEvents,
} from "./articles.js";
import {
  navigateArticle,
  closeReader,
  articleClickListener,
  applyRouteFromLocation,
  onPopState,
  registerSaveArticleButton,
} from "./reader.js";
import {
  initContextMenuListeners,
  openAddContextMenu,
  openChromeContextMenu,
  feedContextMenu,
  folderContextMenu,
  allFeedsContextMenu,
  restoreMenuTriggerFocus,
  hideContextMenu,
} from "./menu.js";
import { initShortcuts, showShortcutsHelp } from "./shortcuts.js";
import { initSearchUi } from "./search-ui.js";

setReloadArticlesForCurrentContext(
  createArticleContextReloader({
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
  }),
);

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/sw.js").catch((err) => {
      console.error("Service worker registration failed:", err);
    });
  });
}

const connectivityBanner = {
  el: () => document.getElementById("connectivity-banner"),
  show() {
    const el = this.el();
    if (el) el.hidden = false;
  },
  hide() {
    const el = this.el();
    if (el) el.hidden = true;
  },
};

let appInitialized = false;
let dataObserversReady = false;
let retrying = false;

/** Wire the shell once; safe to call again after a retry. */
function initApp() {
  if (appInitialized) return;
  appInitialized = true;
  restoreFolderCache();
  initContextMenuListeners();
  initMarkReadFlush();
  initNewArticlesProbe();
  initEventListeners();
  registerFeedsDelegatedEvents({
    onSelectAll: () => selectAndLoadArticles(itemType.ALL, null),
    onSelectUnread: () => selectAndLoadArticles(itemType.UNREAD, null),
    onSelectSaved: () => selectAndLoadArticles(itemType.SAVED, null),
    onSelectFeed: (feed) => selectAndLoadArticles(itemType.FEED, feed),
    onSelectFolder: (folder) =>
      selectAndLoadArticles(itemType.FOLDER, folder),
    onAllFeedsContextMenu: (x, y, trigger) =>
      allFeedsContextMenu(x, y, trigger),
    onFeedContextMenu: (x, y, feed, trigger) =>
      feedContextMenu(x, y, feed, trigger),
    onFolderContextMenu: (x, y, folder, trigger) =>
      folderContextMenu(x, y, folder, trigger),
  });
  registerArticlesDelegatedEvents(articleClickListener);
  initShortcuts();
  initSearchUi(() => submitSearch());
}

/** Fetch the folder tree and apply the route; needs a live session. */
async function loadApp() {
  try {
    if (!dataObserversReady) {
      dataObserversReady = true;
      setLazyLoadObserver(setupScrollObserver());
      window.addEventListener("popstate", onPopState);
    }

    await refreshFolders({ background: true });
    if (isLoggedOut()) return;
    hydrateFeedIcons();
    const returnTo = takeReturnTo(window.location.pathname);
    if (returnTo) {
      window.history.replaceState(window.history.state, "", returnTo);
    }
    await applyRouteFromLocation({ replaceHistory: true });
  } catch (error) {
    if (isLoggedOut() || error?.message === "Session expired") {
      return;
    }
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
}

/** Probe the session and move the UI to the matching state. */
async function startSession() {
  let status = "unavailable";
  try {
    status = await probeSession();
  } catch (error) {
    console.warn("[app] Session probe failed:", error);
  }

  if (status === "signed-out") {
    connectivityBanner.hide();
    enterLoggedOut();
    return;
  }

  enterLoggedIn();
  initApp();

  if (status === "unavailable") {
    // Not a sign-out: keep the cached tree and let the user retry.
    connectivityBanner.show();
    return;
  }

  connectivityBanner.hide();
  await loadApp();
}

async function retryConnection() {
  if (retrying || isLoggedOut()) return;
  retrying = true;
  try {
    await startSession();
  } finally {
    retrying = false;
  }
}

window.addEventListener("DOMContentLoaded", async () => {
  onSessionLoss(() => {
    connectivityBanner.hide();
    if (modal.dialog?.open) modal.close(null);
    hideContextMenu({ restoreFocus: false });
    hideFeedsSpinner();
    removeSkeletons();
  });

  initReturnToCapture();
  initCrossTabLogout();

  document
    .getElementById("connectivity-retry")
    ?.addEventListener("click", () => retryConnection());
  window.addEventListener("online", () => {
    if (!connectivityBanner.el()?.hidden) retryConnection();
  });

  await startSession();
});

function initEventListeners() {
  dom.button.next.addEventListener("click", () => navigateArticle(1));
  dom.button.previous.addEventListener("click", () => navigateArticle(-1));
  if (dom.button.backToFeeds) {
    dom.button.backToFeeds.addEventListener("click", (e) => {
      e.preventDefault();
      navigationService.navigateTo(columns.FEEDS);
    });
  }
  registerSaveArticleButton();
  dom.button.add.addEventListener("click", (e) => {
    e.stopPropagation();
    openAddContextMenu(e.clientX, e.clientY, e.currentTarget);
  });
  dom.button.refresh.addEventListener("click", () => refreshFeeds());
  dom.button.headerMenu?.addEventListener("click", (e) => {
    e.stopPropagation();
    const rect = e.currentTarget.getBoundingClientRect();
    openChromeContextMenu(rect.left, rect.bottom + 4, e.currentTarget);
  });
  dom.button.export?.addEventListener("click", async (event) => {
    event.preventDefault();
    await exportFeeds();
  });
  dom.button.import?.addEventListener("click", () => importFeeds());
  dom.button.shortcutsHelp?.addEventListener("click", () => {
    void showShortcutsHelp();
  });
  dom.button.addFolder.addEventListener("click", async () => {
    try {
      const newFolder = await showAddFolderDialog();
      if (newFolder) await createFolder(newFolder);
    } finally {
      restoreMenuTriggerFocus();
    }
  });
  dom.button.editFolder.addEventListener("click", async () => {
    try {
      const editedFolder = await showEditFolderDialog(state.lastClickedItem.obj);
      if (editedFolder) {
        editedFolder.id = state.lastClickedItem.obj.id;
        await editFolder(editedFolder);
      }
    } finally {
      restoreMenuTriggerFocus();
    }
  });
  dom.button.deleteFolder.addEventListener("click", async () => {
    try {
      const folder = state.lastClickedItem.obj;
      if (!folder) return;
      const response = await showDeleteFolderDialog(folder);
      if (response) await deleteFolder(folder, response.feeds);
    } finally {
      restoreMenuTriggerFocus();
    }
  });

  dom.button.addFeed.addEventListener("click", async () => {
    try {
      const created = await showAddFeedDialog(state.folders);
      if (created) await showAddedFeed(created);
    } finally {
      restoreMenuTriggerFocus();
    }
  });
  dom.button.editFeed.addEventListener("click", async () => {
    try {
      const saved = await showEditFeedDialog(
        state.folders,
        state.lastClickedItem.obj,
      );
      if (saved) await feedSaved();
    } finally {
      restoreMenuTriggerFocus();
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
    try {
      const feed = state.lastClickedItem.obj;
      if (!feed) return;
      const confirmed = await showDeleteFeedDialog(feed);
      if (confirmed) await deleteFeed(feed);
    } finally {
      restoreMenuTriggerFocus();
    }
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
