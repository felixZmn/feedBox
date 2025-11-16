export const columns = Object.freeze({
  FEEDS: "feeds",
  ARTICLES: "articles-list",
  READER: "reader",
});

export class NavigationService {
  constructor() {
    this.currentState = { column: columns.FEEDS };
    this.feedsEl = document.getElementById("feeds");
    this.articlesEl = document.getElementById("articles-list");
    this.readerEl = document.getElementById("reader");

    this.#renderColumn(this.currentState.column);

    window.addEventListener("popstate", (event) => {
      const state = event.state || { column: columns.FEEDS };
      this.currentState = state;
      this.#renderColumn(this.currentState.column);
    });
  }

  navigateTo(column) {
    const prevColumn = this.currentState.column;
    const newState = { column };

    if (prevColumn === columns.READER && column === columns.READER) {
      history.replaceState(newState, "", this.#buildUrl(newState));
    } else {
      history.pushState(newState, "", this.#buildUrl(newState));
    }

    this.currentState = newState;
    this.#renderColumn(this.currentState.column);
  }

  #renderColumn(column) {
    [this.feedsEl, this.articlesEl, this.readerEl].forEach((el) =>
      el.classList.remove("active", "primary")
    );

    switch (column) {
      case columns.FEEDS:
        // Feeds+articles mode, feeds is primary
        this.feedsEl.classList.add("active", "primary");
        this.articlesEl.classList.add("active");
        this.feedsEl.focus();
        break;

      case columns.ARTICLES:
        // Feeds+articles mode, articles is primary
        this.feedsEl.classList.add("active");
        this.articlesEl.classList.add("active", "primary");
        this.articlesEl.focus();
        break;

      case columns.READER:
        // Reader mode
        this.readerEl.classList.add("active", "primary");
        this.readerEl.focus();
        break;
    }
  }

  #buildUrl() {
    return "/"; // for now
  }
}
