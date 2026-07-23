"use strict";

import {
  articleClickListener,
  feedClickListener,
  feedContextMenu,
  folderClickListener,
  folderContextMenu,
} from "./main.js";
import { getRelativeTime, parseDate, sanitizeHTML } from "./util.js";

const FOLDER_STATE_KEY = "folder-state";
const articlesContainer = document.querySelector(
  "#articles-list #articles-container",
);

/** @type {number|null} */
let selectedArticleId = null;

/**
 * Persist which article row is highlighted in the list (independent of hover).
 * @param {number|null|undefined} articleId
 */
export function setSelectedArticleHighlight(articleId) {
  selectedArticleId = articleId ?? null;
  articlesContainer
    .querySelectorAll(".article.selected")
    .forEach((el) => el.classList.remove("selected"));
  if (selectedArticleId == null) return;
  const el = articlesContainer.querySelector(
    `.article[data-article-id="${selectedArticleId}"]`,
  );
  if (el) el.classList.add("selected");
}

/**
 * Highlight the current All Feeds / folder / feed in the sidebar.
 * @param {string} type
 * @param {{id: number}|null} obj
 */
export function updateNavSelection(type, obj) {
  const feedsList = document.getElementById("feeds-list");
  if (!feedsList) return;

  feedsList
    .querySelectorAll(".selected")
    .forEach((el) => el.classList.remove("selected"));

  if (type === "feed" && obj) {
    const feedEl = feedsList.querySelector(
      `li[data-feed-id="${obj.id}"]`,
    );
    if (feedEl) feedEl.classList.add("selected");
    return;
  }

  if (type === "folder" && obj) {
    const folderEl = feedsList.querySelector(
      `details[data-folder-id="${obj.id}"] > summary`,
    );
    if (folderEl) folderEl.classList.add("selected");
    return;
  }

  document.getElementById("trigger-show-all-feeds")?.classList.add("selected");
}

function loadFolderOpenStates() {
  try {
    const raw = localStorage.getItem(FOLDER_STATE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch (err) {
    console.warn("Could not load folder open state", err);
    return {};
  }
}

function saveFolderOpenStates(state) {
  try {
    localStorage.setItem(FOLDER_STATE_KEY, JSON.stringify(state));
  } catch (err) {
    console.warn("Could not save folder open state", err);
  }
}

function setFolderOpenState(folderId, isOpen) {
  const state = loadFolderOpenStates();
  state[folderId] = !!isOpen;
  saveFolderOpenStates(state);
}

function getFolderOpenState(folderId) {
  const state = loadFolderOpenStates();
  return Object.prototype.hasOwnProperty.call(state, folderId)
    ? !!state[folderId]
    : undefined;
}

/**
 * Creates a single article DOM element
 * @param {Article} article - The article to create an element for
 * @returns {HTMLElement} The article div element
 */
function createArticleElement(article) {
  const articleDiv = document.createElement("div");
  const headerDiv = document.createElement("div");
  const imageDiv = document.createElement("div");
  const titleDiv = document.createElement("div");
  const sourceSpan = document.createElement("span");
  const ageSpan = document.createElement("span");

  articleDiv.className = "article";
  articleDiv.dataset.articleId = String(article.id);
  if (selectedArticleId != null && article.id === selectedArticleId) {
    articleDiv.classList.add("selected");
  }
  headerDiv.className = "article-header";
  imageDiv.className = "article-image";
  titleDiv.className = "article-title";
  sourceSpan.className = "source";
  ageSpan.className = "age";

  titleDiv.innerText = article.title || "No Title";
  sourceSpan.innerText = article.feedName || "Unknown";
  ageSpan.innerText = getRelativeTime(article.published);

  headerDiv.appendChild(sourceSpan);
  headerDiv.appendChild(ageSpan);

  articleDiv.appendChild(headerDiv);
  if (article.imageUrl) {
    const image = document.createElement("img");
    image.src = article.imageUrl;
    image.alt = "";
    imageDiv.appendChild(image);
    articleDiv.appendChild(imageDiv);
  }
  articleDiv.appendChild(titleDiv);

  articleDiv.addEventListener("click", () => {
    articleClickListener(article);
  });

  return articleDiv;
}

/**
 * Clears the articles container
 * @returns {void}
 */
export function clearArticlesList() {
  articlesContainer.innerHTML = "";
}

/**
 * Replaces all articles in the DOM (full re-render)
 * @param {Article[]} articles - The articles to render
 * @param {number|null} [keepSelectedId] - Article id to keep highlighted
 * @returns {void}
 */
export function replaceArticlesList(articles, keepSelectedId = selectedArticleId) {
  if (keepSelectedId !== undefined) {
    selectedArticleId = keepSelectedId;
  }
  clearArticlesList();
  appendArticlesList(articles);
}

/**
 * Appends articles to the DOM (incremental update)
 * @param {Article[]} articles - The articles to append
 * @param {number|null} [keepSelectedId]
 * @returns {void}
 */
export function appendArticlesList(articles, keepSelectedId) {
  if (keepSelectedId !== undefined && keepSelectedId !== null) {
    selectedArticleId = keepSelectedId;
  }
  const fragment = document.createDocumentFragment();
  for (const article of articles) {
    const articleEl = createArticleElement(article);
    fragment.appendChild(articleEl);
  }
  articlesContainer.appendChild(fragment);
}

/**
 * @param {Article} article - The array of articles to render.
 * @returns {void}
 */
export function renderReaderView(article) {
  const title = document.querySelector("#reader .title");
  const content = document.querySelector("#reader .content");
  const publisher = document.querySelector("#reader-publisher");
  const date = document.querySelector("#reader .date");
  const externalLink = document.querySelector("#trigger-external-open");

  title.innerText = article.title || "No Title";
  content.innerHTML =
    sanitizeHTML(article.content || article.description || "") || "No Content";
  date.innerText = `${parseDate(article.published) || "Unknown"} by ${
    article.authors || "Unknown"
  }`;
  publisher.innerText = article.feedName || "";
  externalLink.href = article.link || "";

  externalLink.classList.remove("d-none");
}

export function clearReaderView() {
  const title = document.querySelector("#reader .title");
  const content = document.querySelector("#reader .content");
  const publisher = document.querySelector("#reader-publisher");
  const date = document.querySelector("#reader .date");
  const externalLink = document.querySelector("#trigger-external-open");

  title.innerText = "No article selected";
  content.innerHTML = "";
  date.innerText = "";
  publisher.innerText = "";
  externalLink.href = "";

  externalLink.classList.add("d-none");
  setSelectedArticleHighlight(null);
}

/**
 * helper to create a feed element for the feed list
 * @param {Feed} feed
 * @returns
 */
function createFeedElement(feed) {
  const li = document.createElement("li");
  const icon = document.createElement("img");
  icon.src = "./api/icon/" + feed.id;
  icon.className = "tree-entry-icon";
  icon.alt = "";

  const nameSpan = document.createElement("span");
  nameSpan.textContent = feed.name || "";
  nameSpan.className = "tree-name";

  li.addEventListener("click", (e) => {
    feedClickListener(feed);
  });

  const options = document.createElement("span");
  options.classList.add("tree-options");
  options.textContent = "⋮";
  options.setAttribute("role", "button");
  options.setAttribute("aria-label", "Feed options");
  options.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    feedContextMenu(e.clientX, e.clientY, feed);
  });

  li.appendChild(icon);
  li.appendChild(nameSpan);

  if (feed.lastError) {
    const err = document.createElement("span");
    err.className = "feed-error";
    err.textContent = "!";
    err.title = feed.lastError;
    err.setAttribute("aria-label", `Feed error: ${feed.lastError}`);
    li.appendChild(err);
  }

  li.appendChild(options);
  li.dataset.feedId = feed.id.toString();
  return li;
}

/**
 * removes a feed element from the list of feeds
 * @param {number} feedId
 */
export function removeFeedElement(feedId) {
  const feedElement = document.querySelector(`li[data-feed-id='${feedId}']`);
  if (feedElement) {
    feedElement.remove();
  }
}

/**
 * renders the folder and feed list in the feeds column on the left
 * @param {Folder[]} folders - the list of folders with their feeds
 * @param {Feed[]} unfiledFeeds - feeds not belonging to any folder
 */
export function renderFoldersList(folders, unfiledFeeds) {
  const container = document.getElementById("folder-container");

  container.innerHTML = "";

  folders.forEach((folder) => {
    const details = document.createElement("details");
    details.dataset.folderId = String(folder.id);
    const persistedOpen = getFolderOpenState(folder.id);
    if (persistedOpen !== undefined) {
      details.open = persistedOpen;
    }

    details.appendChild(createFolderElement(folder));

    details.addEventListener("toggle", () => {
      setFolderOpenState(folder.id, details.open);
    });

    const feedsContainer = document.createElement("div");
    feedsContainer.className = "folder-container";

    const ul = document.createElement("ul");
    ul.className = "feeds-ul";

    if (!folder.feeds) {
      return;
    }
    folder.feeds.forEach((feed) => {
      ul.appendChild(createFeedElement(feed));
    });

    feedsContainer.appendChild(ul);
    details.appendChild(feedsContainer);
    container.appendChild(details);
  });

  // Render unfiled feeds directly under the folder tree (no folder wrapper)
  if (unfiledFeeds && unfiledFeeds.length > 0) {
    const ul = document.createElement("ul");
    ul.className = "feeds-ul";
    unfiledFeeds.forEach((feed) => {
      ul.appendChild(createFeedElement(feed));
    });
    container.appendChild(ul);
  }
}

/**
 *
 * @param {Folder} folder
 * @returns {HTMLElement}
 */
function createFolderElement(folder) {
  const summary = document.createElement("summary");
  const img = document.createElement("img");
  img.src = "icons/folder.svg";
  img.classList.add("icon", folder.color);

  const nameSpan = document.createElement("span");
  nameSpan.textContent = folder.name || "";
  nameSpan.className = "tree-name";
  nameSpan.addEventListener("click", (e) => {
    e.preventDefault();
    folderClickListener(folder);
  });

  const options = document.createElement("span");
  options.classList.add("tree-options");
  options.textContent = "⋮";
  options.setAttribute("role", "button");
  options.setAttribute("aria-label", "Folder options");
  options.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    folderContextMenu(e.clientX, e.clientY, folder);
  });

  summary.appendChild(img);
  summary.appendChild(nameSpan);
  summary.appendChild(options);

  return summary;
}

/**
 * Renders skeleton placeholder articles during loading.
 * Randomly varies title width (90% or 60%) per skeleton for a natural look,
 * and includes an image placeholder for ~40% of items.
 * @param {number} [count=6] - Number of skeleton articles to render
 */
export function renderSkeletons(count = 6) {
  const fragment = document.createDocumentFragment();
  for (let i = 0; i < count; i++) {
    const articleDiv = document.createElement("div");
    articleDiv.className = "skeleton-article";

    // header row: source + age bars
    const headerDiv = document.createElement("div");
    headerDiv.className = "skeleton-header";
    const sourceBar = document.createElement("div");
    sourceBar.className = "skeleton skeleton-source";
    const ageBar = document.createElement("div");
    ageBar.className = "skeleton skeleton-age";
    headerDiv.appendChild(sourceBar);
    headerDiv.appendChild(ageBar);

    articleDiv.appendChild(headerDiv);

    // ~40% get an image placeholder
    if (i % 3 === 0 || i % 4 === 0) {
      const imageDiv = document.createElement("div");
      imageDiv.className = "article-image";
      const imageBar = document.createElement("div");
      imageBar.className = "skeleton skeleton-image";
      imageDiv.appendChild(imageBar);
      articleDiv.appendChild(imageDiv);
    }

    // title bar
    const titleDiv = document.createElement("div");
    titleDiv.className = `skeleton ${
      i % 2 === 0 ? "skeleton-title" : "skeleton-title-short"
    }`;
    articleDiv.appendChild(titleDiv);

    fragment.appendChild(articleDiv);
  }
  articlesContainer.appendChild(fragment);
}

/**
 * Removes all skeleton article placeholders from the articles container.
 */
export function removeSkeletons() {
  articlesContainer
    .querySelectorAll(".skeleton-article")
    .forEach((el) => el.remove());
}

/**
 * Renders an empty state message in the articles container.
 * @param {string} [message="No articles found"] - The message to display
 */
export function renderEmptyState(message = "No articles found") {
  const div = document.createElement("div");
  div.className = "empty-state";
  div.textContent = message;
  articlesContainer.appendChild(div);
}

/**
 * Shows the feeds sidebar loading spinner.
 */
export function showFeedsSpinner() {
  const el = document.getElementById("feeds-loading");
  if (el) el.classList.remove("d-none");
}

/**
 * Hides the feeds sidebar loading spinner.
 */
export function hideFeedsSpinner() {
  const el = document.getElementById("feeds-loading");
  if (el) el.classList.add("d-none");
}
