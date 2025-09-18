export const columns = Object.freeze({
  FEEDS: "feeds",
  ARTICLES: "articlesList",
  READER: "reader",
});

export class Navigator {
  constructor() {
    this.currentState = { column: columns.FEEDS };
    this.feedsEl = document.getElementById("feeds");
    this.articlesEl = document.getElementById("articlesList");
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
      el.classList.remove("active")
    );

    switch (column) {
      case columns.ARTICLES:
        this.articlesEl.classList.add("active");
        this.articlesEl.focus();
        break;
      case columns.FEEDS:
        this.feedsEl.classList.add("active");
        this.feedsEl.focus();
        break;
      case columns.READER:
        this.readerEl.classList.add("active");
        this.readerEl.focus();
        break;
    }
  }

  #buildUrl() {
    return "/"; // for now
  }
}
