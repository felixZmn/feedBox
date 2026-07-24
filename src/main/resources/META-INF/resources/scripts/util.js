"use strict";

// Vendored DOMPurify; version pin: ./vendor/dompurify.version
import DOMPurify from "./vendor/purify.es.mjs";

/**
 * Escapes a string for safe insertion into HTML attribute values or text nodes.
 * @param {*} str
 * @returns {string}
 */
export function escapeHtml(str) {
  return String(str == null ? "" : str)
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

/**
 * Sanitizes untrusted RSS/HTML content for the reader pane.
 * @param {string} html
 * @returns {string}
 */
export function sanitizeHTML(html) {
  if (!html) return "";
  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true },
    FORBID_TAGS: ["style", "form", "input", "button"],
    ALLOW_DATA_ATTR: false,
  });
}

/**
 * Transforms a date string into a relative time format, e.g. "5m", "2h", "3d".
 * If the date is in the future, prefixes with a '-'.
 * @param {*} dateStr
 * @returns {string|null}
 */
export function getRelativeTime(dateStr) {
  if (!dateStr) return null;
  const date = new Date(dateStr);
  if (isNaN(date)) return null;

  const now = new Date();
  const diffMs = date - now;
  const past = diffMs < 0;
  const absMs = Math.abs(diffMs);

  const SECOND = 1000;
  const MINUTE = 60 * SECOND;
  const HOUR = 60 * MINUTE;
  const DAY = 24 * HOUR;
  const MONTH = 30 * DAY;
  const YEAR = 365 * DAY;

  let value;
  let unit;

  if (absMs < MINUTE) {
    value = Math.floor(absMs / SECOND);
    unit = "s";
  } else if (absMs < HOUR) {
    value = Math.floor(absMs / MINUTE);
    unit = "m";
  } else if (absMs < DAY) {
    value = Math.floor(absMs / HOUR);
    unit = "h";
  } else if (absMs < MONTH) {
    value = Math.floor(absMs / DAY);
    unit = "d";
  } else if (absMs < YEAR) {
    value = Math.floor(absMs / MONTH);
    unit = "mo";
  } else {
    value = Math.floor(absMs / YEAR);
    unit = "y";
  }

  return (past ? "" : "-") + value + unit;
}

export function parseDate(dateStr) {
  if (!dateStr) return null;
  const date = new Date(dateStr);
  return new Intl.DateTimeFormat(undefined, {
    weekday: "short",
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(date);
}
