"use strict";

const MAX_TOASTS = 4;
const EXIT_MS = 180;

/** @type {Map<string, {el: HTMLElement, timer: *}>} */
const live = new Map();
let anonymousCounter = 0;

function prefersReducedMotion() {
  return (
    typeof window !== "undefined" &&
    typeof window.matchMedia === "function" &&
    window.matchMedia("(prefers-reduced-motion: reduce)").matches
  );
}

function dismiss(id) {
  const entry = live.get(id);
  if (!entry) return;
  live.delete(id);
  clearTimeout(entry.timer);
  if (prefersReducedMotion()) {
    entry.el.remove();
    return;
  }
  entry.el.classList.add("is-leaving");
  setTimeout(() => entry.el.remove(), EXIT_MS);
}

/**
 * Show a short, non-blocking status message.
 * Calling again with the same `id` updates that toast in place (progress).
 *
 * @param {string} message Plain text; never interpreted as HTML.
 * @param {Object} [options]
 * @param {string} [options.action] Label of an optional action button.
 * @param {Function} [options.onAction] Called when the action is pressed; the toast then dismisses.
 * @param {number} [options.timeoutMs=5000] Auto-dismiss delay; 0 keeps the toast until replaced or the action is used.
 * @param {string} [options.id] Stable key for in-place updates.
 * @returns {{ id: string, dismiss: () => void }}
 */
export function toast(
  message,
  { action, onAction, timeoutMs = 5000, id } = {},
) {
  const region = document.getElementById("toast-region");
  const key = id ?? `toast-${++anonymousCounter}`;
  const handle = { id: key, dismiss: () => dismiss(key) };
  if (!region) return handle;

  let entry = live.get(key);
  if (entry) {
    clearTimeout(entry.timer);
    entry.el.classList.remove("is-leaving");
    entry.el.replaceChildren();
  } else {
    const el = document.createElement("div");
    el.className = "toast";
    if (!prefersReducedMotion()) el.classList.add("is-entering");
    entry = { el, timer: null };
    live.set(key, entry);
    region.append(el);

    while (live.size > MAX_TOASTS) {
      dismiss(live.keys().next().value);
    }
  }

  const text = document.createElement("span");
  text.className = "toast-message";
  text.textContent = message;
  entry.el.append(text);

  if (action) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "toast-action";
    button.textContent = action;
    button.addEventListener("click", () => {
      try {
        onAction?.();
      } finally {
        dismiss(key);
      }
    });
    entry.el.append(button);
  }

  if (timeoutMs > 0 && Number.isFinite(timeoutMs)) {
    entry.timer = setTimeout(() => dismiss(key), timeoutMs);
  }
  return handle;
}
