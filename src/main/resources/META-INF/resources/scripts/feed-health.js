"use strict";

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

const relativeFormat = new Intl.RelativeTimeFormat("en", { numeric: "auto" });

/**
 * "just now", "5 minutes ago", "3 hours ago", "yesterday", "12 days ago".
 * @param {string|number|Date} value
 * @param {number} [now]
 * @returns {string|null} null when the value is not a date
 */
export function relativeTimeText(value, now = Date.now()) {
  const time = new Date(value).getTime();
  if (!Number.isFinite(time)) return null;
  const elapsed = Math.max(0, now - time);
  if (elapsed < MINUTE) return "just now";
  if (elapsed < HOUR) {
    return relativeFormat.format(-Math.floor(elapsed / MINUTE), "minute");
  }
  if (elapsed < DAY) {
    return relativeFormat.format(-Math.floor(elapsed / HOUR), "hour");
  }
  return relativeFormat.format(-Math.floor(elapsed / DAY), "day");
}

/**
 * One-line refresh status for a feed's menu.
 * @param {{ lastError?: string|null, lastRefreshedAt?: string|null }} feed
 * @param {number} [now]
 * @returns {{ text: string, isError: boolean }}
 */
export function feedHealth(feed, now = Date.now()) {
  if (feed?.lastError) {
    return { text: `Couldn’t update: ${feed.lastError}`, isError: true };
  }
  const updated = feed?.lastRefreshedAt
    ? relativeTimeText(feed.lastRefreshedAt, now)
    : null;
  return {
    text: updated ? `Updated ${updated}` : "Not updated yet",
    isError: false,
  };
}

/**
 * Progress line for a running background job.
 * @param {"Refreshing"|"Importing"} verb
 * @param {{ total?: number, completed?: number, failed?: number }} job
 */
export function jobProgressText(verb, job) {
  const total = Number(job?.total) || 0;
  const done = (Number(job?.completed) || 0) + (Number(job?.failed) || 0);
  if (total <= 0) return `${verb}…`;
  return `${verb} ${Math.min(done, total)} of ${total}…`;
}

/**
 * Toast after a finished refresh; null when every feed updated.
 * @param {{ failed?: number }} job
 * @returns {string|null}
 */
export function refreshSummaryText(job) {
  const failed = Number(job?.failed) || 0;
  if (failed <= 0) return null;
  return failed === 1
    ? "1 feed couldn’t be updated. It’s marked in the sidebar."
    : `${failed} feeds couldn’t be updated. They’re marked in the sidebar.`;
}
