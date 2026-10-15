"use strict";

const FOLDER_TREE_CACHE_KEY = "folder-tree-cache";
const FOLDER_STATE_KEY = "folder-state";

function clearPrivateUiCache() {
  try {
    localStorage.removeItem(FOLDER_TREE_CACHE_KEY);
    localStorage.removeItem(FOLDER_STATE_KEY);
  } catch {
    /* ignore quota / private-mode errors */
  }
}

/**
 * Same-origin fetch that sends the OIDC session cookie.
 * On expired/missing session Quarkus returns 401/499 for XHR; reload to restart login.
 * @param {string} url
 * @param {RequestInit} [options]
 * @returns {Promise<Response>}
 */
export async function apiFetch(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    credentials: "same-origin",
  });

  if (response.status === 401 || response.status === 499) {
    clearPrivateUiCache();
    window.location.assign("/");
    throw new Error("Session expired");
  }

  return response;
}

/** End the Quarkus OIDC session (httpOnly cookie cleared server-side). */
export function logout() {
  clearPrivateUiCache();
  window.location.assign("/logout");
}
