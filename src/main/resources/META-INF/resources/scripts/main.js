"use strict";

import { modal } from "./modal.js";
import {
  showAddFeedDialog,
  showAddFolderDialog,
  showConfirmDialog,
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
  createFeed,
  editFeed,
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
} from "./reader.js";
import {
  initContextMenuListeners,
  openAddContextMenu,
  feedContextMenu,
  folderContextMenu,
  allFeedsContextMenu,
} from "./menu.js";

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

window.addEventListener("DOMContentLoaded", async () => {
  onSessionLoss(() => {
    hideFeedsSpinner();
    removeSkeletons();
  });

  let signedIn = false;
  try {
    signedIn = await probeSession();
  } catch (error) {
    console.warn("[app] Session probe failed:", error);
    signedIn = false;
  }

  if (!signedIn) {
    enterLoggedOut();
    return;
  }

  enterLoggedIn();
  restoreFolderCache();
  initContextMenuListeners();
  initEventListeners();
  registerFeedsDelegatedEvents({
    onSelectAll: () => selectAndLoadArticles(itemType.ALL, null),
    onSelectFeed: (feed) => selectAndLoadArticles(itemType.FEED, feed),
    onSelectFolder: (folder) =>
      selectAndLoadArticles(itemType.FOLDER, folder),
    onAllFeedsContextMenu: allFeedsContextMenu,
    onFeedContextMenu: feedContextMenu,
    onFolderContextMenu: folderContextMenu,
  });
  registerArticlesDelegatedEvents(articleClickListener);

  try {
    setLazyLoadObserver(setupScrollObserver());
    window.addEventListener("popstate", onPopState);

    await refreshFolders({ background: true });
    if (isLoggedOut()) return;
    hydrateFeedIcons();
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
    const message = `Are you sure you want to delete the folder "${state.lastClickedItem.obj.name}"? All contained feeds will be deleted.`;
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
    const message = `Are you sure you want to delete the feed "${state.lastClickedItem.obj.name}"?`;
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
