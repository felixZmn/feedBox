import { getRelativeTime, parseDate } from "./util.js";
import {
  feedClickListener,
  folderClickListener,
  articleClickListener,
  openAddContextMenu,
  folderContextMenu,
  feedContextMenu,
} from "./main.js";

export function renderArticlesList(articles) {
  const container = document.querySelector(
    "#articles-list #articles-container"
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

export function renderReaderView(article) {
  const title = document.querySelector("#reader .title");
  const content = document.querySelector("#reader .content");
  const publisher = document.querySelector("#reader-publisher");
  const date = document.querySelector("#reader .date");
  const externalLink = document.querySelector("#trigger-external-open");

  title.innerText = article.title || "No Title";
  content.innerHTML = article.content || article.description || "No Content";
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
 * @param {*} feed
 * @returns
 */
function createFeedElement(feed) {
  const li = document.createElement("li");
  const icon = document.createElement("img");
  icon.src = "./api/icons/" + feed.id;
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
    feedContextMenu(e.pageX, e.pageY, feed);
  });

  li.appendChild(icon);
  li.appendChild(nameSpan);
  li.appendChild(options);
  li.dataset.feedId = feed.id;
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
 * @param {*} folders
 */
export function renderFoldersList(folders) {
  const container = document.getElementById("folder-container");
  const noFolderFeeds = document.createElement("ul");

  container.innerHTML = "";

  folders.forEach((folder) => {
    if (folder.id == 0) {
      // folder 0 => feeds without folder
      noFolderFeeds.className = "feeds-ul";
      folder.feeds.forEach((feed) => {
        noFolderFeeds.appendChild(createFeedElement(feed));
      });
      return;
    }
    const details = document.createElement("details");
    details.appendChild(createFolderElement(folder));

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
    container.appendChild(noFolderFeeds);
  });

  container.appendChild(addElement());
}

function createFolderElement(folder) {
  const summary = document.createElement("summary");
  const img = document.createElement("img");
  img.src = "icons/folder.svg";
  img.classList.add("icon", folder.color);

  const nameSpan = document.createElement("span");
  nameSpan.textContent = folder.name || "";
  nameSpan.className = "tree-name";
  nameSpan.addEventListener("click", (e) => {
    folderClickListener(folder);
  });

  const options = document.createElement("span");
  options.classList.add("tree-options");
  options.textContent = "⋮";
  options.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    folderContextMenu(e.pageX, e.pageY, folder);
  });

  summary.appendChild(img);
  summary.appendChild(nameSpan);
  summary.appendChild(options);

  return summary;
}

/**
 * Creates the "Add Feed/Folder" "Button"
 * @returns
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
    openAddContextMenu(e.pageX, e.pageY);
  });
  const summary = document.createElement("div");
  summary.className = "summary";
  summary.appendChild(img);
  summary.appendChild(span);
  details.appendChild(summary);
  return details;
}

/**
 * this method gets called after the folder list is recieved from the backend
 * to populate the folder dropdown in the "add feed" dialog
 *
 * ToDo: this method could only store the folders in a global state
 * and the dropdown could use this as source of data
 */
export function folderDropdownOptions(folders) {
  const select = document.getElementById("feed-folder");
  select.innerHTML = '<option value="0">No Folder</option>'; // Reset options

  folders.forEach((folder) => {
    if (folder.id == 0) {
      return; // Skip "no folder" option
    }
    const option = document.createElement("option");
    option.value = folder.id;
    option.textContent = folder.name;
    select.appendChild(option);
  });
}
