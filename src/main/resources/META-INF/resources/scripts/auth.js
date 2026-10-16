"use strict";

const FOLDER_TREE_CACHE_KEY = "folder-tree-cache";
const FOLDER_STATE_KEY = "folder-state";

/** Prevents repeated enterLoggedOut work during a burst of 401s. */
let loggedOut = false;
let sessionLossHandler = null;

function clearPrivateUiCache() {
  try {
    localStorage.removeItem(FOLDER_TREE_CACHE_KEY);
    localStorage.removeItem(FOLDER_STATE_KEY);
  } catch {
    /* ignore quota / private-mode errors */
  }
}

/**
 * Register a one-shot UI callback when the session is lost mid-use
 * (show login panel, abort boot, etc.).
 * @param {() => void} handler
 */
export function onSessionLoss(handler) {
  sessionLossHandler = typeof handler === "function" ? handler : null;
}

export function isLoggedOut() {
  return loggedOut;
}

/**
 * Switch the shell to the unauthenticated login panel. Does not navigate.
 */
export function enterLoggedOut() {
  if (loggedOut) {
    return;
  }
  loggedOut = true;
  clearPrivateUiCache();
  document.body.classList.add("logged-out");
  const appShell = document.getElementById("app-shell");
  const loginPanel = document.getElementById("login-panel");
  if (appShell) {
    appShell.hidden = true;
  }
  if (loginPanel) {
    loginPanel.hidden = false;
  }
  void loadAuthProviders();
  try {
    sessionLossHandler?.();
  } catch (err) {
    console.error("[auth] session-loss handler failed", err);
  }
}

/**
 * Reveal the app chrome after a successful session probe.
 */
export function enterLoggedIn() {
  loggedOut = false;
  document.body.classList.remove("logged-out");
  const appShell = document.getElementById("app-shell");
  const loginPanel = document.getElementById("login-panel");
  if (appShell) {
    appShell.hidden = false;
  }
  if (loginPanel) {
    loginPanel.hidden = true;
  }
}

/**
 * Probe for an active OIDC session without following IdP redirects.
 * @returns {Promise<boolean>}
 */
export async function probeSession() {
  const response = await fetch("./api/folder", {
    method: "GET",
    credentials: "same-origin",
    redirect: "manual",
    headers: { Accept: "application/json" },
  });

  if (response.status === 401 || response.status === 499) {
    return false;
  }
  // SW / browser may surface auth redirects as opaque or 3xx with redirect:manual.
  if (response.type === "opaqueredirect" || (response.status >= 300 && response.status < 400)) {
    return false;
  }
  return response.ok;
}

/**
 * Same-origin fetch that sends the OIDC session cookie.
 * On expired/missing session Quarkus returns 401/499 for XHR; show login state.
 * @param {string} url
 * @param {RequestInit} [options]
 * @returns {Promise<Response>}
 */
export async function apiFetch(url, options = {}) {
  if (loggedOut) {
    throw new Error("Session expired");
  }

  const response = await fetch(url, {
    ...options,
    credentials: "same-origin",
    redirect: "manual",
  });

  if (
    response.status === 401 ||
    response.status === 499 ||
    response.type === "opaqueredirect" ||
    (response.status >= 300 && response.status < 400)
  ) {
    enterLoggedOut();
    throw new Error("Session expired");
  }

  return response;
}

/** End the Quarkus OIDC session (httpOnly cookie cleared server-side). */
export function logout() {
  clearPrivateUiCache();
  window.location.assign("/logout");
}

/**
 * Safe same-origin path for a provider button (blocks // and absolute URLs).
 * @param {unknown} href
 * @returns {string}
 */
function safeProviderHref(href) {
  if (typeof href !== "string") {
    return "/start";
  }
  const trimmed = href.trim();
  if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.includes("://")) {
    return "/start";
  }
  return trimmed;
}

/**
 * Render SSO provider buttons from public config (multi-IdP-ready list).
 * Falls back to the static HTML button when the request fails.
 * @returns {Promise<void>}
 */
export async function loadAuthProviders() {
  const list = document.querySelector(".login-providers");
  if (!list) {
    return;
  }

  try {
    const response = await fetch("./api/public/auth-providers", {
      method: "GET",
      credentials: "same-origin",
      redirect: "manual",
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      return;
    }
    const data = await response.json();
    const providers = Array.isArray(data?.providers) ? data.providers : [];
    if (providers.length === 0) {
      return;
    }

    list.replaceChildren();
    for (const provider of providers) {
      const label =
        typeof provider?.label === "string" && provider.label.trim()
          ? provider.label.trim()
          : "Sign in with SSO";
      const href = safeProviderHref(provider?.href);
      const li = document.createElement("li");
      const a = document.createElement("a");
      a.className = "login-provider";
      a.href = href;
      a.textContent = label;
      if (provider?.id === "sso" || providers.length === 1) {
        a.id = "trigger-login";
      }
      li.appendChild(a);
      list.appendChild(li);
    }
  } catch (err) {
    console.warn("[auth] Failed to load auth providers:", err);
  }
}
