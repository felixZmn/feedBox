"use strict";

import { dom, itemType, state } from "./state.js";
import { feedHealth } from "./feed-health.js";
import { placeContextMenu } from "./menu-placement.js";

export { placeContextMenu } from "./menu-placement.js";

/** @type {HTMLElement | null} */
let menuTrigger = null;

/**
 * @param {string} groupClass
 * @param {EventTarget | null} [trigger]
 * @param {{ align?: "start" | "end" }} [options]
 */
function showContextMenuGroup(groupClass, trigger, options) {
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
  openContextMenu(trigger, options);
}

/**
 * @param {EventTarget | null} [trigger]
 */
export function openAddContextMenu(trigger) {
  showContextMenuGroup("context-add", trigger, { align: "start" });
}

/**
 * @param {object} feed
 * @param {EventTarget | null} [trigger]
 */
export function feedContextMenu(feed, trigger) {
  state.lastClickedItem.type = itemType.FEED;
  state.lastClickedItem.obj = feed;
  if (dom.feedHealth) {
    const { text, isError } = feedHealth(feed);
    dom.feedHealth.textContent = text;
    dom.feedHealth.classList.toggle("is-error", isError);
  }
  setMarkReadMenuLabel(dom.button.markFeedRead, Number(feed?.unreadCount) || 0);
  showContextMenuGroup("context-feed", trigger, { align: "start" });
}

/**
 * @param {object} folder
 * @param {EventTarget | null} [trigger]
 */
export function folderContextMenu(folder, trigger) {
  state.lastClickedItem.type = itemType.FOLDER;
  state.lastClickedItem.obj = folder;
  const live = state.folders.find((f) => f.id === folder?.id) ?? folder;
  const count = (live?.feeds ?? []).reduce(
    (sum, feed) => sum + (Number(feed.unreadCount) || 0),
    0,
  );
  setMarkReadMenuLabel(dom.button.markFolderRead, count);
  showContextMenuGroup("context-folder", trigger, { align: "start" });
}

/**
 * @param {EventTarget | null} [trigger]
 */
export function allFeedsContextMenu(trigger) {
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
  showContextMenuGroup("context-all", trigger, { align: "start" });
}

/**
 * Profile / chrome overflow: import, export, shortcuts, sign out.
 * @param {EventTarget | null} [trigger]
 * @param {{ align?: "start" | "end" }} [options]
 */
export function openChromeContextMenu(trigger, options = {}) {
  showContextMenuGroup("context-chrome", trigger, {
    align: options.align === "start" ? "start" : "end",
  });
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
 * @param {EventTarget | null} [trigger]
 * @param {{ align?: "start" | "end" }} [options]
 */
export function openContextMenu(trigger, options = {}) {
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
  const menuRect = menu.getBoundingClientRect();
  const triggerRect = el?.getBoundingClientRect() ?? {
    left: 8,
    right: 8,
    top: 8,
    bottom: 8,
  };
  const { left, top } = placeContextMenu({
    triggerRect,
    menuWidth: menuRect.width,
    menuHeight: menuRect.height,
    viewport: { width: window.innerWidth, height: window.innerHeight },
    align,
    gap: 4,
    edge: 8,
  });
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
