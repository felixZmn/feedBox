"use strict";

import { dataService } from "./data.js";
import { modal } from "./modal.js";
import { html } from "./util.js";
import {
  normalizeFeedUrl,
  feedErrorMessage,
  INVALID_URL_MESSAGE,
} from "./feed-form.js";

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

/**
 * @param {number} count
 * @param {string} singular
 * @param {string} plural
 */
function countLabel(count, singular, plural) {
  return `${count} ${count === 1 ? singular : plural}`;
}

/**
 * Confirm deleting a feed; warns when saved articles would be removed.
 * @param {{ id: number, name: string }} feed
 * @returns {Promise<boolean>}
 */
export async function showDeleteFeedDialog(feed) {
  let savedCount = 0;
  try {
    const result = await dataService.loadSavedCount({ feed: feed.id });
    savedCount = Number(result?.count) || 0;
  } catch (error) {
    console.warn("[app] Could not load saved count for feed delete:", error);
  }

  const parts = [
    `Are you sure you want to delete the feed "${feed.name}"?`,
  ];
  if (savedCount > 0) {
    parts.push(
      `This permanently removes ${countLabel(savedCount, "saved article", "saved articles")}.`,
    );
  }
  const body = parts.map((text) => {
    const p = document.createElement("p");
    p.textContent = text;
    return p.outerHTML;
  }).join("");

  const result = await modal.show({
    title: "Delete Feed",
    content: `<div class="dialog-stack">${body}</div>`,
    type: "confirm",
    confirmLabel: "Delete Feed",
    destructive: true,
  });
  return !!result;
}

/**
 * Confirm deleting a folder; offers keep vs delete feeds, warns about saved.
 * @param {{ id: number, name: string, feeds?: object[] }} folder
 * @returns {Promise<{ feeds: "keep"|"delete" } | null>}
 */
export async function showDeleteFolderDialog(folder) {
  const feedCount = (folder.feeds ?? []).length;
  let savedCount = 0;
  try {
    const result = await dataService.loadSavedCount({ folder: folder.id });
    savedCount = Number(result?.count) || 0;
  } catch (error) {
    console.warn("[app] Could not load saved count for folder delete:", error);
  }

  const intro = document.createElement("p");
  intro.textContent = `Are you sure you want to delete the folder "${folder.name}"?`;

  let warningHtml = "";
  if (savedCount > 0) {
    const warn = document.createElement("p");
    warn.textContent =
      feedCount > 0
        ? `This folder has ${countLabel(savedCount, "saved article", "saved articles")}. Deleting its feeds removes them permanently.`
        : `This folder has ${countLabel(savedCount, "saved article", "saved articles")}.`;
    warningHtml = warn.outerHTML;
  }

  let choiceHtml = "";
  if (feedCount > 0) {
    choiceHtml = html`<fieldset class="dialog-choice">
      <legend>What should happen to the feeds?</legend>
      <label class="dialog-choice-option">
        <input type="radio" name="feeds" value="keep" checked />
        Move ${countLabel(feedCount, "feed", "feeds")} to No folder
      </label>
      <label class="dialog-choice-option">
        <input type="radio" name="feeds" value="delete" />
        Delete folder and ${countLabel(feedCount, "feed", "feeds")}
      </label>
    </fieldset>`;
  }

  const result = await modal.show({
    title: "Delete Folder",
    content: `<div class="dialog-stack">${intro.outerHTML}${warningHtml}${choiceHtml}</div>`,
    type: "confirm",
    confirmLabel: "Delete Folder",
    destructive: true,
  });
  if (!result) return null;
  const feeds = result.feeds === "delete" ? "delete" : "keep";
  return { feeds };
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

/**
 * Replace the Add Feed form with a chooser when a page links several feeds.
 * @param {HTMLElement} bodyEl
 * @param {Array<{ name?: string, feedUrl: string }>} feeds
 * @param {string} folderId
 */
function renderFeedChooser(bodyEl, feeds, folderId) {
  const options = feeds
    .map((feed) => {
      const label =
        feed.name && feed.name !== feed.feedUrl
          ? `${feed.name} — ${feed.feedUrl}`
          : feed.feedUrl;
      return html`<option value="${feed.feedUrl}">${label}</option>`;
    })
    .join("");
  bodyEl.innerHTML =
    html`<p class="dialog-note">This site offers ${feeds.length} feeds. Choose one to add.</p>
    <label class="field" for="feed-choice">Feed</label>
    <select id="feed-choice" name="chosenFeedUrl">
      ` +
    options +
    html`</select>
    <input type="hidden" name="folderId" value="${folderId ?? ""}" />`;
  bodyEl.querySelector("#feed-choice")?.focus();
}

/**
 * Ask for a site or feed address, discover the feed, and create it.
 * Errors (not a feed, unreachable, already followed) stay in the dialog.
 * @param {Folder[]} folders
 * @returns {Promise<{ id: number, feedUrl: string, folderId: number|null }|null>}
 *          the created feed, or null when cancelled
 */
export async function showAddFeedDialog(folders) {
  const folderOptions = folders
    .map((folder) => html`<option value="${folder.id}">${folder.name}</option>`)
    .join("");
  const content =
    html`<label class="field" for="feed-url">Website or feed</label>
    <input
      id="feed-url"
      name="feedUrl"
      type="text"
      inputmode="url"
      autocomplete="url"
      autocapitalize="off"
      spellcheck="false"
      maxlength="2048"
      placeholder="example.com"
      required
      autofocus
    />
    <label class="field" for="feed-folder">Folder</label>
    <select id="feed-folder" name="folderId">
      <option value="">No folder</option>
      ` +
    folderOptions +
    html`</select>`;

  let created = null;
  const confirmed = await modal.show({
    title: "Add Feed",
    content,
    type: "confirm",
    confirmLabel: "Add Feed",
    onValidate: async (data, bodyEl) => {
      const folderId = data.folderId ? Number(data.folderId) : null;
      let feedUrl = data.chosenFeedUrl;

      if (!feedUrl) {
        const requested = normalizeFeedUrl(data.feedUrl);
        if (!requested) {
          showDialogError(bodyEl, INVALID_URL_MESSAGE);
          return false;
        }
        modal.setBusy("Checking…");
        let found;
        try {
          found = await dataService.checkFeed(requested);
        } catch (error) {
          showDialogError(bodyEl, feedErrorMessage(error));
          return false;
        }
        if (!Array.isArray(found) || found.length === 0) {
          showDialogError(bodyEl, feedErrorMessage({ code: "NOT_A_FEED" }));
          return false;
        }
        if (found.length > 1) {
          renderFeedChooser(bodyEl, found, data.folderId);
          return false;
        }
        feedUrl = found[0].feedUrl;
      }

      modal.setBusy("Adding…");
      try {
        const id = await dataService.createFeed({ feedUrl, folderId });
        created = { id: Number(id), feedUrl, folderId };
        return true;
      } catch (error) {
        showDialogError(bodyEl, feedErrorMessage(error));
        return false;
      }
    },
  });
  return confirmed ? created : null;
}

/**
 * Edit name, address, and folder; saves before closing so a conflicting
 * address is reported in the dialog.
 * @param {Folder[]} folders
 * @param {Feed} feed
 * @returns {Promise<{ id: number, name: string, feedUrl: string, folderId: number|null }|null>}
 */
export async function showEditFeedDialog(folders, feed) {
  const folderOptions = folders
    .map((folder) => {
      const isSelected = folder.id === feed.folderId ? "selected" : "";
      return html`<option value="${folder.id}" ${isSelected}>${folder.name}</option>`;
    })
    .join("");
  const content =
    html`<label class="field" for="feed-name">Name</label>
    <input
      id="feed-name"
      name="name"
      type="text"
      maxlength="255"
      value="${feed.name}"
      required
      autofocus
    />
    <label class="field" for="feed-url">Feed address</label>
    <input
      id="feed-url"
      name="feedUrl"
      type="text"
      inputmode="url"
      autocomplete="url"
      autocapitalize="off"
      spellcheck="false"
      maxlength="2048"
      value="${feed.feedUrl}"
      required
    />
    <label class="field" for="feed-folder">Folder</label>
    <select id="feed-folder" name="folderId">
      <option value="">No folder</option>
      ` +
    folderOptions +
    html`</select>`;

  let saved = null;
  const confirmed = await modal.show({
    title: "Edit Feed",
    content,
    type: "confirm",
    confirmLabel: "Save",
    onValidate: async (data, bodyEl) => {
      const name = (data.name ?? "").trim();
      if (!name) {
        showDialogError(bodyEl, "Please enter a name.");
        return false;
      }
      const typedUrl = (data.feedUrl ?? "").trim();
      const feedUrl =
        typedUrl === feed.feedUrl ? feed.feedUrl : normalizeFeedUrl(typedUrl);
      if (!feedUrl) {
        showDialogError(bodyEl, INVALID_URL_MESSAGE);
        return false;
      }
      const update = {
        id: feed.id,
        name,
        feedUrl,
        folderId: data.folderId ? Number(data.folderId) : null,
      };
      modal.setBusy("Saving…");
      try {
        await dataService.updateFeed(update);
        saved = update;
        return true;
      } catch (error) {
        showDialogError(
          bodyEl,
          error?.code === "DUPLICATE"
            ? "Another feed already uses this address."
            : feedErrorMessage(error),
        );
        return false;
      }
    },
  });
  return confirmed ? saved : null;
}
