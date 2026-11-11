"use strict";

import { toast } from "./toast.js";
import { isAppPath } from "./router.js";

const RETURN_TO_KEY = "returnTo";
const OFFLINE_HEADER = "X-FeedBox-Offline";
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

const LOGIN_REASONS = {
  expired: "Your session ended. Sign in to pick up where you left off.",
  session: "Your session ended. Sign in to pick up where you left off.",
  "signed-out-elsewhere":
    "You signed out in another tab. Sign in again to continue.",
};

export function isLoggedOut() {
  return loggedOut;
}

/**
 * Switch the shell to the unauthenticated login panel. Does not navigate.
 * @param {"expired" | "session" | "signed-out-elsewhere"} [reason]
 *   Why the session ended; omit on a first visit.
 */
export function enterLoggedOut(reason) {
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
  setLoginReason(reason);
  void loadAuthProviders();
  try {
    sessionLossHandler?.();
  } catch (err) {
    console.error("[auth] session-loss handler failed", err);
  }
}

/** @param {string | undefined} reason */
function setLoginReason(reason) {
  const el = document.getElementById("login-reason");
  if (!el) return;
  const text = (reason && LOGIN_REASONS[reason]) || "";
  el.textContent = text;
  el.hidden = text === "";
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
  setLoginReason(undefined);
}

/**
 * @param {Response | null | undefined} response
 * @returns {boolean} true for 401/499/redirects (the server says "sign in").
 */
function isAuthFailure(response) {
  return (
    response.status === 401 ||
    response.status === 499 ||
    // SW / browser may surface auth redirects as opaque or 3xx with redirect:manual.
    response.type === "opaqueredirect" ||
    (response.status >= 300 && response.status < 400)
  );
}

/** @param {Response} response */
function isOfflineResponse(response) {
  return Boolean(response.headers?.get?.(OFFLINE_HEADER));
}

function notifyOffline() {
  try {
    toast("Can't reach FeedBox. Check your connection.", {
      id: "offline",
      timeoutMs: 4000,
    });
  } catch {
    /* no DOM (tests) */
  }
}

/**
 * Probe for an active OIDC session without following IdP redirects.
 * Only an explicit auth answer counts as signed out; a network failure or a
 * server error says nothing about the session and must not wipe the UI.
 * @returns {Promise<"signed-in" | "signed-out" | "unavailable">}
 */
export async function probeSession() {
  let response;
  try {
    response = await fetch("./api/folder", {
      method: "GET",
      credentials: "same-origin",
      redirect: "manual",
      headers: { Accept: "application/json" },
    });
  } catch {
    return "unavailable";
  }

  if (isOfflineResponse(response)) {
    return "unavailable";
  }
  if (isAuthFailure(response)) {
    return "signed-out";
  }
  return response.ok ? "signed-in" : "unavailable";
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

  let response;
  try {
    response = await fetch(url, {
      ...options,
      credentials: "same-origin",
      redirect: "manual",
    });
  } catch (err) {
    if (err instanceof TypeError) {
      notifyOffline();
    }
    throw err;
  }

  if (isOfflineResponse(response)) {
    notifyOffline();
    return response;
  }

  if (isAuthFailure(response)) {
    enterLoggedOut("expired");
    throw new Error("Session expired");
  }

  return response;
}

/**
 * Remember where the user was so sign-in can bring them back to it.
 * @param {string} pathname
 * @param {Storage} [storage]
 */
export function rememberReturnTo(pathname, storage = globalThis.sessionStorage) {
  try {
    if (isAppPath(pathname)) {
      storage?.setItem(RETURN_TO_KEY, pathname);
    }
  } catch {
    /* storage unavailable */
  }
}

/**
 * Read and clear the remembered deep link. Only returns a path when the page
 * landed on the root (post-login) and the stored value is an app route.
 * @param {string} currentPathname
 * @param {Storage} [storage]
 * @returns {string | null}
 */
export function takeReturnTo(currentPathname, storage = globalThis.sessionStorage) {
  try {
    const stored = storage?.getItem(RETURN_TO_KEY);
    if (stored == null) return null;
    storage.removeItem(RETURN_TO_KEY);
    if (currentPathname === "/" && stored !== "/" && isAppPath(stored)) {
      return stored;
    }
  } catch {
    /* storage unavailable */
  }
  return null;
}

/** Record the current deep link when the user heads to the identity provider. */
export function initReturnToCapture() {
  document.addEventListener("click", (event) => {
    const link = event.target?.closest?.(".login-providers a");
    if (link) {
      rememberReturnTo(window.location.pathname);
    }
  });
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
