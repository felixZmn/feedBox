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
 * Update or remove a `.tree-unread` badge under a parent element in place.
 * @param {Element|null} parent
 * @param {number} count
 * @param {Element|null} [beforeEl] insert badge before this sibling when creating
 */
export function syncUnreadBadge(parent, count, beforeEl = null) {
  if (!parent) return;
  let badge = parent.querySelector(":scope > .tree-unread");
  const n = Number(count) || 0;
  if (n <= 0) {
    badge?.remove();
    return;
  }
  if (!badge) {
    badge = document.createElement("span");
    badge.className = "tree-unread";
    if (beforeEl && beforeEl.parentNode === parent) {
      parent.insertBefore(badge, beforeEl);
    } else {
      parent.appendChild(badge);
    }
  }
  badge.textContent = String(n);
  badge.setAttribute("aria-label", `${n} unread`);
}

/**
 * In-place feed unread badge update (no sidebar re-render).
 * @param {number|string} feedId
 * @param {number} count
 */
export function setFeedUnreadBadge(feedId, count) {
  const li = document.querySelector(
    `#folder-container li[data-feed-id="${feedId}"]`,
  );
  if (!li) return;
  syncUnreadBadge(li, count, li.querySelector(":scope > .tree-options"));
}

/**
 * In-place folder unread badge update (summary row).
 * @param {number|string} folderId
 * @param {number} count
 */
export function setFolderUnreadBadge(folderId, count) {
  const details = document.querySelector(
    `#folder-container details[data-folder-id="${folderId}"]`,
  );
  const summary = details?.querySelector(":scope > summary");
  if (!summary) return;
  syncUnreadBadge(
    summary,
    count,
    summary.querySelector(":scope > .tree-options"),
  );
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
