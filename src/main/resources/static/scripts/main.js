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
});

document.getElementById("trigger-edit").addEventListener("click", (e) => {
  editFeedFolderClick();
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
document.getElementById("trigger-add-folder").addEventListener("click", (e) => {
  showAddFolderDialog(createFolder);
});
document.getElementById("trigger-add-feed").addEventListener("click", (e) => {
  showAddFeedDialog(createFeed);
});

document.getElementById("trigger-delete").addEventListener("click", (e) => {
  var message = "";
  if (lastClickedFeedItem.type == articleLoadType.FOLDER) {
    message = `Are you sure you want to delete the folder "${lastClickedFeedItem.obj.name}"? All contained feeds will be deleted".`;
  }
  if (lastClickedFeedItem.type == articleLoadType.FEED) {
    message = `Are you sure you want to delete the feed "${lastClickedFeedItem.obj.name}"?`;
  }
  if (message == "") {
    return;
  }

  showConfirmDialog("Delete", message, deleteElementClick);
});
document.getElementById("trigger-import").addEventListener("click", (e) => {
  importFeeds();
});

document.getElementById("trigger-export").addEventListener("click", (e) => {
   exportFeeds();
 });

function editFeedFolderClick() {
  if (lastClickedFeedItem.type == articleLoadType.FEED) {
    showEditFeedDialog(lastClickedFeedItem.obj, editFeed);
  }
  if (lastClickedFeedItem.type == articleLoadType.FOLDER) {
    showEditFolderDialog(lastClickedFeedItem.obj, editFolder);
  }
}

function deleteElementClick() {
  if (lastClickedFeedItem.type == articleLoadType.FOLDER) {
    deleteFolder(lastClickedFeedItem.obj);
  }
  if (lastClickedFeedItem.type == articleLoadType.FEED) {
    deleteFeed(lastClickedFeedItem.obj);
  }
}

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

async function exportFeeds(){
  const response = await fetch("./api/opml", {
    method: "GET",
  });
}
