import { Navigator, columns } from "./nav.js";
import { buildUrlParams, fetchJson } from "./util.js";
import {
  renderFoldersList,
  renderArticlesList,
  renderReaderView,
  clearReaderView,
  folderDropdownOptions,
} from "./dom.js";
import {
  hideDialog,
  showAddFolderDialog,
  showEditFeedDialog,
  showEditFolderDialog,
  showConfirmDialog,
  showAddFeedDialog,
} from "./dialog.js";

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

var articles = null;
var paginationId = null;
var paginationPublished = null;
var isRefreshing = false;
var lastClickedFeedItem = {
  type: articleLoadType.ALL,
  obj: null,
};
var selectedArticle = null;

const navigator = new Navigator();

window.addEventListener("DOMContentLoaded", async () => {
  loadFolders();
  loadArticles(articleLoadType.ALL);
  document.querySelector(".modal-close").onclick = () => {
    hideDialog();
  };
});

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
  showEditFeedDialog(lastClickedFeedItem.obj, () => editFeed());
});

document
  .getElementById("trigger-feed-delete")
  .addEventListener("click", (e) => {
    let message = `Are you sure you want to delete the feed "${lastClickedFeedItem.obj.name}"?`;
    showConfirmDialog("Delete", message, () =>
      deleteFeed(lastClickedFeedItem.obj)
    );
  });

document
  .getElementById("trigger-folder-edit")
  .addEventListener("click", (e) => {
    showEditFolderDialog(lastClickedFeedItem.obj, () => editFolder());
  });

document
  .getElementById("trigger-folder-delete")
  .addEventListener("click", (e) => {
    let message = `Are you sure you want to delete the folder "${lastClickedFeedItem.obj.name}"? All contained feeds will be deleted".`;
    showConfirmDialog("Delete", message, () => {
      deleteFolder(lastClickedFeedItem.obj);
    });
  });

document.getElementById("trigger-import").addEventListener("click", (e) => {
  importFeeds();
});

document.getElementById("search-input").addEventListener("input", (e) => {
  console.log("Search input:", e.target.value);
  // ToDo: implement filtering
});

async function loadArticles(type, object) {
  var url = "./api/articles";
  var params = new Map();

  lastClickedFeedItem.type = type;
  if (object != null) {
    lastClickedFeedItem.obj = object;
  }

  params.set(lastClickedFeedItem.type, lastClickedFeedItem.obj?.id);
  if (paginationPublished != null && paginationId != null) {
    params.set("pagination_id", paginationId);
    params.set("pagination_date", paginationPublished);
  }

  var newArticles = await fetchJson(url + buildUrlParams(params));
  if (newArticles.length == 0) {
    return;
  }

  if (articles == null) {
    articles = newArticles;
  } else {
    articles = articles.concat(newArticles);
  }

  const last = articles[articles.length - 1];
  // check if last is something new
  if (paginationPublished == last.published && paginationId == last.id) {
    // no new content
    return;
  }

  paginationPublished = last.published;
  paginationId = last.id;
  renderArticlesList(articles);
}

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

export function scrollObserver() {
  return new IntersectionObserver(
    (entries, obs) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          obs.disconnect();
          loadArticles(lastClickedFeedItem.type, null);
        }
      });
    },
    {
      root: document.querySelector("#articlesList .container"),
      rootMargin: "0px",
      scrollMargin: "0px",
      threshold: 0.1,
    }
  );
}

// pre-defined clicklisteners

/**
 * Click listener for an click on the "All Feeds"-Element
 */
export function allFeedsClickListener() {
  // ToDo: Reset Navigation
  navigator.navigateTo(columns.ARTICLES);
  resetPagination();
  clearArticlesList();
  loadArticles(articleLoadType.ALL);
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
  lastClickedFeedItem.type = articleLoadType.FEED;
  lastClickedFeedItem.obj = feed;
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
  lastClickedFeedItem.type = articleLoadType.FOLDER;
  lastClickedFeedItem.obj = folder;
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
 * Click listener for an click on a single feed in the left-side list
 * @param {Feed} feed the clicked feed
 */
export function feedClickListener(feed) {
  navigator.navigateTo(columns.ARTICLES);
  resetPagination();
  clearArticlesList();
  loadArticles(articleLoadType.FEED, feed);
}

/**
 * Click listener for an click on a single folder in the left-side list
 * @param {Folder} folder id of the clicked folder
 */
export function folderClickListener(folder) {
  navigator.navigateTo(columns.ARTICLES);
  resetPagination();
  clearArticlesList();
  loadArticles(articleLoadType.FOLDER, folder);
}

/**
 * Click listener for an click on an single article in the middle list
 * @param {Article} article the clicked article
 */
export function articleClickListener(article) {
  loadArticle(article);
  navigator.navigateTo(columns.READER);
}

// DOING STUFF

async function loadFolders() {
  var foldersWithFeeds = await fetchJson("./api/folders");
  renderFoldersList(foldersWithFeeds);
  folderDropdownOptions(foldersWithFeeds);
  addFolderEvents();
}

function clearArticlesList() {
  articles = null;
  document.querySelector("#articlesList .column").innerHTML = "";
}

function resetPagination() {
  paginationId = null;
  paginationPublished = null;
}

async function refreshFeeds() {
  if (isRefreshing) {
    return;
  }

  isRefreshing = true;
  document.getElementById("refresh-spinner").classList.add("spinner");
  await fetchJson("./api/feeds/refresh");
  document.getElementById("refresh-spinner").classList.remove("spinner");
  allFeedsClickListener();
  isRefreshing = false;
}

async function createFolder() {
  var name = document.getElementById("folder-name").value;
  var color = document.getElementById("folder-color").value;

  const response = await fetch("./api/folders", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ name: name, color: color }),
  });

  if (response.ok) {
    loadFolders();
    hideDialog();
  } else {
    alert("Error saving folder: " + response.statusText);
  }
}

async function editFolder() {
  var name = document.getElementById("folder-name").value;
  var color = document.getElementById("folder-color").value;

  const response = await fetch(`./api/folders/${lastClickedFeedItem.obj.id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ name: name, color: color }),
  });

  if (response.ok) {
    loadFolders();
    hideDialog();
    lastClickedFeedItem.obj.name = name;
    lastClickedFeedItem.obj.color = color;
  } else {
    alert("Error updating folder: " + response.statusText);
  }
}

async function deleteFolder(folder) {
  const response = await fetch(`./api/folders/${folder.id}`, {
    method: "DELETE",
  });

  if (response.ok) {
    loadFolders();
    hideDialog();
  } else {
    alert("Error deleting folder: " + response.statusText);
  }
}

async function createFeed() {
  var url = document.getElementById("feed-url").value;
  var folder = document.getElementById("feed-folder").value;

  const response = await fetch("./api/feeds", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ feedUrl: url, folderId: folder }),
  });

  if (response.ok) {
    loadFolders();
    hideDialog();
  } else {
    alert("Error saving feed: " + response.statusText);
  }
}

async function editFeed() {
  var feed = lastClickedFeedItem.obj;
  feed.feedUrl = document.getElementById("feed-url").value;
  feed.folderId = document.getElementById("feed-folder").value;

  const response = await fetch(`./api/feeds/${feed.id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(feed),
  });

  if (response.ok) {
    loadFolders();
    hideDialog();
    lastClickedFeedItem.obj.url = feed.feedUrl;
    lastClickedFeedItem.obj.folderId = feed.folderId;
  } else {
    alert("Error updating feed: " + response.statusText);
  }
}

async function deleteFeed(feed) {
  const response = await fetch(`./api/feeds/${feed.id}`, {
    method: "DELETE",
  });

  if (response.ok) {
    loadFolders();
    articles = articles.filter((a) => a.feedId !== feed.id);
    renderArticlesList(articles);
    navigator.navigateTo(columns.FEEDS);
    clearReaderView();
    hideDialog();
  } else {
    alert("Error deleting feed: " + response.statusText);
  }
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
