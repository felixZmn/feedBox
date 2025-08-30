import { showView, navigateToArticlesList, navigateToReader } from "./nav.js";
import { buildUrlParams, fetchJson } from "./util.js";
import {
  renderFoldersList,
  renderArticlesList,
  renderReaderView,
} from "./dom.js";
import {
  hideDialog,
  showAddFolderDialog,
  showEditFeedDialog,
  showEditFolderDialog,
  showImportFeedDialog,
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

window.addEventListener("DOMContentLoaded", async () => {
  showView("feeds");
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
  dummy();
});
document.getElementById("trigger-next").addEventListener("click", (e) => {
  dummy();
});
document.getElementById("trigger-close").addEventListener("click", (e) => {
  dummy();
});
document.getElementById("trigger-refresh").addEventListener("click", (e) => {
  refreshFeeds();
});
document.getElementById("trigger-add-folder").addEventListener("click", (e) => {
  showAddFolderDialog(createFolder);
});
document.getElementById("trigger-delete").addEventListener("click", (e) => {
  deleteElementClick();
});
document.getElementById("trigger-import").addEventListener("click", (e) => {
  showImportFeedDialog(importFeeds);
});

function editFeedFolderClick() {
  if (lastClickedFeedItem.type == articleLoadType.FEED) {
    showEditFeedDialog(updateFeed);
  }
  if (lastClickedFeedItem.type == articleLoadType.FOLDER) {
    showEditFolderDialog(lastClickedFeedItem.obj, updateFolder);
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

function dummy() {
  console.log("Click!");
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

export function loadArticle(article) {
  // article should be stored in global articles array
  const result = articles.find((a) => a.id === article.id);
  if (!result) {
    console.error("Article not found:", article);
    return;
  }
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

/**
 * listener to handle back and forth clicks - used for mobile view
 */
window.addEventListener("popstate", (e) => {
  const state = e.state;
  if (!state) {
    // default landing
    showView("feeds");
  } else {
    showView(state.view);
  }
});

// pre-defined clicklisteners

/**
 * Click listener for an click on the "All Feeds"-Element
 */
export function allFeedsClickListener() {
  // ToDo: Reset Navigation
  navigateToArticlesList();
  resetPagination();
  clearArticlesList();
  loadArticles(articleLoadType.ALL);
}

/**
 * Click listener for an click on a single feed in the left-side list
 * @param {Feed} feed the clicked feed
 */
export function feedClickListener(feed) {
  navigateToArticlesList();
  resetPagination();
  clearArticlesList();
  loadArticles(articleLoadType.FEED, feed);
}

/**
 * Click listener for an click on a single folder in the left-side list
 * @param {Folder} folder id of the clicked folder
 */
export function folderClickListener(folder) {
  navigateToArticlesList();
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
  navigateToReader();
}

// DOING STUFF

async function loadFolders() {
  var foldersWithFeeds = await fetchJson("./api/folders");
  renderFoldersList(foldersWithFeeds);
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

async function updateFolder() {
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

async function createFeed() {}

async function updateFeed() {}

async function deleteFeed(feed) {
  const response = await fetch(`./api/feeds/${feed.id}`, {
    method: "DELETE",
  });

  if (response.ok) {
    loadFolders();
    hideDialog();
  } else {
    alert("Error deleting feed: " + response.statusText);
  }
}

async function importFeeds() {
  const input = document.getElementById("import-file");
  if (input.files.length === 0) {
    alert("Please select a file to import.");
    return;
  }
  const file = input.files[0];

  const response = await fetch("./api/opml", {
    method: "POST",
    body: file,
    headers: {
      "Content-Type": file.type,
    },
  });

  if (response.ok) {
    loadFolders();
    hideDialog();
    refreshFeeds();
  } else {
    alert("Error importing feeds: " + response.statusText);
  }
}
