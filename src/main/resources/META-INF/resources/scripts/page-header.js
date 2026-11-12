"use strict";

/** Fixed Profile control — no IdP photo in this feature. */
export const PROFILE_LABEL = "Profile";
export const PROFILE_ICON_PATH = "icons/profile_placeholder.svg";

/** Chrome menu labels in display order (Import → Export → Shortcuts → Sign Out). */
export const CHROME_MENU_ORDER = Object.freeze([
  "Import OPML",
  "Export OPML",
  "Keyboard shortcuts",
  "Sign Out",
]);

export const layoutBands = Object.freeze({
  WIDE: "wide",
  MEDIUM: "medium",
  NARROW: "narrow",
});

/**
 * @param {number} width
 * @returns {"wide"|"medium"|"narrow"}
 */
export function layoutBandFromWidth(width) {
  if (width <= 700) return layoutBands.NARROW;
  if (width <= 1100) return layoutBands.MEDIUM;
  return layoutBands.WIDE;
}

/**
 * Page-header back visibility per contracts/page-header.md.
 * @param {string} primaryColumn columns.FEEDS | ARTICLES | READER
 * @param {"wide"|"medium"|"narrow"} layoutBand
 * @returns {{ visible: boolean, label: string, target: "feeds"|"articles"|null }}
 */
export function pageBackState(primaryColumn, layoutBand) {
  if (layoutBand === layoutBands.WIDE) {
    return { visible: false, label: "", target: null };
  }
  if (primaryColumn === "reader") {
    return {
      visible: true,
      label: "Back to articles",
      target: "articles",
    };
  }
  if (
    layoutBand === layoutBands.NARROW &&
    primaryColumn === "articles-list"
  ) {
    return { visible: true, label: "Back to feeds", target: "feeds" };
  }
  return { visible: false, label: "", target: null };
}

/**
 * Reader action header is shown only while an article is open.
 * @param {boolean} articleOpen
 * @returns {boolean}
 */
export function readerActionsVisible(articleOpen) {
  return !!articleOpen;
}

/**
 * @param {HTMLElement | null | undefined} button
 * @param {{ visible: boolean, label: string, target: "feeds"|"articles"|null }} state
 */
export function applyPageBack(button, state) {
  if (!button) return;
  button.hidden = !state.visible;
  if (state.visible) {
    button.title = state.label;
    button.setAttribute("aria-label", state.label);
    button.dataset.backTarget = state.target ?? "";
  } else {
    button.removeAttribute("title");
    button.removeAttribute("aria-label");
    delete button.dataset.backTarget;
  }
}

/**
 * @param {HTMLElement | null | undefined} readerEl
 * @param {boolean} open
 */
export function setReaderHasArticle(readerEl, open) {
  readerEl?.classList.toggle("has-article", !!open);
}
