"use strict";

import { dataService } from "./data.js";
import { modal } from "./modal.js";
import { html } from "./util.js";

const colorOptions = [
  { value: "f-base", label: "Grey" },
  { value: "f-red", label: "Red" },
  { value: "f-orange", label: "Orange" },
  { value: "f-yellow", label: "Yellow" },
  { value: "f-green", label: "Green" },
  { value: "f-blue", label: "Blue" },
  { value: "f-violet", label: "Violet" },
];

/**
 * Renders an inline error message inside an open modal body.
 * Avoids calling modal.show() on an already-open <dialog>, which throws.
 * @param {HTMLElement} bodyEl - The modal body element
 * @param {string} message - The error message to display
 */
function showDialogError(bodyEl, message) {
  let errorEl = bodyEl.querySelector(".dialog-error");
  if (!errorEl) {
    errorEl = document.createElement("div");
    errorEl.className = "dialog-error";
    errorEl.setAttribute("role", "alert");
    bodyEl.prepend(errorEl);
  }
  errorEl.textContent = message;
}

function colorOptionsHtml(selectedValue) {
  return colorOptions
    .map((opt) => {
      const isSelected = opt.value === selectedValue ? "selected" : "";
      return html`<option value="${opt.value}" ${isSelected}>${opt.label}</option>`;
    })
    .join("");
}

export async function showConfirmDialog(headline, message) {
  // Callers pass plain text; wrap safely via textContent.
  const p = document.createElement("p");
  p.textContent = message;
  return modal.show({
    title: headline,
    content: p.outerHTML,
    type: "confirm",
  });
}

export async function showAddFolderDialog() {
  const optionsHtml = colorOptionsHtml();
  const content =
    html`<label class="field" for="name">Folder name</label>
    <input
      id="name"
      name="name"
      type="text"
      placeholder="My folder"
      required
    />
    <label class="field" for="folder-color">Color</label>
    <div class="color-row">
      <select id="folder-color" name="color">
        ` +
    optionsHtml +
    html`</select>
    </div>`;
  return modal.show({
    title: "Add Folder",
    content,
    type: "confirm",
  });
}

export async function showEditFolderDialog(folder) {
  const optionsHtml = colorOptionsHtml(folder.color);
  const content =
    html`<label class="field" for="folder-name">Folder name</label>
    <input
      id="folder-name"
      name="name"
      type="text"
      value="${folder.name}"
      required
    />
    <label class="field" for="folder-color">Color</label>
    <div class="color-row">
      <select id="folder-color" name="color">
        ` +
    optionsHtml +
    html`</select>
    </div>`;
  return modal.show({
    title: "Edit Folder",
    content,
    type: "confirm",
  });
}

export async function showAddFeedDialog(folders) {
  const folderOptions = folders
    .map((folder) => html`<option value="${folder.id}">${folder.name}</option>`)
    .join("");
  const content =
    html`<label class="field" for="feed-url">Feed URL</label>
    <input
      id="feed-url"
      name="feedUrl"
      type="url"
      placeholder="https://example.com/feed.xml"
      required
    />
    <label class="field" for="feed-folder">Folder</label>
    <select id="feed-folder" name="folderId">
      <option value="">No folder</option>
      ` +
    folderOptions +
    html`</select>`;

  return modal.show({
    title: "Add Feed",
    content,
    type: "confirm",
    onValidate: async (data, bodyEl) => {
      if (!data.feedUrl || data.feedUrl.trim() === "") {
        showDialogError(bodyEl, "Please enter a URL.");
        return false;
      }
      const response = await dataService.checkFeed(data.feedUrl);
      if (!Array.isArray(response) || response.length === 0) {
        showDialogError(bodyEl, "No feeds found.");
        return false;
      }

      if (response.length === 1) {
        const feedUrlInput = bodyEl.querySelector("#feed-url");
        if (feedUrlInput) {
          feedUrlInput.value = response[0].feedUrl;
        }
        return true;
      }

      const feedOptions = response
        .map(
          (feed) =>
            html`<option value="${feed.feedUrl}">${feed.name} (${feed.feedUrl})</option>`,
        )
        .join("");

      bodyEl.innerHTML =
        html`<label class="field" for="feed-selector">Multiple feeds found. Please select one:</label>
        <select id="feed-selector" name="feedUrl">
          ` +
        feedOptions +
        html`</select>
        <input type="hidden" name="folderId" value="${data.folderId}" />`;

      return false;
    },
  });
}

export function showEditFeedDialog(folders, feed) {
  const folderOptions = folders
    .map((folder) => {
      const isSelected = folder.id === feed.folderId ? "selected" : "";
      return html`<option value="${folder.id}" ${isSelected}>${folder.name}</option>`;
    })
    .join("");
  const content =
    html`<label class="field" for="feed-url">Feed URL</label>
    <input
      id="feed-url"
      name="feedUrl"
      type="url"
      value="${feed.feedUrl}"
      required
    />
    <label class="field" for="feed-folder">Folder</label>
    <select id="feed-folder" name="folderId">
      <option value="">No folder</option>
      ` +
    folderOptions +
    html`</select>`;

  return modal.show({
    title: "Edit Feed",
    content,
    type: "confirm",
  });
}
