"use strict";

/**
 * Turn what a person types into a fetchable http(s) URL.
 * Adds `https://` when no scheme is given and unwraps `feed:` subscribe links.
 * @param {string} input
 * @returns {string|null} absolute URL, or null when it cannot be one
 */
export function normalizeFeedUrl(input) {
  let value = String(input ?? "").trim();
  if (!value) return null;
  if (/^feed:\/\//i.test(value)) {
    value = "https://" + value.slice("feed://".length);
  } else if (/^feed:https?:\/\//i.test(value)) {
    value = value.slice("feed:".length);
  }
  if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(value)) {
    value = "https://" + value;
  }
  let url;
  try {
    url = new URL(value);
  } catch {
    return null;
  }
  if (url.protocol !== "http:" && url.protocol !== "https:") return null;
  if (!url.hostname) return null;
  return url.href;
}

const FEED_ERROR_COPY = {
  DUPLICATE: "You already follow this feed.",
  INVALID_URL:
    "That doesn’t look like a web address. Try something like example.com.",
  NOT_A_FEED: "No RSS or Atom feed was found at this address.",
  BLOCKED: "FeedBox isn’t allowed to load this address.",
  TIMEOUT: "The site took too long to respond. Try again later.",
  TOO_LARGE: "This feed is too large to load.",
};

/**
 * User-facing sentence for a failed feed check, create, or update.
 * @param {{ code?: string|null, detail?: string|null, status?: number }} error
 * @returns {string}
 */
export function feedErrorMessage(error) {
  const code = error?.code;
  const detail = error?.detail;
  if (code === "UNREACHABLE") {
    return detail
      ? `Couldn’t reach this site (${detail}).`
      : "Couldn’t reach this site.";
  }
  if (code === "HTTP_STATUS") {
    return detail
      ? `The site answered with an error (${detail}).`
      : "The site answered with an error.";
  }
  if (code && FEED_ERROR_COPY[code]) return FEED_ERROR_COPY[code];
  if (error?.status === 409) return FEED_ERROR_COPY.DUPLICATE;
  if (error?.status == null) {
    return "Couldn’t reach FeedBox. Check your connection and try again.";
  }
  return detail || "Something went wrong. Please try again.";
}

export const INVALID_URL_MESSAGE = FEED_ERROR_COPY.INVALID_URL;
