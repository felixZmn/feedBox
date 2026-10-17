"use strict";

/**
 * Status mark. Outline means still unread; filled means read.
 * @param {boolean} read
 * @returns {{ src: string, label: string }}
 */
export function articleMarkState(read) {
  if (read) {
    return { src: "icons/circle-check-filled.svg", label: "Read", tone: "f-grey" };
  }
  return { src: "icons/circle-check.svg", label: "Mark as read", tone: "f-blue" };
}

/**
 * Bookmark. Filled means saved.
 * @param {boolean} saved
 * @returns {{ src: string, label: string }}
 */
export function articleStarState(saved) {
  if (saved) {
    return { src: "icons/bookmark-filled.svg", label: "Remove from Saved", tone: "f-blue" };
  }
  return { src: "icons/bookmark.svg", label: "Save article", tone: "f-grey" };
}
