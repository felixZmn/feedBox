"use strict";

import { articleMarkState, articleStarState } from "./article-row.js";
import { FOLDER_STATE_KEY } from "./data.js";
import { savedBadgePresentation } from "./saved.js";
import { unreadBadgeHtml, totalUnreadCount } from "./unread.js";
import { html, getRelativeTime, parseDate, sanitizeHTML, isSafeHttpUrl } from "./util.js";

export {
  unreadBadgeHtml,
  totalUnreadCount,
  setFeedUnreadBadge,
  setFolderUnreadBadge,
} from "./unread.js";

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
 * Toggle the unread style on a loaded article row.
 * @param {number} articleId
 * @param {boolean} read
 */
export function setArticleReadState(articleId, read) {
  const el = articlesContainer.querySelector(
    `.article[data-article-id="${articleId}"]`,
  );
  if (!el) return;
  el.classList.toggle("unread", !read);
  const title = el.querySelector(".article-title")?.textContent || "No Title";
  el.setAttribute("aria-label", `${title}, ${read ? "Read" : "Unread"}`);
  const button = el.querySelector(".article-mark");
  if (!button) return;
  paintFlag(button, articleMarkState(read));
}

/**
 * Swap the star on a loaded row without moving it.
 * @param {number} articleId
 * @param {boolean} saved
 */
export function setArticleSavedState(articleId, saved) {
  const el = articlesContainer?.querySelector(
    `.article[data-article-id="${articleId}"]`,
  );
  if (!el) return;
  const button = el.querySelector(".article-star");
  if (!button) return;
  paintFlag(button, articleStarState(saved));
}

/**
 * @param {number} count
 */
export function updateSavedBadge(count) {
  const el = document.getElementById("saved-count");
  if (!el) return;
  const badge = savedBadgePresentation(count);
  el.textContent = badge.text;
  el.hidden = badge.hidden;
  if (badge.label) {
    el.setAttribute("aria-label", badge.label);
  } else {
    el.removeAttribute("aria-label");
  }
}

/**
 * Header star. Disabled when no article is open.
 * @param {{ id?: number, saved?: boolean } | null} article
 */
export function syncReaderSaveButton(article) {
  const button = document.getElementById("trigger-save-article");
  const img = document.getElementById("reader-save-icon");
  if (!button || !img) return;
  if (!article?.id) {
    button.disabled = true;
    paintFlag(button, articleStarState(false));
    return;
  }
  button.disabled = false;
  paintFlag(button, articleStarState(!!article.saved));
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

  if (type === "unread") {
    document.getElementById("trigger-show-unread")?.classList.add("selected");
    return;
  }

  if (type === "saved") {
    document.getElementById("trigger-show-saved")?.classList.add("selected");
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
 * @param {number} count
 */
export function updateAllFeedsUnreadBadge(count) {
  const el = document.getElementById("all-feeds-unread");
  if (!el) return;
  const n = Number(count) || 0;
  if (n <= 0) {
    el.textContent = "";
    el.hidden = true;
    el.removeAttribute("aria-label");
    return;
  }
  el.textContent = String(n);
  el.hidden = false;
  el.setAttribute("aria-label", `${n} unread`);
}

/**
 * Build feed list item HTML (no event listeners — delegation handles clicks).
 * @param {Feed} feed
 */
function feedHtml(feed) {
  const error = feed.lastError
    ? html`<span class="feed-error" title="${feed.lastError}" aria-label="Feed error: ${feed.lastError}">!</span>`
    : "";
  // Icons require auth — hydrate via loadFeedIcons() after paint (cookie
  // credentials on fetch; placeholder until then).
  return (
    html`<li data-feed-id="${feed.id}">
    <img class="tree-entry-icon" src="icons/rss.svg" alt="" data-feed-icon="${feed.id}" />
    <span class="tree-name">${feed.name || ""}</span>
    ` +
    error +
    unreadBadgeHtml(feed.unreadCount) +
    html`<span class="tree-options" role="button" tabindex="0" aria-label="Feed options">⋮</span>
  </li>`
  );
}

/** @type {Map<string, string>} blob object URLs keyed by feed id */
const feedIconObjectUrls = new Map();

/**
 * Fetch feed icons with an authenticated client and set blob: URLs on
 * {@code [data-feed-icon]} images. Safe to call repeatedly after re-renders.
 * @param {(url: string, options?: RequestInit) => Promise<Response>} apiFetch
 */
export function loadFeedIcons(apiFetch) {
  const container = document.getElementById("folder-container");
  if (!container || typeof apiFetch !== "function") return;

  container.querySelectorAll("img[data-feed-icon]").forEach((img) => {
    const feedId = img.dataset.feedIcon;
    if (!feedId || img.dataset.iconLoaded === "1") return;
    img.dataset.iconLoaded = "1";

    void (async () => {
      try {
        const response = await apiFetch(`./api/icon/${feedId}`);
        if (!response.ok) return;
        const blob = await response.blob();
        const previous = feedIconObjectUrls.get(feedId);
        if (previous) URL.revokeObjectURL(previous);
        const objectUrl = URL.createObjectURL(blob);
        feedIconObjectUrls.set(feedId, objectUrl);
        // Element may have been replaced by a re-render; re-query.
        const live = container.querySelector(
          `img[data-feed-icon="${feedId}"]`,
        );
        if (live) live.src = objectUrl;
      } catch (err) {
        console.warn("Failed to load feed icon", feedId, err);
      }
    })();
  });
}

/**
 * Build folder details HTML.
 * @param {Folder} folder
 * @param {Record<string, boolean>} openStates
 */
function folderHtml(folder, openStates) {
  const openAttr =
    Object.prototype.hasOwnProperty.call(openStates, folder.id) &&
    openStates[folder.id]
      ? " open"
      : "";
  const color = folder.color || "f-base";
  const folderUnread = (folder.feeds ?? []).reduce(
    (sum, feed) => sum + (Number(feed.unreadCount) || 0),
    0,
  );
  const feeds = (folder.feeds ?? []).map(feedHtml).join("");
  return (
    html`<details data-folder-id="${folder.id}"` +
    openAttr +
    html`>
    <summary>
      <img src="icons/folder.svg" alt="" class="icon ${color}" />
      <span class="tree-name">${folder.name || ""}</span>
      ` +
    unreadBadgeHtml(folderUnread) +
    html`<span class="tree-options" role="button" tabindex="0" aria-label="Folder options">⋮</span>
    </summary>
    <div class="folder-container">
      <ul class="feeds-ul">` +
    feeds +
    html`</ul>
    </div>
  </details>`
  );
}

/**
 * Apply persisted open/closed state onto an already-painted folder tree.
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
 * Canonical folder-tree HTML for live render (open state from folder-state).
 * @param {Folder[]} folders
 * @param {Feed[]} unfiledFeeds
 * @param {Record<string, boolean>} [openStates]
 * @returns {string}
 */
export function buildFoldersHtml(folders, unfiledFeeds, openStates = {}) {
  const folderPart = (folders ?? []).map((f) => folderHtml(f, openStates)).join("");
  const unfiled =
    unfiledFeeds && unfiledFeeds.length > 0
      ? "<ul class=\"feeds-ul\">" + unfiledFeeds.map(feedHtml).join("") + "</ul>"
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
  const markup = buildFoldersHtml(folders, unfiledFeeds);
  container.innerHTML = markup;
  delete container.dataset.cachePainted;
  syncFolderOpenStatesFromStorage();
  updateAllFeedsUnreadBadge(totalUnreadCount(folders, unfiledFeeds));
  return markup;
}

/**
 * Mark cache-painted icons as needing auth hydration (boot HTML may still
 * use the old {@code src="./api/icon/…"} form).
 */
export function prepareCachedIconsForHydration() {
  const container = document.getElementById("folder-container");
  if (!container) return;
  container.querySelectorAll("img.tree-entry-icon").forEach((img) => {
    const feedLi = img.closest("li[data-feed-id]");
    if (!feedLi) return;
    const feedId = feedLi.dataset.feedId;
    img.dataset.feedIcon = feedId;
    delete img.dataset.iconLoaded;
    if (!img.src.includes("icons/rss.svg")) {
      img.src = "icons/rss.svg";
    }
  });
}

/**
 * Creates a single article DOM element (clicks via delegation).
 * @param {Article} article
 * @returns {HTMLElement}
 */
/**
 * @param {string} className
 * @param {{ src: string, label: string }} flag
 * @returns {HTMLButtonElement}
 */
function flagButton(className, flag) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `item-spacer ${className}`;
  const img = document.createElement("img");
  img.alt = "";
  button.appendChild(img);
  paintFlag(button, flag);
  return button;
}

/**
 * @param {HTMLButtonElement} button
 * @param {{ src: string, label: string, tone: string }} flag
 */
function paintFlag(button, flag) {
  button.setAttribute("aria-label", flag.label);
  button.title = flag.label;
  const img = button.querySelector("img");
  if (!img) return;
  img.src = flag.src;
  img.className = `icon ${flag.tone}`;
}

function createArticleElement(article) {
  const articleDiv = document.createElement("div");
  const headerDiv = document.createElement("div");
  const imageDiv = document.createElement("div");
  const titleDiv = document.createElement("div");
  const sourceSpan = document.createElement("span");
  const ageSpan = document.createElement("span");
  const title = article.title || "No Title";

  articleDiv.className = "article";
  articleDiv.dataset.articleId = String(article.id);
  articleDiv.tabIndex = 0;
  articleDiv.setAttribute(
    "aria-label",
    `${title}, ${article.read ? "Read" : "Unread"}`,
  );
  if (!article.read) {
    articleDiv.classList.add("unread");
  }
  if (selectedArticleId != null && article.id === selectedArticleId) {
    articleDiv.classList.add("selected");
  }

  const metaEnd = document.createElement("div");
  metaEnd.className = "article-meta-end";
  const body = document.createElement("div");
  body.className = "article-body";
  headerDiv.className = "article-header";
  imageDiv.className = "article-image";
  titleDiv.className = "article-title";
  sourceSpan.className = "source";
  ageSpan.className = "age";

  titleDiv.textContent = title;
  sourceSpan.textContent = article.feedName || "Unknown";
  ageSpan.textContent = getRelativeTime(article.published) || "";

  body.appendChild(titleDiv);

  if (article.imageUrl && isSafeHttpUrl(article.imageUrl)) {
    const image = document.createElement("img");
    image.loading = "lazy";
    image.decoding = "async";
    image.referrerPolicy = "no-referrer";
    image.src = article.imageUrl;
    image.alt = "";
    imageDiv.appendChild(image);
    body.appendChild(imageDiv);
  }

  headerDiv.appendChild(sourceSpan);
  metaEnd.appendChild(ageSpan);
  metaEnd.appendChild(flagButton("article-mark", articleMarkState(!!article.read)));
  metaEnd.appendChild(flagButton("article-star", articleStarState(!!article.saved)));
  headerDiv.appendChild(metaEnd);
  body.appendChild(headerDiv);
  articleDiv.appendChild(body);
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
  syncReaderSaveButton(article);
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
  syncReaderSaveButton(null);
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
