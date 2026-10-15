"use strict";

export const columns = Object.freeze({
  FEEDS: "feeds-list",
  ARTICLES: "articles-list",
  READER: "reader",
});

/**
 * Owns mobile/medium pane visibility. History and URLs are handled by main.js.
 */
export class NavigationService {
  #domReadyListener;
  #pageshowHandler;

  currentState = { column: columns.FEEDS };

  constructor() {
    if (document.readyState === "loading") {
      this.#domReadyListener = () => this.#init();
      document.addEventListener("DOMContentLoaded", this.#domReadyListener);
    } else {
      requestAnimationFrame(() => this.#init());
    }
  }

  #init() {
    this.feedsEl = document.getElementById("feeds-list");
    this.articlesEl = document.getElementById("articles-list");
    this.readerEl = document.getElementById("reader");

    if (!this.feedsEl || !this.articlesEl || !this.readerEl) {
      console.warn("NavigationService: Column elements not found in DOM");
      return;
    }

    this.#renderColumn(this.currentState.column);

    this.#pageshowHandler = () => {
      this.#renderColumn(this.currentState.column);
    };
    window.addEventListener("pageshow", this.#pageshowHandler);
  }

  /**
   * Show a column without touching history.
   * @param {string} column
   */
  navigateTo(column) {
    if (!Object.values(columns).includes(column)) {
      console.warn(`Invalid column: ${column}`);
      return;
    }

    if (this.currentState.column === column) {
      this.#renderColumn(column);
      return;
    }

    this.currentState = { column };
    this.#renderColumn(column);
  }

  #renderColumn(column) {
    if (!this.feedsEl || !this.articlesEl || !this.readerEl) return;

    [this.feedsEl, this.articlesEl, this.readerEl].forEach((el) =>
      el.classList.remove("active", "primary"),
    );

    switch (column) {
      case columns.FEEDS:
        this.feedsEl.classList.add("active", "primary");
        this.articlesEl.classList.add("active");
        this.feedsEl.focus?.();
        break;
      case columns.ARTICLES:
        this.feedsEl.classList.add("active");
        this.articlesEl.classList.add("active", "primary");
        this.articlesEl.focus?.();
        break;
      case columns.READER:
        this.readerEl.classList.add("active", "primary");
        this.readerEl.focus?.();
        break;
      default:
        console.error(`Unknown column: ${column}`);
    }
  }

  destroy() {
    if (this.#domReadyListener) {
      document.removeEventListener("DOMContentLoaded", this.#domReadyListener);
      this.#domReadyListener = null;
    }
    window.removeEventListener("pageshow", this.#pageshowHandler);
  }
}
