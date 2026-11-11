"use strict";

export const SHORTCUTS_STORAGE_KEY = "feedbox-shortcuts";

/**
 * @returns {boolean} whether single-key shortcuts are on (default true)
 */
export function isShortcutsEnabled() {
  try {
    const raw = localStorage.getItem(SHORTCUTS_STORAGE_KEY);
    if (raw == null) return true;
    return raw !== "0" && raw !== "false" && raw !== "off";
  } catch {
    return true;
  }
}

/**
 * @param {boolean} enabled
 */
export function setShortcutsEnabled(enabled) {
  try {
    localStorage.setItem(SHORTCUTS_STORAGE_KEY, enabled ? "true" : "false");
  } catch {
    /* ignore quota / private mode */
  }
}

/**
 * @param {Pick<KeyboardEvent, 'target' | 'ctrlKey' | 'altKey' | 'metaKey' | 'shiftKey' | 'key'>} event
 * @param {{ dialogOpen?: boolean }} [opts]
 * @returns {boolean}
 */
export function shouldIgnoreKey(event, { dialogOpen = false } = {}) {
  if (dialogOpen) return true;

  const target = event.target;
  if (target && typeof target === "object") {
    const el = /** @type {Element & { isContentEditable?: boolean }} */ (target);
    const tag = (el.tagName || "").toLowerCase();
    if (
      tag === "input" ||
      tag === "textarea" ||
      tag === "select" ||
      el.isContentEditable ||
      (typeof el.closest === "function" &&
        el.closest("input, textarea, select, [contenteditable='true']"))
    ) {
      return true;
    }
  }

  if (event.ctrlKey || event.altKey || event.metaKey) return true;
  // Allow Shift+? for help; ignore other shifted letter shortcuts.
  if (event.shiftKey && event.key !== "?") return true;
  return false;
}

/**
 * Map a key event to a shortcut action name, or null.
 * When shortcuts are off, only `help` (`?`) is available so the user can
 * turn them back on.
 * @param {Pick<KeyboardEvent, 'key'>} event
 * @param {{ enabled?: boolean }} [opts]
 * @returns {string|null}
 */
export function resolveShortcutAction(event, { enabled = true } = {}) {
  const key = event.key;
  if (key === "?") return "help";
  if (!enabled) return null;

  switch (key) {
    case "j":
      return "next";
    case "k":
      return "previous";
    case "o":
      return "open";
    case "m":
      return "markRead";
    case "s":
      return "save";
    case "v":
      return "external";
    case "/":
      return "search";
    case "r":
      return "refresh";
    case "u":
    case "Escape":
      return "back";
    default:
      return null;
  }
}

const SHORTCUT_ROWS = [
  ["j / k", "Next / previous article"],
  ["o", "Open article"],
  ["m", "Mark as read"],
  ["s", "Save / unsave"],
  ["v", "Open original"],
  ["/", "Focus search"],
  ["r", "Refresh feeds"],
  ["u / Esc", "Back"],
  ["?", "This help"],
];

function isDialogOpen() {
  const dialog = document.getElementById("generic-modal");
  return Boolean(dialog?.open);
}

function isDesktopLayout() {
  return (
    typeof window.matchMedia === "function" &&
    window.matchMedia("(min-width: 1101px)").matches
  );
}

/** Lazily loaded app modules used by shortcut actions. */
let appApi = null;

async function loadAppApi() {
  if (appApi) return appApi;
  const [
    { state, dom, columns, navigationService },
    { markArticleAsRead, toggleArticleSaved },
    { refreshFeeds },
    { closeReader, loadArticle },
    { isSafeHttpUrl, html },
    { modal },
  ] = await Promise.all([
    import("./state.js"),
    import("./articles.js"),
    import("./feeds.js"),
    import("./reader.js"),
    import("./util.js"),
    import("./modal.js"),
  ]);
  appApi = {
    state,
    dom,
    columns,
    navigationService,
    markArticleAsRead,
    toggleArticleSaved,
    refreshFeeds,
    closeReader,
    loadArticle,
    isSafeHttpUrl,
    modal,
    html,
  };
  return appApi;
}

/**
 * @param {object} api
 * @returns {object | null}
 */
function focusedOrSelectedArticle(api) {
  const focusedRow = document.activeElement?.closest?.(
    ".article[data-article-id]",
  );
  if (focusedRow) {
    const id = Number(focusedRow.dataset.articleId);
    const found = api.state.articles.find((a) => a.id === id);
    if (found) return found;
  }
  if (api.state.selectedArticle) {
    const listed = api.state.articles.find(
      (a) => a.id === api.state.selectedArticle.id,
    );
    return listed ?? api.state.selectedArticle;
  }
  return api.state.articles[0] ?? null;
}

/**
 * @param {object} api
 * @param {object} article
 * @param {{ open?: boolean }} [opts]
 */
function focusArticleRow(api, article, { open = false } = {}) {
  if (!article) return;
  const row = document.querySelector(
    `.article[data-article-id="${article.id}"]`,
  );
  const link = row?.querySelector("a.article-link");
  row?.scrollIntoView({ block: "nearest" });
  link?.focus({ preventScroll: true });
  if (open) void api.loadArticle(article);
}

/**
 * @param {object} api
 * @param {1|-1} direction
 */
function moveSelection(api, direction) {
  if (!api.state.articles.length) return;
  const current = focusedOrSelectedArticle(api);
  let idx = current
    ? api.state.articles.findIndex((a) => a.id === current.id)
    : -1;
  if (idx < 0) idx = direction > 0 ? -1 : 0;
  const nextIdx = Math.max(
    0,
    Math.min(api.state.articles.length - 1, idx + direction),
  );
  const next = api.state.articles[nextIdx];
  if (!next) return;
  focusArticleRow(api, next, { open: isDesktopLayout() });
}

/**
 * Show the shortcut list and the single-key off switch (WCAG 2.1.4).
 */
export async function showShortcutsHelp() {
  const api = await loadAppApi();
  const enabled = isShortcutsEnabled();
  const rows = SHORTCUT_ROWS.map(
    ([keys, label]) =>
      api.html`<tr><th scope="row"><kbd>${keys}</kbd></th><td>${label}</td></tr>`,
  ).join("");
  const checkedAttr = enabled ? " checked" : "";

  const pending = api.modal.show({
    title: "Keyboard shortcuts",
    type: "alert",
    content:
      `<label class="shortcut-toggle">` +
      `<input type="checkbox" id="shortcut-enabled" name="shortcutsEnabled"${checkedAttr} />` +
      ` Single-key shortcuts</label>` +
      `<table class="shortcut-table"><tbody>${rows}</tbody></table>`,
  });

  const checkbox = document.getElementById("shortcut-enabled");
  checkbox?.addEventListener("change", () => {
    setShortcutsEnabled(/** @type {HTMLInputElement} */ (checkbox).checked);
  });

  await pending;
}

/**
 * @param {object} api
 * @param {string} action
 */
function runAction(api, action) {
  switch (action) {
    case "next":
      moveSelection(api, 1);
      break;
    case "previous":
      moveSelection(api, -1);
      break;
    case "open": {
      const article = focusedOrSelectedArticle(api);
      if (article) focusArticleRow(api, article, { open: true });
      break;
    }
    case "markRead": {
      const article = focusedOrSelectedArticle(api);
      if (article && !article.read) void api.markArticleAsRead(article);
      break;
    }
    case "save": {
      const article = focusedOrSelectedArticle(api);
      if (article) void api.toggleArticleSaved(article);
      break;
    }
    case "external": {
      const article =
        focusedOrSelectedArticle(api) ?? api.state.selectedArticle;
      const link = article?.link;
      if (link && api.isSafeHttpUrl(link)) {
        window.open(link, "_blank", "noopener,noreferrer");
      }
      break;
    }
    case "search": {
      const input = api.dom.searchInput;
      if (!input) break;
      api.navigationService.navigateTo(api.columns.ARTICLES);
      input.focus();
      input.select?.();
      break;
    }
    case "refresh":
      void api.refreshFeeds();
      break;
    case "back":
      if (api.state.selectedArticle) {
        api.closeReader();
      } else if (api.navigationService.currentState.column !== api.columns.FEEDS) {
        api.navigationService.navigateTo(api.columns.FEEDS);
      }
      break;
    case "help":
      void showShortcutsHelp();
      break;
    default:
      break;
  }
}

/**
 * Register the global keydown listener. Safe to call once after login.
 */
export function initShortcuts() {
  document.addEventListener("keydown", (event) => {
    if (shouldIgnoreKey(event, { dialogOpen: isDialogOpen() })) return;

    const action = resolveShortcutAction(event, {
      enabled: isShortcutsEnabled(),
    });
    if (!action) return;

    event.preventDefault();
    void loadAppApi().then((api) => runAction(api, action));
  });
}
