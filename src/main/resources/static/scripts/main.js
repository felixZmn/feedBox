import { Navigator, columns } from "./nav.js";
import {
  renderFoldersList,
  renderArticlesList,
  renderReaderView,
  folderDropdownOptions,
  clearReaderView,
  removeFeedElement,
} from "./dom.js";
import {
  hideDialog,
  showAddFolderDialog,
  showEditFeedDialog,
  showEditFolderDialog,
  showConfirmDialog,
  showAddFeedDialog,
} from "./dialog.js";
import { dataService } from "./data.js";

const articleLoadType = Object.freeze({
  ALL: "",
  FEED: "feed",
  FOLDER: "folder",
});

export const dialogType = Object.freeze({
  ADD_FOLDER: "add-folder",
  EDIT_FOLDER: "edit-folder",
  ADD_FEED: "add-feed",
  EDIT_FEED: "edit-feed",
});

var articles = [];
var paginationId = null;
var paginationPublished = null;
var lazyLoadObserver = null;
var isRefreshing = false;
var isFilterActive = false;
var lastSearchTerm = "";
var lastClickedItem = {
  type: articleLoadType.ALL,
  obj: null,
};
var selectedArticle = null;

const navigator = new Navigator();

window.addEventListener("DOMContentLoaded", async () => {
  await loadFolders();
  await loadArticles();
  lazyLoadObserver = setupScrollObserver();
});

document.querySelector(".modal-close").onclick = () => {
  hideDialog();
};

document.addEventListener("click", (e) => {
  if (e.target === document.getElementById("modal")) {
    hideDialog();
  }

  // close context menu on outside click
  const menu = document.getElementById("context-menu");
  menu.style.display = "none";
});

document.getElementById("trigger-previous").addEventListener("click", (e) => {
  for (var i = articles.length - 1; i >= 0; i--) {
    if (articles[i].id == selectedArticle.id) {
      if (i - 1 >= 0) {
        loadArticle(articles[i - 1]);
      }
      break;
    }
  }
});

document.getElementById("trigger-next").addEventListener("click", (e) => {
  for (var i = 0; i < articles.length; i++) {
    if (articles[i].id == selectedArticle.id) {
      if (i + 1 < articles.length) {
        loadArticle(articles[i + 1]);
      }
      break;
    }
  }
});

document.getElementById("trigger-close").addEventListener("click", (e) => {
  clearReaderView();
  navigator.navigateTo(columns.ARTICLES);
});

document.getElementById("trigger-refresh").addEventListener("click", (e) => {
  refreshFeeds();
});

document.getElementById("trigger-folder-add").addEventListener("click", (e) => {
  showAddFolderDialog(() => createFolder());
});

document.getElementById("trigger-feed-add").addEventListener("click", (e) => {
  showAddFeedDialog(() => createFeed());
});

document.getElementById("trigger-feed-edit").addEventListener("click", (e) => {
  showEditFeedDialog(lastClickedItem.obj, () => editFeed());
});

document
  .getElementById("trigger-feed-delete")
  .addEventListener("click", (e) => {
    let message = `Are you sure you want to delete the feed "${lastClickedItem.obj.name}"?`;
    showConfirmDialog("Delete", message, () => deleteFeed(lastClickedItem.obj));
  });

document
  .getElementById("trigger-folder-edit")
  .addEventListener("click", (e) => {
    showEditFolderDialog(lastClickedItem.obj, () => editFolder());
  });

document
  .getElementById("trigger-folder-delete")
  .addEventListener("click", (e) => {
    let message = `Are you sure you want to delete the folder "${lastClickedItem.obj.name}"? All contained feeds will be deleted".`;
    showConfirmDialog("Delete", message, () => {
      deleteFolder(lastClickedItem.obj);
    });
  });

document.getElementById("trigger-import").addEventListener("click", (e) => {
  importFeeds();
});

document.getElementById("search-input").addEventListener("input", (e) => {
  let searchTerm = e.target.value.trim().toLowerCase();

  // filter empty -> reset
  if (searchTerm === "") {
    isFilterActive = false;
    lastSearchTerm = "";
    articles = dataService.getArticles();
    renderArticlesList(articles);
    return;
  }

  if (!searchTerm.startsWith(lastSearchTerm)) {
    articles = dataService.getArticles();
  }

  lastSearchTerm = searchTerm;
  articles = articles.filter((article) =>
    article.title.toLowerCase().includes(searchTerm)
  );
  isFilterActive = true;
  renderArticlesList(articles);
});

/**
 * seraches the selected article by id in the global articles array and renders it
 * ToDo: add lazy loading of missing articles - currently not an issue
 * @param {Article} article
 * @returns
 */
export function loadArticle(article) {
  // article should be stored in global articles array
  const result = articles.find((a) => a.id === article.id);
  if (!result) {
    console.error("Article not found:", article);
    return;
  }
  selectedArticle = result;
  renderReaderView(result);
}

// collapse/expand mechanic
function addFolderEvents() {
  const elements = document.getElementsByTagName("details");
  for (let i = 0; i < elements.length; i++) {
    elements[i].addEventListener("click", function (e) {
      e.preventDefault();
    });
  }

  const icons = document.querySelectorAll("details summary img");
  for (let i = 0; i < icons.length; i++) {
    icons[i].addEventListener("click", function (e) {
      const details = this.parentElement.parentElement;
      details.open = !details.open;
      if (details.open) {
        this.src = "icons/folder_open.svg";
      } else {
        this.src = "icons/folder.svg";
      }
    });
  }
}

/**
 * Sets up the scroll observer for infinite scrolling in the articles list
 */
function setupScrollObserver() {
  const sentinel = document.getElementById("articles-sentinel");

  const observer = new IntersectionObserver(
    (entries, obs) => {
      const entry = entries[0];
      if (!entry) return;
      if (entry.isIntersecting && !isFilterActive) {
        loadArticles();
      }
    },
    {
      root: document.querySelector("#articles-list .container"),
      rootMargin: "0px",
      scrollMargin: "325px", // 5 articles with about 65px height each
      threshold: 0.1,
    }
  );
  observer.observe(sentinel);
  return {
    pause: () => observer.disconnect(),
    resume: () => observer.observe(sentinel),
  };
}

/**
 * Click listener for the "Add"-Element
 */
export function openAddContextMenu(x, y) {
  // hide all items first, then show the add menu items
  document.querySelectorAll(".context-menu-item").forEach((element) => {
    element.style.display = "none";
  });
  document.querySelectorAll(".context-add").forEach((element) => {
    element.style.display = "block";
  });
  openContextMenu(x, y);
}

export function feedContextMenu(x, y, feed) {
  lastClickedItem.type = articleLoadType.FEED;
  lastClickedItem.obj = feed;
  // hide all items first, then show the feed menu items
  document.querySelectorAll(".context-menu-item").forEach((element) => {
    element.style.display = "none";
  });
  document.querySelectorAll(".context-feed").forEach((element) => {
    element.style.display = "block";
  });
  openContextMenu(x, y);
}

export function folderContextMenu(x, y, folder) {
  lastClickedItem.type = articleLoadType.FOLDER;
  lastClickedItem.obj = folder;
  // hide all items first, then show the folder menu items
  document.querySelectorAll(".context-menu-item").forEach((element) => {
    element.style.display = "none";
  });
  document.querySelectorAll(".context-folder").forEach((element) => {
    element.style.display = "block";
  });
  openContextMenu(x, y);
}

function openContextMenu(x, y) {
  const menu = document.getElementById("context-menu");
  menu.style.display = "block";
  const { innerWidth, innerHeight } = window;
  const menuRect = menu.getBoundingClientRect();
  menu.style.left = `${Math.min(x, innerWidth - menuRect.width)}px`;
  menu.style.top = `${Math.min(y, innerHeight - menuRect.height)}px`;
}

/**
 * Click listener for an click on the "All Feeds"-Element
 */
export async function allFeedsClickListener() {
  navigator.navigateTo(columns.ARTICLES);
  resetPagination();
  lastClickedItem.type = articleLoadType.ALL;
  lastClickedItem.obj = null;
  lazyLoadObserver.pause();
  clearArticlesList();
  await loadArticles();
  lazyLoadObserver.resume();
}

/**
 * Click listener for an click on a single feed in the left-side list
 * @param {Feed} feed the clicked feed
 */
export async function feedClickListener(feed) {
  navigator.navigateTo(columns.ARTICLES);
  resetPagination();
  lastClickedItem.type = articleLoadType.FEED;
  lastClickedItem.obj = feed;
  lazyLoadObserver.pause();
  clearArticlesList();
  await loadArticles();
  lazyLoadObserver.resume();
}

/**
 * Click listener for an click on a single folder in the left-side list
 * @param {Folder} folder id of the clicked folder
 */
export async function folderClickListener(folder) {
  navigator.navigateTo(columns.ARTICLES);
  resetPagination();
  lastClickedItem.type = articleLoadType.FOLDER;
  lastClickedItem.obj = folder;
  lazyLoadObserver.pause();
  clearArticlesList();
  await loadArticles();
  lazyLoadObserver.resume();
}

/**
 * Click listener for an click on an single article in the middle list
 * @param {Article} article the clicked article
 */
export function articleClickListener(article) {
  loadArticle(article);
  navigator.navigateTo(columns.READER);
}

function clearArticlesList() {
  articles = [];
  dataService.clearArticles();
  document.querySelector("#articles-list #articles-container").innerHTML = "";
}

function resetPagination() {
  paginationId = null;
  paginationPublished = null;
}

async function loadFolders() {
  const foldersWithFeeds = await dataService.getFolders();
  renderFoldersList(foldersWithFeeds);
  folderDropdownOptions(foldersWithFeeds);
  addFolderEvents();
  return foldersWithFeeds;
}

async function createFolder() {
  var folder = {};
  folder.name = document.getElementById("folder-name").value;
  folder.color = document.getElementById("folder-color").value;

  try {
    await dataService.createFolder(folder);
    await loadFolders();
  } catch (error) {
    alert("Error saving folder: " + folder.name);
    console.error(error.message);
  }
  hideDialog();
}

async function editFolder() {
  var folder = {};
  folder.name = document.getElementById("folder-name").value;
  folder.color = document.getElementById("folder-color").value;
  folder.id = lastClickedItem.obj.id;

  try {
    await dataService.updateFolder(folder);
    await loadFolders();
    lastClickedItem.obj.name = folder.name;
    lastClickedItem.obj.color = folder.color;
  } catch (error) {
    alert("Error updating folder: " + folder.name);
    console.error(error.message);
  }
  hideDialog();
}

async function deleteFolder(folder) {
  try {
    await dataService.deleteFolder(folder.id);
    await loadFolders();
  } catch (error) {
    alert("Error deleting folder: " + folder.name);
    console.error(error.message);
  }
  hideDialog();
}

async function createFeed() {
  var feed = {};
  feed.feedUrl = document.getElementById("feed-url").value;
  feed.folderId = document.getElementById("feed-folder").value;

  try {
    await dataService.createFeed(feed);
    await loadFolders();
  } catch (error) {
    alert("Error saving feed: " + feed.feedUrl);
    console.error(error.message);
  }
  hideDialog();
}

async function editFeed() {
  var feed = lastClickedItem.obj;
  feed.feedUrl = document.getElementById("feed-url").value;
  feed.folderId = document.getElementById("feed-folder").value;

  try {
    await dataService.updateFeed(feed);
    await loadFolders();
    lastClickedItem.obj.url = feed.feedUrl;
    lastClickedItem.obj.folderId = feed.folderId;
  } catch (error) {
    alert("Error updating feed: " + feed.feedUrl);
    console.error(error.message);
  }
  hideDialog();
}

async function deleteFeed(feed) {
  try {
    await dataService.deleteFeed(feed.id);
    removeFeedElement(feed.id);
    articles = articles.filter((article) => article.feedId !== feed.id);
    renderArticlesList(articles);
  } catch (error) {
    alert("Error deleting feed: " + feed.name);
    console.error(error.message);
  }
  hideDialog();
}

async function importFeeds() {
  const fileInput = document.createElement("input");
  fileInput.type = "file";
  fileInput.style.display = "none";
  fileInput.accept = ".opml,.xml,application/xml,text/xml";
  document.body.appendChild(fileInput);

  fileInput.addEventListener("change", async () => {
    if (fileInput.files.length === 0) {
      alert("Please select a file to import.");
      return;
    }
    const file = fileInput.files[0];

    const response = await fetch("./api/opml", {
      method: "POST",
      body: file,
      headers: {
        "Content-Type": file.type,
      },
    });

    if (response.ok) {
      loadFolders();
      refreshFeeds();
    } else {
      alert("Error importing feeds: " + response.statusText);
    }

    document.body.removeChild(fileInput);
  });

  fileInput.click();
}

async function refreshFeeds() {
  if (isRefreshing) {
    return;
  }

  isRefreshing = true;
  document.getElementById("refresh-spinner").classList.add("spinner");
  dataService.refreshFeeds().then(() => {
    document.getElementById("refresh-spinner").classList.remove("spinner");
    allFeedsClickListener();
    isRefreshing = false;
  });
}

async function loadArticles() {
  const params = {};
  switch (lastClickedItem.type) {
    case articleLoadType.FEED:
      if (!lastClickedItem.obj) return;
      params.feed = lastClickedItem.obj.id;
      break;
    case articleLoadType.FOLDER:
      if (!lastClickedItem.obj) return;
      params.folder = lastClickedItem.obj.id;
      break;
    case articleLoadType.ALL:
    // no additional param
    default:
      // no additional param
      break;
  }
  if (paginationId != null) {
    params.pagination_id = paginationId;
  }
  if (paginationPublished != null) {
    params.pagination_date = paginationPublished;
  }

  await dataService.loadArticles(params);

  const newArticles = dataService.getArticles();
  if (!newArticles || newArticles.length === 0) return;

  articles = newArticles;

  // update pagination
  const lastArticle = newArticles[newArticles.length - 1];
  paginationId = lastArticle.id;
  paginationPublished = lastArticle.published;

  renderArticlesList(articles);
}
