const feedsEl = document.getElementById("feeds");
const articlesEl = document.getElementById("articlesList");
const readerEl = document.getElementById("reader");
const scrollStore = { feeds: 0, articles: {}, reader: {} };

export function showView(view) {
  [feedsEl, articlesEl, readerEl].forEach((el) =>
    el.classList.remove("active")
  );

  if (view === "feeds") {
    feedsEl.classList.add("active");
    feedsEl.focus();
  } else if (view === "articlesList") {
    articlesEl.classList.add("active");
    articlesEl.focus();
  } else if (view === "reader") {
    readerEl.classList.add("active");
    readerEl.focus();
  }
}

export function navigateToFeeds() {
  history.pushState({ view: "feeds" }, "", `/`);
  showView("feeds");
}

export function navigateToArticlesList() {
  history.pushState({ view: "articlesList" }, "", `/`);
  showView("articlesList");
}

export function navigateToReader() {
  history.pushState({ view: "reader" }, "", `/`);
  showView("reader");
}
