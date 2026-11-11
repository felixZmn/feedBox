"use strict";

import { state, dom } from "./state.js";

/**
 * Human-readable name for a sidebar selection.
 * @param {string} [type]
 * @param {{ name?: string } | null} [obj]
 * @returns {string}
 */
export function contextDisplayName(type = "", obj = null) {
  if ((type === "feed" || type === "folder") && obj?.name) {
    return obj.name;
  }
  if (type === "unread") return "Unread";
  if (type === "saved") return "Saved";
  return "All Feeds";
}

function searchClearButton() {
  return document.getElementById("search-clear");
}

function searchResultsRow() {
  return document.getElementById("search-results-row");
}

/**
 * Keep the search placeholder, clear control and results row in sync.
 */
export function updateSearchChrome() {
  const name = contextDisplayName(
    state.lastClickedItem.type,
    state.lastClickedItem.obj,
  );

  const input = dom.searchInput;
  if (input) {
    input.placeholder = `Search ${name}`;
    input.setAttribute("aria-label", `Search ${name}`);
    input.title = `Search articles in ${name}`;
  }

  const hasQuery = Boolean(input?.value.trim());
  const clearBtn = searchClearButton();
  if (clearBtn) clearBtn.hidden = !hasQuery;

  const row = searchResultsRow();
  if (!row) return;

  if (!state.filter.isActive || !state.filter.term) {
    row.hidden = true;
    row.replaceChildren();
    return;
  }

  row.hidden = false;
  row.replaceChildren();

  const text = document.createElement("span");
  text.className = "search-results-text";
  text.textContent = `Results for “${state.filter.term}” in ${name}`;

  const sep = document.createElement("span");
  sep.className = "search-results-sep";
  sep.setAttribute("aria-hidden", "true");
  sep.textContent = " · ";

  const clear = document.createElement("button");
  clear.type = "button";
  clear.className = "search-results-clear";
  clear.textContent = "Clear";

  row.append(text, sep, clear);
}

/**
 * @param {() => void | Promise<void>} onClear
 */
export function initSearchUi(onClear) {
  const input = dom.searchInput;
  input?.addEventListener("input", () => {
    const clearBtn = searchClearButton();
    if (clearBtn) clearBtn.hidden = !input.value.trim();
  });

  searchClearButton()?.addEventListener("click", () => {
    if (input) input.value = "";
    void onClear();
  });

  searchResultsRow()?.addEventListener("click", (event) => {
    const btn = event.target.closest(".search-results-clear");
    if (!btn) return;
    if (input) input.value = "";
    void onClear();
  });

  updateSearchChrome();
}
