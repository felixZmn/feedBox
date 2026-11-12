"use strict";

import { dom, itemType, state } from "./state.js";
import { feedHealth } from "./feed-health.js";

/** @type {HTMLElement | null} */
let menuTrigger = null;

/**
 * @param {string} groupClass
 * @param {number} x
 * @param {number} y
 * @param {EventTarget | null} [trigger]
 * @param {{ align?: "start" | "end" }} [options]
 */
function showContextMenuGroup(groupClass, x, y, trigger, options) {
  dom.contextMenu
    .querySelectorAll(
      ".context-menu-item, .context-menu-status, .context-menu-separator",
    )
    .forEach((element) => {
      element.classList.add("d-none");
    });
  document.querySelectorAll(`.${groupClass}`).forEach((element) => {
    element.classList.remove("d-none");
  });
  openContextMenu(x, y, trigger, options);
}

/**
 * @param {number} x
 * @param {number} y
 * @param {EventTarget | null} [trigger]
 */
export function openAddContextMenu(x, y, trigger) {
  showContextMenuGroup("context-add", x, y, trigger);
}

/**
 * @param {number} x
 * @param {number} y
 * @param {object} feed
 * @param {EventTarget | null} [trigger]
 */
export function feedContextMenu(x, y, feed, trigger) {
  state.lastClickedItem.type = itemType.FEED;
  state.lastClickedItem.obj = feed;
  if (dom.feedHealth) {
    const { text, isError } = feedHealth(feed);
    dom.feedHealth.textContent = text;
    dom.feedHealth.classList.toggle("is-error", isError);
  }
  setMarkReadMenuLabel(dom.button.markFeedRead, Number(feed?.unreadCount) || 0);
  showContextMenuGroup("context-feed", x, y, trigger);
}

/**
 * @param {number} x
 * @param {number} y
 * @param {object} folder
 * @param {EventTarget | null} [trigger]
 */
export function folderContextMenu(x, y, folder, trigger) {
  state.lastClickedItem.type = itemType.FOLDER;
  state.lastClickedItem.obj = folder;
  const live = state.folders.find((f) => f.id === folder?.id) ?? folder;
  const count = (live?.feeds ?? []).reduce(
    (sum, feed) => sum + (Number(feed.unreadCount) || 0),
    0,
  );
  setMarkReadMenuLabel(dom.button.markFolderRead, count);
  showContextMenuGroup("context-folder", x, y, trigger);
}

/**
 * @param {number} x
 * @param {number} y
 * @param {EventTarget | null} [trigger]
 */
export function allFeedsContextMenu(x, y, trigger) {
  state.lastClickedItem.type = itemType.ALL;
  state.lastClickedItem.obj = null;
  let count = 0;
  for (const folder of state.folders) {
    for (const feed of folder.feeds ?? []) {
      count += Number(feed.unreadCount) || 0;
    }
  }
  for (const feed of state.unfiledFeeds) {
    count += Number(feed.unreadCount) || 0;
  }
  setMarkReadMenuLabel(dom.button.markAllRead, count);
  showContextMenuGroup("context-all", x, y, trigger);
}

/**
 * Profile / chrome overflow: import, export, shortcuts, sign out.
 * @param {number} x
 * @param {number} y
 * @param {EventTarget | null} [trigger]
 * @param {{ align?: "start" | "end" }} [options]
 */
export function openChromeContextMenu(x, y, trigger, options) {
  showContextMenuGroup("context-chrome", x, y, trigger, options);
}

/**
 * @param {HTMLElement | null | undefined} el
 * @param {number} count
 */
function setMarkReadMenuLabel(el, count) {
  if (!el) return;
  el.textContent =
    count > 0 ? `Mark ${count} as read` : "Mark as read";
}

/**
 * @param {number} x  Left edge for start align; right edge for end align
 * @param {number} y
 * @param {EventTarget | null} [trigger]
 * @param {{ align?: "start" | "end" }} [options]
 */
export function openContextMenu(x, y, trigger, options = {}) {
  const el =
    trigger instanceof HTMLElement
      ? trigger
      : document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
  menuTrigger = el;

  const align = options.align === "end" ? "end" : "start";
  const menu = dom.contextMenu;
  menu.style.visibility = "hidden";
  menu.style.left = "0px";
  menu.style.top = "0px";
  menu.classList.add("is-open");
  const { innerWidth, innerHeight } = window;
  const menuRect = menu.getBoundingClientRect();
  const triggerRect = el?.getBoundingClientRect();
  const edge = 8;
  const anchorX =
    align === "end" && triggerRect ? triggerRect.right : x;
  const left =
    align === "end"
      ? Math.max(
          edge,
          Math.min(
            anchorX - menuRect.width,
            innerWidth - menuRect.width - edge,
          ),
        )
      : Math.max(edge, Math.min(anchorX, innerWidth - menuRect.width - edge));
  const top = Math.min(
    y,
    Math.max(edge, innerHeight - menuRect.height - edge),
  );
  menu.style.left = `${left}px`;
  menu.style.top = `${top}px`;
  menu.style.visibility = "";
  getVisibleContextMenuItems()[0]?.focus();
}

/**
 * @param {{ restoreFocus?: boolean }} [options]
 */
export function hideContextMenu({ restoreFocus = false } = {}) {
  dom.contextMenu.classList.remove("is-open");
  if (restoreFocus) restoreMenuTriggerFocus();
}

/** Return focus to the control that opened the menu. */
export function restoreMenuTriggerFocus() {
  const el = menuTrigger;
  menuTrigger = null;
  el?.focus?.();
}

export function getVisibleContextMenuItems() {
  return [...dom.contextMenu.querySelectorAll(".context-menu-item")].filter(
    (el) => !el.classList.contains("d-none"),
  );
}

export function initContextMenuListeners() {
  document.addEventListener("click", (e) => {
    const onItem = e.target.closest("#context-menu .context-menu-item");
    if (onItem) {
      hideContextMenu({ restoreFocus: false });
      queueMicrotask(() => {
        const dialog = document.getElementById("generic-modal");
        if (!dialog?.open) restoreMenuTriggerFocus();
      });
      return;
    }
    if (e.target.closest("#context-menu .context-menu-status")) return;
    hideContextMenu({ restoreFocus: true });
  });

  dom.contextMenu.addEventListener("keydown", (e) => {
    const items = getVisibleContextMenuItems();
    if (items.length === 0) return;
    const currentIndex = items.indexOf(document.activeElement);
    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        items[currentIndex < 0 ? 0 : (currentIndex + 1) % items.length].focus();
        break;
      case "ArrowUp":
        e.preventDefault();
        items[
          currentIndex <= 0 ? items.length - 1 : currentIndex - 1
        ].focus();
        break;
      case "Enter":
      case " ":
        e.preventDefault();
        document.activeElement?.click();
        break;
      case "Escape":
        e.preventDefault();
        hideContextMenu({ restoreFocus: true });
        break;
    }
  });
}
