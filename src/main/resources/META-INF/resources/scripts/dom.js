import {
  articleClickListener,
  feedClickListener,
  feedContextMenu,
  folderClickListener,
  folderContextMenu,
  openAddContextMenu,
} from "./main.js";
import { getRelativeTime, parseDate, sanitizeHTML } from "./util.js";

const FOLDER_STATE_KEY = "folder-state";

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
 * Renders a list of articles to the DOM.
 *
 * @param {Article[]} articles - The array of articles to render.
 * @returns {void}
 */
export function renderArticlesList(articles) {
  const container = document.querySelector(
    "#articles-list #articles-container",
  );
  container.innerHTML = ""; // Clear previous content

  for (let i = 0; i < articles.length; i++) {
    const article = articles[i];
    const articleDiv = document.createElement("div");
    const headerDiv = document.createElement("div");
    const imageDiv = document.createElement("div");
    const titleDiv = document.createElement("div");
    const sourceSpan = document.createElement("span");
    const ageSpan = document.createElement("span");

    articleDiv.className = "article";
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
      imageDiv.appendChild(image);
      articleDiv.appendChild(imageDiv);
    }
    articleDiv.appendChild(titleDiv);

    articleDiv.addEventListener("click", () => {
      articleClickListener(article);
    });

    container.appendChild(articleDiv);
  }
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

  const nameSpan = document.createElement("span");
  nameSpan.textContent = feed.name || "";
  nameSpan.className = "tree-name";

  li.addEventListener("click", (e) => {
    feedClickListener(feed);
  });

  const options = document.createElement("span");
  options.classList.add("tree-options");
  options.textContent = "⋮";
  options.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    feedContextMenu(e.clientX, e.clientY, feed);
  });

  li.appendChild(icon);
  li.appendChild(nameSpan);
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
 * @param {Folder[]} folders
 */
export function renderFoldersList(folders) {
  const container = document.getElementById("folder-container");
  const noFolderFeeds = document.createElement("ul");

  container.innerHTML = "";

  folders.forEach((folder) => {
    if (folder.id === 0) {
      // folder 0 => feeds without folder
      noFolderFeeds.className = "feeds-ul";
      folder.feeds.forEach((feed) => {
        noFolderFeeds.appendChild(createFeedElement(feed));
      });
      return;
    }

    const details = document.createElement("details");
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

  container.appendChild(noFolderFeeds);
  container.appendChild(addElement());
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
 * Creates the "Add Feed/Folder" "Button"
 * @returns {HTMLDivElement}
 */
function addElement() {
  const img = document.createElement("img");
  img.src = "icons/feed_add.svg";
  img.classList.add("icon", "f-grey");

  const span = document.createElement("span");
  span.textContent = "Add";
  span.className = "tree-name";

  const details = document.createElement("div");
  details.classList.add("space-top", "details");
  details.addEventListener("click", (e) => {
    e.stopPropagation();
    openAddContextMenu(e.clientX, e.clientY);
  });
  const summary = document.createElement("div");
  summary.className = "summary";
  summary.appendChild(img);
  summary.appendChild(span);
  details.appendChild(summary);
  return details;
}
