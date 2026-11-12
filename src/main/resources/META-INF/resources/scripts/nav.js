"use strict";

import {
  applyPageBack,
  layoutBandFromWidth,
  pageBackState,
} from "./page-header.js";

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
  /** @type {MediaQueryList | null} */
  #narrowPanes = null;
  /** @type {MediaQueryList | null} */
  #onePane = null;
  /** @type {(() => void) | null} */
  #layoutChangeHandler = null;

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
    this.#narrowPanes = window.matchMedia("(max-width: 1100px)");
    this.#onePane = window.matchMedia("(max-width: 700px)");

    if (!this.feedsEl || !this.articlesEl || !this.readerEl) {
      console.warn("NavigationService: Column elements not found in DOM");
      return;
    }

    this.#renderColumn(this.currentState.column, { moveFocus: false });

    this.#pageshowHandler = () => {
      this.#renderColumn(this.currentState.column, { moveFocus: false });
    };
    window.addEventListener("pageshow", this.#pageshowHandler);

    this.#layoutChangeHandler = () => {
      this.#syncPageHeaderBack();
    };
    this.#narrowPanes.addEventListener?.("change", this.#layoutChangeHandler);
    this.#onePane.addEventListener?.("change", this.#layoutChangeHandler);
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

    const changed = this.currentState.column !== column;
    if (changed) {
      this.currentState = { column };
    }
    this.#renderColumn(column, { moveFocus: changed });
  }

  /**
   * @param {string} column
   * @param {{ moveFocus?: boolean }} [options]
   */
  #renderColumn(column, { moveFocus = false } = {}) {
    if (!this.feedsEl || !this.articlesEl || !this.readerEl) return;

    [this.feedsEl, this.articlesEl, this.readerEl].forEach((el) =>
      el.classList.remove("active", "primary"),
    );

    switch (column) {
      case columns.FEEDS:
        this.feedsEl.classList.add("active", "primary");
        this.articlesEl.classList.add("active");
        break;
      case columns.ARTICLES:
        this.feedsEl.classList.add("active");
        this.articlesEl.classList.add("active", "primary");
        break;
      case columns.READER:
        this.readerEl.classList.add("active", "primary");
        break;
      default:
        console.error(`Unknown column: ${column}`);
        return;
    }

    this.#syncPageHeaderBack();

    if (!moveFocus || !this.#narrowPanes?.matches) return;
    this.#focusColumn(column);
  }

  #syncPageHeaderBack() {
    const button = document.getElementById("trigger-page-back");
    const band = layoutBandFromWidth(window.innerWidth);
    applyPageBack(button, pageBackState(this.currentState.column, band));
  }

  /**
   * @param {string} column
   */
  #focusColumn(column) {
    if (column === columns.READER) {
      this.readerEl.querySelector("h1[tabindex='-1']")?.focus();
      return;
    }
    if (column === columns.ARTICLES) {
      this.articlesEl
        .querySelector(".article.selected .article-link")
        ?.focus();
      return;
    }
    if (column === columns.FEEDS) {
      this.feedsEl.querySelector(".selected .tree-link, .selected a")?.focus();
    }
  }

  destroy() {
    if (this.#domReadyListener) {
      document.removeEventListener("DOMContentLoaded", this.#domReadyListener);
      this.#domReadyListener = null;
    }
    window.removeEventListener("pageshow", this.#pageshowHandler);
    if (this.#layoutChangeHandler) {
      this.#narrowPanes?.removeEventListener?.(
        "change",
        this.#layoutChangeHandler,
      );
      this.#onePane?.removeEventListener?.(
        "change",
        this.#layoutChangeHandler,
      );
    }
  }
}
