import { showView, navigateToArticlesList, navigateToReader } from "./nav.js";
import { buildUrlParams, hideDialog, showDialog } from "./util.js";
import {
  renderFoldersList,
  renderArticlesList,
  renderReaderView,
} from "./dom.js";

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
var lastClickedType = articleLoadType.ALL;
var lastClickedId = null;

window.addEventListener("DOMContentLoaded", async () => {
  showView("feeds");
  loadFolders();
  loadArticles(articleLoadType.ALL);
  document.querySelector(".modal-close").onclick = () => {
    hideDialog();
  };
});

document.querySelectorAll("#modal-content-container form").forEach((form) => {
  form.addEventListener("click", (e) => {
    e.preventDefault();
  });
});

document.addEventListener("click", (e) => {
  if (e.target === document.getElementById("modal")) {
    hideDialog();
  }
});

document.getElementById("trigger-edit").addEventListener("click", (e) => {
  editFeedClick();
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
  addFolderClick();
});

function addFolderClick() {
  showDialog(dialogType.ADD_FOLDER);
}

function editFeedClick() {
  if (lastClickedType == articleLoadType.FEED) {
    showDialog(dialogType.EDIT_FEED);
  }
  if (lastClickedType == articleLoadType.FOLDER) {
    showDialog(dialogType.EDIT_FOLDER);
  }
}

function dummy() {
  console.log("Click!");
}

async function loadArticles(t, id) {
  var url = "./api/articles";
  var params = new Map();

  lastClickedType = t;
  if (id != null) {
    lastClickedId = id;
  }

  params.set(lastClickedType, lastClickedId);
  if (paginationPublished != null && paginationId != null) {
    params.set("pagination_id", paginationId);
    params.set("pagination_date", paginationPublished);
  }

  var newArticles = await fetchJson(url + buildUrlParams(params));
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

export function loadArticle(articleId) {
  // article should be stored in global articles array
  const article = articles.find((a) => a.id === articleId);
  if (!article) {
    console.error("Article not found:", articleId);
    return;
  }
  renderReaderView(article);
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

async function fetchJson(url) {
  try {
    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(
        "Error fetching data from " + url + ": " + response.status
      );
    }

    if (response.status === 204) {
      return [];
    }

    return await response.json();
  } catch (e) {
    throw new Error("Error fetching data from " + url + ": " + response.status);
  }
}

export function scrollObserver() {
  return new IntersectionObserver(
    (entries, obs) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          obs.disconnect();
          loadArticles(lastClickedType);
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
 * @param {Number} feedId id of the clicked feed
 */
export function feedClickListener(feedId) {
  navigateToArticlesList();
  resetPagination();
  clearArticlesList();
  loadArticles(articleLoadType.FEED, feedId);
}

/**
 * Click listener for an click on a single folder in the left-side list
 * @param {Number} folderId id of the clicked folder
 */
export function folderClickListener(folderId) {
  navigateToArticlesList();
  resetPagination();
  clearArticlesList();
  loadArticles(articleLoadType.FOLDER, folderId);
}

/**
 * Click listener for an click on an single article in the middle list
 * @param {Number} articleId id of the clicked article
 */
export function articleClickListener(articleId) {
  loadArticle(articleId);
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
