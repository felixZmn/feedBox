import assert from "node:assert/strict";
import test from "node:test";

import {
  resolveContentUrl,
  resolveSrcset,
  isSummaryOnly,
  getRelativeTime,
  parseDate,
} from "../../main/resources/META-INF/resources/scripts/util.js";

const BASE = "https://example.com/blog/post/1?x=1";

test("resolveContentUrl resolves relative paths against the base", () => {
  assert.equal(resolveContentUrl("img/a.png", BASE), "https://example.com/blog/post/img/a.png");
  assert.equal(resolveContentUrl("/a.png", BASE), "https://example.com/a.png");
  assert.equal(resolveContentUrl("../a.png", BASE), "https://example.com/blog/a.png");
  assert.equal(resolveContentUrl("//cdn.example.org/a.png", BASE), "https://cdn.example.org/a.png");
  assert.equal(resolveContentUrl("#notes", BASE), "https://example.com/blog/post/1?x=1#notes");
});

test("resolveContentUrl leaves absolute and unresolvable values alone", () => {
  assert.equal(resolveContentUrl("https://other.test/a.png", BASE), "https://other.test/a.png");
  assert.equal(resolveContentUrl("mailto:a@b.test", BASE), "mailto:a@b.test");
  assert.equal(resolveContentUrl("a.png", "not a url"), "a.png");
  assert.equal(resolveContentUrl("a.png", ""), "a.png");
  assert.equal(resolveContentUrl("", BASE), "");
});

test("resolveSrcset resolves every candidate and keeps descriptors", () => {
  assert.equal(
    resolveSrcset("a.png 1x, /b.png 2x, https://x.test/c.png 640w", BASE),
    "https://example.com/blog/post/a.png 1x, https://example.com/b.png 2x, https://x.test/c.png 640w",
  );
});

test("resolveSrcset handles commas inside URLs and bare candidates", () => {
  assert.equal(
    resolveSrcset("/w_100,h_100/a.png 100w,/w_200,h_200/a.png 200w", BASE),
    "https://example.com/w_100,h_100/a.png 100w, https://example.com/w_200,h_200/a.png 200w",
  );
  assert.equal(resolveSrcset("a.png, b.png", BASE), "https://example.com/blog/post/a.png, https://example.com/blog/post/b.png");
  assert.equal(resolveSrcset("", BASE), "");
});

test("isSummaryOnly needs a safe link and no full content", () => {
  const link = "https://example.com/a";
  assert.equal(isSummaryOnly({ link, description: "teaser" }), true);
  assert.equal(isSummaryOnly({ link, content: "  ", description: "teaser" }), true);
  assert.equal(isSummaryOnly({ link, content: "<p>full</p>" }), false);
  assert.equal(isSummaryOnly({ content: "", description: "teaser" }), false);
  assert.equal(isSummaryOnly({ link: "javascript:alert(1)" }), false);
  assert.equal(isSummaryOnly(null), false);
});

test("getRelativeTime uses compact ages and maps the future to now", () => {
  const now = Date.parse("2024-06-01T12:00:00.000Z");
  assert.equal(getRelativeTime("2024-06-01T11:55:00.000Z", now), "5m");
  assert.equal(getRelativeTime("2024-06-01T10:00:00.000Z", now), "2h");
  assert.equal(getRelativeTime("2024-06-01T13:00:00.000Z", now), "now");
  assert.equal(getRelativeTime(null, now), null);
});

test("parseDate omits seconds", () => {
  const formatted = parseDate("2024-06-01T12:34:56.000Z");
  assert.ok(formatted);
  assert.equal(/:\d{2}:\d{2}/.test(formatted), false);
});
