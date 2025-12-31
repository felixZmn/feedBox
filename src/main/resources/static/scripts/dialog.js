import { dataService } from "./data.js";
import { modal } from "./modal.js";

const colorOptions = [
  { value: "f-base", label: "Grey" },
  { value: "f-red", label: "Red" },
  { value: "f-orange", label: "Orange" },
  { value: "f-yellow", label: "Yellow" },
  { value: "f-green", label: "Green" },
  { value: "f-blue", label: "Blue" },
  { value: "f-violet", label: "Violet" },
];

export async function showConfirmDialog(headline, message) {
  return await modal.show({
    title: headline,
    content: `<p>${message}</p>`,
    type: "confirm",
  });
}

export async function showAddFolderDialog() {
  const optionsHtml = colorOptions
    .map((opt) => {
      return `<option value="${opt.value}">${opt.label}</option>`;
    })
    .join("");
  const html = `
    <label class="field" for="name">Folder name</label>
    <input
      id="name"
      name="name"
      type="text"
      placeholder="My folder"
      required
    />
    <label class="field" for="folder-color" aria-labelledby="color-label">Color</label>
    <div class="color-row">
      <select id="folder-color" name="color">
        ${optionsHtml}
      </select>
    </div>
  `;
  return await modal.show({
    title: "Add Folder",
    content: html,
    type: "confirm",
  });
}

export async function showEditFolderDialog(folder) {
  const optionsHtml = colorOptions
    .map((opt) => {
      const isSelected = opt.value === folder.color ? "selected" : "";
      return `<option value="${opt.value}" ${isSelected}>${opt.label}</option>`;
    })
    .join("");

  const html = `
    <label class="field" for="folder-name">Folder name</label>
    <input
      id="folder-name"
      name="name" 
      type="text"
      value="${folder.name}"
      required
    />
    
    <label class="field" for="folder-color" aria-labelledby="color-label">Color</label>
    <div class="color-row">
      <select id="folder-color" name="color">
        ${optionsHtml}
      </select>
    </div>
  `;
  return await modal.show({
    title: "Edit Folder",
    content: html,
    type: "confirm",
  });
}

export async function showAddFeedDialog(folders) {
  const html = `
    <label class="field" for="feed-url">Feed URL</label>
    <input
      id="feed-url"
      name="feedUrl"
      type="url"
      placeholder="https://example.com/feed.xml"
      required
    />
    <label class="field" for="feed-folder">Folder</label>
    <select id="feed-folder" name="folderId">
      ${folders.map((folder) => {
        return `<option value="${folder.id}">${folder.name}</option>`;
      })}
    </select>
  `;

  return await modal.show({
    title: "Add Feed",
    content: html,
    type: "confirm",
    onValidate: async (data, bodyEl) => {
      if (!data.feedUrl || data.feedUrl.trim() === "") {
        alert("Please enter a URL.");
        return;
      }
      const response = await dataService.checkFeed(data.feedUrl);
      if (!Array.isArray(response) || response.length === 0) {
        alert("No feeds found.");
        return;
      }

      if (response.length === 1) {
        return true;
      }

      // Multiple feeds found, show selector
      const feedOptions = response
        .map(
          (feed, index) =>
            `<option value="${feed.feedUrl}">${feed.name} (${feed.feedUrl})</option>`
        )
        .join("");

      bodyEl.innerHTML = `
        <label class="field" for="feed-selector">Multiple feeds found. Please select one:</label>
        <select id="feed-selector" name="feedUrl">
          ${feedOptions}
        </select>
        <input type="hidden" name="folderId" value="${data.folderId}" />
      `;

      return false; // Keep dialog open
    },
  });
}

export function showEditFeedDialog(folders, feed) {
  const html = `
    <label class="field" for="feed-url">Feed URL</label>
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
      ${folders.map((folder) => {
        const isSelected = folder.id === feed.folderId ? "selected" : "";
        return `<option value="${folder.id}" ${isSelected}>${folder.name}</option>`;
      })}
    </select>
  `;

  return modal.show({
    title: "Edit Feed",
    content: html,
    type: "confirm",
  });
}
