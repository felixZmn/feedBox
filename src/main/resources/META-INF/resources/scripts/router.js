"use strict";

/**
 * @typedef {'all' | 'unread' | 'saved' | 'feed' | 'folder'} RouteContext
 * @typedef {{
 *   valid: boolean,
 *   context: RouteContext,
 *   contextId: number | null,
 *   articleId: number | null,
 * }} AppRoute
 */

const LIST_RE = /^\/(feed|folder)\/(\d+)(?:\/article\/(\d+))?$/;
const ARTICLE_ONLY_RE = /^\/article\/(\d+)$/;
const UNREAD_RE = /^\/unread(?:\/article\/(\d+))?$/;
const SAVED_RE = /^\/saved(?:\/article\/(\d+))?$/;

/**
 * Parse an app pathname into a route. Unknown paths are invalid.
 * @param {string} pathname
 * @returns {AppRoute}
 */
export function parseRoute(pathname) {
  const path = normalizePathname(pathname);

  if (path === "/") {
    return { valid: true, context: "all", contextId: null, articleId: null };
  }

  const unread = path.match(UNREAD_RE);
  if (unread) {
    return {
      valid: true,
      context: "unread",
      contextId: null,
      articleId: unread[1] != null ? Number(unread[1]) : null,
    };
  }

  const saved = path.match(SAVED_RE);
  if (saved) {
    return {
      valid: true,
      context: "saved",
      contextId: null,
      articleId: saved[1] != null ? Number(saved[1]) : null,
    };
  }

  const articleOnly = path.match(ARTICLE_ONLY_RE);
  if (articleOnly) {
    return {
      valid: true,
      context: "all",
      contextId: null,
      articleId: Number(articleOnly[1]),
    };
  }

  const list = path.match(LIST_RE);
  if (list) {
    return {
      valid: true,
      context: /** @type {RouteContext} */ (list[1]),
      contextId: Number(list[2]),
      articleId: list[3] != null ? Number(list[3]) : null,
    };
  }

  return { valid: false, context: "all", contextId: null, articleId: null };
}

/**
 * Build a pathname from route pieces.
 * @param {{
 *   context?: RouteContext,
 *   contextId?: number | null,
 *   articleId?: number | null,
 * }} parts
 * @returns {string}
 */
export function buildPath({
  context = "all",
  contextId = null,
  articleId = null,
} = {}) {
  let path = "/";
  if (context === "feed" && contextId != null) {
    path = `/feed/${contextId}`;
  } else if (context === "folder" && contextId != null) {
    path = `/folder/${contextId}`;
  } else if (context === "unread") {
    path = "/unread";
  } else if (context === "saved") {
    path = "/saved";
  }

  if (articleId != null) {
    path = path === "/" ? `/article/${articleId}` : `${path}/article/${articleId}`;
  }
  return path;
}

/**
 * @param {string} pathname
 * @returns {boolean}
 */
export function isAppPath(pathname) {
  return parseRoute(pathname).valid;
}

/**
 * @param {string} pathname
 * @returns {string}
 */
function normalizePathname(pathname) {
  if (!pathname || pathname === "") return "/";
  let path = pathname.startsWith("/") ? pathname : `/${pathname}`;
  if (path.length > 1 && path.endsWith("/")) {
    path = path.slice(0, -1);
  }
  return path;
}
