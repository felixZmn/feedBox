"use strict";

import { dom, itemType, state } from "./state.js";

/** @type {HTMLElement | null} */
let menuTrigger = null;

function showContextMenuGroup(groupClass, x, y, trigger) {
  document.querySelectorAll(".context-menu-item").forEach((element) => {
    element.classList.add("d-none");
  });
  document.querySelectorAll(`.${groupClass}`).forEach((element) => {
    element.classList.remove("d-none");
  });
  openContextMenu(x, y, trigger);
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
  showContextMenuGroup("context-all", x, y, trigger);
}

/**
 * @param {number} x
 * @param {number} y
 * @param {EventTarget | null} [trigger]
 */
export function openContextMenu(x, y, trigger) {
  const el =
    trigger instanceof HTMLElement
      ? trigger
      : document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null;
  menuTrigger = el;

  const menu = dom.contextMenu;
  menu.classList.add("is-open");
  const { innerWidth, innerHeight } = window;
  const menuRect = menu.getBoundingClientRect();
  menu.style.left = `${Math.min(x, innerWidth - menuRect.width)}px`;
  menu.style.top = `${Math.min(y, innerHeight - menuRect.height)}px`;
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
