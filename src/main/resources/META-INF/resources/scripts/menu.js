"use strict";

import { dom, itemType, state } from "./state.js";

function showContextMenuGroup(groupClass, x, y) {
  document.querySelectorAll(".context-menu-item").forEach((element) => {
    element.classList.add("d-none");
  });
  document.querySelectorAll(`.${groupClass}`).forEach((element) => {
    element.classList.remove("d-none");
  });
  openContextMenu(x, y);
}

export function openAddContextMenu(x, y) {
  showContextMenuGroup("context-add", x, y);
}

export function feedContextMenu(x, y, feed) {
  state.lastClickedItem.type = itemType.FEED;
  state.lastClickedItem.obj = feed;
  showContextMenuGroup("context-feed", x, y);
}

export function folderContextMenu(x, y, folder) {
  state.lastClickedItem.type = itemType.FOLDER;
  state.lastClickedItem.obj = folder;
  showContextMenuGroup("context-folder", x, y);
}

export function allFeedsContextMenu(x, y) {
  state.lastClickedItem.type = itemType.ALL;
  state.lastClickedItem.obj = null;
  showContextMenuGroup("context-all", x, y);
}

export function openContextMenu(x, y) {
  const menu = dom.contextMenu;
  menu.classList.add("is-open");
  const { innerWidth, innerHeight } = window;
  const menuRect = menu.getBoundingClientRect();
  menu.style.left = `${Math.min(x, innerWidth - menuRect.width)}px`;
  menu.style.top = `${Math.min(y, innerHeight - menuRect.height)}px`;
  getVisibleContextMenuItems()[0]?.focus();
}

export function hideContextMenu() {
  dom.contextMenu.classList.remove("is-open");
}

export function getVisibleContextMenuItems() {
  return [...dom.contextMenu.querySelectorAll(".context-menu-item")].filter(
    (el) => !el.classList.contains("d-none"),
  );
}

export function initContextMenuListeners() {
  document.addEventListener("click", () => {
    hideContextMenu();
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
        hideContextMenu();
        break;
    }
  });
}
