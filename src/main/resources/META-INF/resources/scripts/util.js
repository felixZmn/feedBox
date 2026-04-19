"use strict";

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
 * Sanitizes an HTML string by removing scripts, event handlers, and
 * dangerous elements before inserting into the DOM.
 * @param {string} html
 * @returns {string} sanitized HTML string
 */
export function sanitizeHTML(html) {
  if (!html) return "";
  const tmp = document.createElement("div");
  tmp.innerHTML = html;
  tmp
    .querySelectorAll("script, object, embed, form, iframe, meta, link")
    .forEach((el) => el.remove());
  tmp.querySelectorAll("*").forEach((el) => {
    [...el.attributes].forEach((attr) => {
      if (
        attr.name.startsWith("on") ||
        attr.value.toLowerCase().startsWith("javascript:")
      ) {
        el.removeAttribute(attr.name);
      }
    });
  });
  return tmp.innerHTML;
}

/**
 * Transforms a date string into a relative time format, e.g. "5m", "2h", "3d".
 * If the date is in the future, prefixes with a '-'.
 * @param {*} dateStr the date string to transform
 * @returns {string|null} the relative time string or null if invalid date
 */
export function getRelativeTime(dateStr) {
  if (!dateStr) return null;
  const date = new Date(dateStr);
  if (isNaN(date)) return null;

  const now = new Date();
  let diffMs = date - now;
  const past = diffMs < 0;

  const absMs = Math.abs(diffMs);

  const SECOND = 1000;
  const MINUTE = 60 * SECOND;
  const HOUR = 60 * MINUTE;
  const DAY = 24 * HOUR;
  const MONTH = 30 * DAY;
  const YEAR = 365 * DAY;

  let value, unit;

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

  // If in the future, prefix with '-'
  return (past ? "" : "-") + value + unit;
}

export function parseDate(dateStr) {
  if (!dateStr) return null;
  const date = new Date(dateStr);

  const options = {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "numeric",
    hour12: false,
    minute: "2-digit",
    timeZone: "UTC",
    timeZoneName: "short",
  };
  const formatter = new Intl.DateTimeFormat(undefined, options);
  return formatter.format(date);
}
