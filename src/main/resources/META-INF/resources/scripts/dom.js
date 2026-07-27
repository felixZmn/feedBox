"use strict";

import { FOLDER_STATE_KEY } from "./data.js";
import { escapeHtml, getRelativeTime, parseDate, sanitizeHTML, isSafeHttpUrl } from "./util.js";

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
    const feedEl = feedsList.querySelector(`li[data-feed-id="${obj.id}"]`);
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

export function loadFolderOpenStates() {
  try {
    const raw = localStorage.getItem(FOLDER_STATE_KEY);
    return raw ? JSON.parse(raw) : {};
  } catch (err) {
    console.warn("Could not load folder open state", err);
    return {};
  }
}

export function saveFolderOpenStates(states) {
  try {
    localStorage.setItem(FOLDER_STATE_KEY, JSON.stringify(states));
  } catch (err) {
    console.warn("Could not save folder open state", err);
  }
}

export function setFolderOpenState(folderId, isOpen) {
  const states = loadFolderOpenStates();
  states[folderId] = !!isOpen;
  saveFolderOpenStates(states);
}

/**
 * Build feed list item HTML (no event listeners — delegation handles clicks).
 * @param {Feed} feed
 */
function feedHtml(feed) {
  const error = feed.lastError
    ? `<span class="feed-error" title="${escapeHtml(feed.lastError)}" aria-label="Feed error: ${escapeHtml(feed.lastError)}">!</span>`
    : "";
  return `<li data-feed-id="${feed.id}">
    <img class="tree-entry-icon" src="./api/icon/${feed.id}" alt="" data-fallback-icon="icons/rss.svg" />
    <span class="tree-name">${escapeHtml(feed.name || "")}</span>
    ${error}
    <span class="tree-options" role="button" tabindex="0" aria-label="Feed options">⋮</span>
  </li>`;
}

/**
 * Build folder details HTML.
 * @param {Folder} folder
 * @param {Record<string, boolean>} openStates
 */
function folderHtml(folder, openStates) {
  const open =
    Object.prototype.hasOwnProperty.call(openStates, folder.id) &&
    openStates[folder.id]
      ? " open"
      : "";
  const color = escapeHtml(folder.color || "f-base");
  const feeds = (folder.feeds ?? []).map(feedHtml).join("");
  return `<details data-folder-id="${folder.id}"${open}>
    <summary>
      <img src="icons/folder.svg" alt="" class="icon ${color}" />
      <span class="tree-name">${escapeHtml(folder.name || "")}</span>
      <span class="tree-options" role="button" tabindex="0" aria-label="Folder options">⋮</span>
    </summary>
    <div class="folder-container">
      <ul class="feeds-ul">${feeds}</ul>
    </div>
  </details>`;
}

/**
 * Apply persisted open/closed state onto an already-painted tree
 * (e.g. HTML injected from localStorage cache).
 */
export function syncFolderOpenStatesFromStorage() {
  const container = document.getElementById("folder-container");
  if (!container) return;
  const states = loadFolderOpenStates();
  container.querySelectorAll("details[data-folder-id]").forEach((details) => {
    const id = details.dataset.folderId;
    if (Object.prototype.hasOwnProperty.call(states, id)) {
      details.open = !!states[id];
    }
  });
}

/**
 * Canonical folder-tree HTML used both for live render and localStorage cache.
 * Cached HTML is stored without open attributes; open state is applied from
 * folder-state after inject / render.
 * @param {Folder[]} folders
 * @param {Feed[]} unfiledFeeds
 * @param {Record<string, boolean>} [openStates]
 * @returns {string}
 */
export function buildFoldersHtml(folders, unfiledFeeds, openStates = {}) {
  const folderPart = (folders ?? []).map((f) => folderHtml(f, openStates)).join("");
  const unfiled =
    unfiledFeeds && unfiledFeeds.length > 0
      ? `<ul class="feeds-ul">${unfiledFeeds.map(feedHtml).join("")}</ul>`
      : "";
  return folderPart + unfiled;
}

/**
 * Render folders into #folder-container and return the HTML for caching.
 * @param {Folder[]} folders
 * @param {Feed[]} unfiledFeeds
 * @returns {string}
 */
export function renderFoldersList(folders, unfiledFeeds) {
  const container = document.getElementById("folder-container");
  const html = buildFoldersHtml(folders, unfiledFeeds);
  container.innerHTML = html;
  delete container.dataset.cachePainted;
  syncFolderOpenStatesFromStorage();
  return html;
}

/**
 * Creates a single article DOM element (clicks via delegation).
 * @param {Article} article
 * @returns {HTMLElement}
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
  articleDiv.tabIndex = 0;
  articleDiv.setAttribute("role", "button");
  if (selectedArticleId != null && article.id === selectedArticleId) {
    articleDiv.classList.add("selected");
  }
  headerDiv.className = "article-header";
  imageDiv.className = "article-image";
  titleDiv.className = "article-title";
  sourceSpan.className = "source";
  ageSpan.className = "age";

  titleDiv.textContent = article.title || "No Title";
  sourceSpan.textContent = article.feedName || "Unknown";
  ageSpan.textContent = getRelativeTime(article.published) || "";

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
  return articleDiv;
}

export function clearArticlesList() {
  articlesContainer.innerHTML = "";
}

/**
 * @param {Article[]} articles
 * @param {number|null} [keepSelectedId]
 */
export function replaceArticlesList(articles, keepSelectedId = selectedArticleId) {
  if (keepSelectedId !== undefined) {
    selectedArticleId = keepSelectedId;
  }
  clearArticlesList();
  appendArticlesList(articles);
}

/**
 * @param {Article[]} articles
 * @param {number|null} [keepSelectedId]
 */
export function appendArticlesList(articles, keepSelectedId) {
  if (keepSelectedId !== undefined && keepSelectedId !== null) {
    selectedArticleId = keepSelectedId;
  }
  const fragment = document.createDocumentFragment();
  for (const article of articles) {
    fragment.appendChild(createArticleElement(article));
  }
  articlesContainer.appendChild(fragment);
}

/**
 * @param {Article} article
 */
export function renderReaderView(article) {
  const title = document.querySelector("#reader .title");
  const content = document.querySelector("#reader .content");
  const publisher = document.querySelector("#reader-publisher");
  const date = document.querySelector("#reader .date");
  const externalLink = document.querySelector("#trigger-external-open");

  title.textContent = article.title || "No Title";
  content.innerHTML =
    sanitizeHTML(article.content || article.description || "") || "No Content";
  date.textContent = `${parseDate(article.published) || "Unknown"} by ${
    article.authors || "Unknown"
  }`;
  publisher.textContent = article.feedName || "";

  if (article.link && isSafeHttpUrl(article.link)) {
    externalLink.href = article.link;
    externalLink.classList.remove("d-none");
  } else {
    externalLink.removeAttribute("href");
    externalLink.classList.add("d-none");
  }
}

export function clearReaderView() {
  const title = document.querySelector("#reader .title");
  const content = document.querySelector("#reader .content");
  const publisher = document.querySelector("#reader-publisher");
  const date = document.querySelector("#reader .date");
  const externalLink = document.querySelector("#trigger-external-open");

  title.textContent = "No article selected";
  content.innerHTML = "";
  date.textContent = "";
  publisher.textContent = "";
  externalLink.removeAttribute("href");
  externalLink.classList.add("d-none");
  setSelectedArticleHighlight(null);
}

/**
 * @param {number} [count=6]
 */
export function renderSkeletons(count = 6) {
  const fragment = document.createDocumentFragment();
  for (let i = 0; i < count; i++) {
    const articleDiv = document.createElement("div");
    articleDiv.className = "skeleton-article";

    const headerDiv = document.createElement("div");
    headerDiv.className = "skeleton-header";
    const sourceBar = document.createElement("div");
    sourceBar.className = "skeleton skeleton-source";
    const ageBar = document.createElement("div");
    ageBar.className = "skeleton skeleton-age";
    headerDiv.appendChild(sourceBar);
    headerDiv.appendChild(ageBar);
    articleDiv.appendChild(headerDiv);

    if (i % 3 === 0 || i % 4 === 0) {
      const imageDiv = document.createElement("div");
      imageDiv.className = "article-image";
      const imageBar = document.createElement("div");
      imageBar.className = "skeleton skeleton-image";
      imageDiv.appendChild(imageBar);
      articleDiv.appendChild(imageDiv);
    }

    const titleDiv = document.createElement("div");
    titleDiv.className = `skeleton ${
      i % 2 === 0 ? "skeleton-title" : "skeleton-title-short"
    }`;
    articleDiv.appendChild(titleDiv);
    fragment.appendChild(articleDiv);
  }
  articlesContainer.appendChild(fragment);
}

export function removeSkeletons() {
  articlesContainer
    .querySelectorAll(".skeleton-article")
    .forEach((el) => el.remove());
}

/**
 * @param {string} [message="No articles found"]
 */
export function renderEmptyState(message = "No articles found") {
  const div = document.createElement("div");
  div.className = "empty-state";
  div.textContent = message;
  articlesContainer.appendChild(div);
}

export function showFeedsSpinner() {
  const el = document.getElementById("feeds-loading");
  if (el) el.classList.remove("d-none");
}

export function hideFeedsSpinner() {
  const el = document.getElementById("feeds-loading");
  if (el) el.classList.add("d-none");
}
