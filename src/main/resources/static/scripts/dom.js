import { getRelativeTime, parseDate } from "./util.js";
import {
  feedClickListener,
  folderClickListener,
  scrollObserver,
  articleClickListener,
  allFeedsClickListener,
} from "./main.js";

export function renderArticlesList(articles) {
  const container = document.querySelector("#articlesList .column");
  container.innerHTML = ""; // Clear previous content

  for (let i = 0; i < articles.length; i++) {
    const article = articles[i];
    const articleDiv = document.createElement("div");
    const headerDiv = document.createElement("div");
    const titleDiv = document.createElement("div");
    const sourceSpan = document.createElement("span");
    const ageSpan = document.createElement("span");

    articleDiv.className = "article";
    headerDiv.className = "articleHeader";
    titleDiv.className = "title";
    sourceSpan.className = "source";
    ageSpan.className = "age";

    titleDiv.innerText = article.title || "No Title";
    sourceSpan.innerText = article.feedName || "Unknown";
    ageSpan.innerText = getRelativeTime(article.published);

    headerDiv.appendChild(sourceSpan);
    headerDiv.appendChild(ageSpan);

    articleDiv.appendChild(headerDiv);
    articleDiv.appendChild(titleDiv);

    articleDiv.addEventListener("click", () => {
      articleClickListener(article);
    });

    if (i == articles.length - 1) {
      var observer = scrollObserver();
      observer.observe(articleDiv);
    }

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

function createFeedElement(feed) {
  const li = document.createElement("li");
  const icon = document.createElement("img");
  icon.src = "./icons/rss.svg"; // ToDo: replace with actual icon
  icon.className = "feed-icon";

  li.appendChild(icon);
  li.appendChild(document.createTextNode(feed.name || "Unnamed Feed"));
  li.addEventListener("click", (e) => {
    feedClickListener(feed);
  });
  return li;
}

export function renderFoldersList(folders) {
  const container = document.getElementById("feeds-col");
  const noFolderFeeds = document.createElement("ul");

  container.innerHTML = "";
  container.appendChild(viewAllFeedsElement());

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
    feedsContainer.className = "folderContainer";

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
}

function createFolderElement(folder) {
  const summary = document.createElement("summary");
  const img = document.createElement("img");
  img.src = "icons/folder.svg";
  img.classList.add("icon", folder.color);

  const nameSpan = document.createElement("span");
  nameSpan.textContent = folder.name || "";
  nameSpan.className = "folder-name";
  nameSpan.addEventListener("click", (e) => {
    folderClickListener(folder);
  });

  summary.appendChild(img);
  summary.appendChild(nameSpan);
  return summary;
}

// Creates the "All Feeds" Element
function viewAllFeedsElement() {
  const img = document.createElement("img");
  img.src = "icons/package.svg";
  img.classList.add("icon", "f-base");

  const span = document.createElement("span");
  span.textContent = "All Feeds";
  span.className = "folder-name";

  const details = document.createElement("div");
  details.className = "details";
  details.addEventListener("click", () => {
    allFeedsClickListener();
  });
  const summary = document.createElement("div");
  summary.className = "summary";
  summary.appendChild(img);
  summary.appendChild(span);
  details.appendChild(summary);
  return details;
}

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
