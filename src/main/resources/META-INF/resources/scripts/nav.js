"use strict";

export const columns = Object.freeze({
  FEEDS: "feeds-list",
  ARTICLES: "articles-list",
  READER: "reader",
});

export class NavigationService {
  #popstateHandler;
  #domReadyListener;
  #pageshowHandler;

  currentState = { column: columns.FEEDS };

  constructor() {
    this.#popstateHandler = (event) => {
      if (this.#isAuthCallback()) return;

      let rawColumn = event.state?.column;
      if (!Object.values(columns).includes(rawColumn)) {
        rawColumn = columns.FEEDS;
        history.replaceState({ column: rawColumn }, "", "");
      }

      this.currentState = { column: rawColumn };
      this.#renderColumn(rawColumn);
    };

    // Mobile-friendly initialization
    if (document.readyState === "loading") {
      this.#domReadyListener = () => this.#init();
      document.addEventListener("DOMContentLoaded", this.#domReadyListener);
    } else {
      // Defer slightly on desktop to match mobile behavior
      requestAnimationFrame(() => this.#init());
    }
  }

  #init() {
    this.feedsEl = document.getElementById("feeds-list");
    this.articlesEl = document.getElementById("articles-list");
    this.readerEl = document.getElementById("reader");

    // Guard against missing elements
    if (!this.feedsEl || !this.articlesEl || !this.readerEl) {
      console.warn("NavigationService: Column elements not found in DOM");
      return;
    }

    if (this.#isAuthCallback()) {
      console.info("Auth callback detected. Initializing with fallback state.");
    }

    // Restore state if valid, otherwise default to FEEDS
    const savedColumn = history.state?.column;
    this.currentState = {
      column:
        savedColumn && Object.values(columns).includes(savedColumn)
          ? savedColumn
          : columns.FEEDS,
    };

    requestAnimationFrame(() => {
      history.replaceState(this.currentState, "", "");
      this.#renderColumn(this.currentState.column);
    });

    window.addEventListener("popstate", this.#popstateHandler);

    this.#pageshowHandler = (event) => {
      this.#renderColumn(this.currentState.column);
    };
    window.addEventListener("pageshow", this.#pageshowHandler);
  }

  navigateTo(column) {
    if (!Object.values(columns).includes(column)) {
      console.warn(`Invalid column: ${column}`);
      return;
    }

    if (this.#isAuthCallback()) return;
    if (this.currentState.column === column) return;

    this.currentState = { column };
    history.pushState(this.currentState, "", "");
    this.#renderColumn(column);
  }

  #isAuthCallback() {
    const params = new URLSearchParams(window.location.search);
    return (
      params.has("code") || params.has("state") || params.has("session_state")
    );
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
    window.removeEventListener("popstate", this.#popstateHandler);
    window.removeEventListener("pageshow", this.#pageshowHandler);
  }
}
