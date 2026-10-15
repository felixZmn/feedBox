"use strict";

/**
 * Compact unread count badge HTML. Zero / missing counts render nothing.
 * @param {number} [count]
 * @returns {string}
 */
export function unreadBadgeHtml(count) {
  const n = Number(count) || 0;
  if (n <= 0) return "";
  return `<span class="tree-unread" aria-label="${n} unread">${n}</span>`;
}

/**
 * Sum unread counts across folders and unfiled feeds for the All Feeds row.
 * @param {Folder[]} folders
 * @param {Feed[]} unfiledFeeds
 * @returns {number}
 */
export function totalUnreadCount(folders, unfiledFeeds) {
  let total = 0;
  for (const folder of folders ?? []) {
    for (const feed of folder.feeds ?? []) {
      total += Number(feed.unreadCount) || 0;
    }
  }
  for (const feed of unfiledFeeds ?? []) {
    total += Number(feed.unreadCount) || 0;
  }
  return total;
}
