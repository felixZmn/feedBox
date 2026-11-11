import assert from "node:assert/strict";
import test from "node:test";

import {
  relativeTimeText,
  feedHealth,
  jobProgressText,
  refreshSummaryText,
} from "../../main/resources/META-INF/resources/scripts/feed-health.js";

const NOW = Date.parse("2026-11-11T12:00:00Z");
const ago = (ms) => new Date(NOW - ms).toISOString();
const MIN = 60 * 1000;
const HOUR = 60 * MIN;

test("relativeTimeText rounds down to the largest unit", () => {
  assert.equal(relativeTimeText(ago(20 * 1000), NOW), "just now");
  assert.equal(relativeTimeText(ago(5 * MIN), NOW), "5 minutes ago");
  assert.equal(relativeTimeText(ago(3 * HOUR + 10 * MIN), NOW), "3 hours ago");
  assert.equal(relativeTimeText(ago(30 * HOUR), NOW), "yesterday");
  assert.equal(relativeTimeText(ago(12 * 24 * HOUR), NOW), "12 days ago");
  assert.equal(relativeTimeText("not a date", NOW), null);
});

test("relativeTimeText treats future timestamps as now", () => {
  assert.equal(relativeTimeText(new Date(NOW + HOUR).toISOString(), NOW), "just now");
});

test("feedHealth prefers the last error", () => {
  assert.deepEqual(
    feedHealth({ lastError: "HTTP 404", lastRefreshedAt: ago(HOUR) }, NOW),
    { text: "Couldn’t update: HTTP 404", isError: true },
  );
});

test("feedHealth shows when the feed last updated", () => {
  assert.deepEqual(feedHealth({ lastRefreshedAt: ago(3 * HOUR) }, NOW), {
    text: "Updated 3 hours ago",
    isError: false,
  });
  assert.deepEqual(feedHealth({ lastRefreshedAt: null, lastError: null }, NOW), {
    text: "Not updated yet",
    isError: false,
  });
});

test("jobProgressText counts finished and failed items", () => {
  assert.equal(
    jobProgressText("Refreshing", { total: 300, completed: 110, failed: 10 }),
    "Refreshing 120 of 300…",
  );
  assert.equal(jobProgressText("Importing", { total: 0 }), "Importing…");
  assert.equal(jobProgressText("Importing", {}), "Importing…");
});

test("refreshSummaryText is silent when nothing failed", () => {
  assert.equal(refreshSummaryText({ failed: 0 }), null);
  assert.match(refreshSummaryText({ failed: 1 }), /^1 feed couldn’t/);
  assert.match(refreshSummaryText({ failed: 4 }), /^4 feeds couldn’t/);
});
