import assert from "node:assert/strict";
import test from "node:test";

import {
  normalizeFeedUrl,
  feedErrorMessage,
} from "../../main/resources/META-INF/resources/scripts/feed-form.js";

test("normalizeFeedUrl adds https to bare hosts and paths", () => {
  assert.equal(normalizeFeedUrl("example.com"), "https://example.com/");
  assert.equal(
    normalizeFeedUrl("  example.com/blog/feed.xml "),
    "https://example.com/blog/feed.xml",
  );
  assert.equal(normalizeFeedUrl("example.com:8080/rss"), "https://example.com:8080/rss");
});

test("normalizeFeedUrl keeps explicit http and https", () => {
  assert.equal(normalizeFeedUrl("http://example.com/rss"), "http://example.com/rss");
  assert.equal(normalizeFeedUrl("HTTPS://Example.com/rss"), "https://example.com/rss");
});

test("normalizeFeedUrl unwraps feed: subscribe links", () => {
  assert.equal(normalizeFeedUrl("feed://example.com/rss"), "https://example.com/rss");
  assert.equal(normalizeFeedUrl("feed:http://example.com/rss"), "http://example.com/rss");
});

test("normalizeFeedUrl rejects empty and non-web input", () => {
  assert.equal(normalizeFeedUrl(""), null);
  assert.equal(normalizeFeedUrl("   "), null);
  assert.equal(normalizeFeedUrl(undefined), null);
  assert.equal(normalizeFeedUrl("ftp://example.com/rss"), null);
  assert.equal(normalizeFeedUrl("javascript://alert(1)"), null);
  assert.equal(normalizeFeedUrl("exa mple.com"), null);
});

test("feedErrorMessage maps server codes to copy", () => {
  assert.equal(
    feedErrorMessage({ status: 409, code: "DUPLICATE" }),
    "You already follow this feed.",
  );
  assert.equal(
    feedErrorMessage({ status: 422, code: "NOT_A_FEED", detail: "Not an RSS or Atom feed" }),
    "No RSS or Atom feed was found at this address.",
  );
  assert.equal(
    feedErrorMessage({ status: 422, code: "HTTP_STATUS", detail: "HTTP 404" }),
    "The site answered with an error (HTTP 404).",
  );
  assert.equal(
    feedErrorMessage({ status: 422, code: "UNREACHABLE", detail: "Host not found" }),
    "Couldn’t reach this site (Host not found).",
  );
});

test("feedErrorMessage falls back for offline, conflicts and unknown codes", () => {
  assert.match(feedErrorMessage(new Error("Failed to fetch")), /connection/);
  assert.equal(feedErrorMessage({ status: 409 }), "You already follow this feed.");
  assert.equal(
    feedErrorMessage({ status: 400, code: "BAD_REQUEST", detail: "url is required" }),
    "url is required",
  );
  assert.equal(
    feedErrorMessage({ status: 500, code: "INTERNAL" }),
    "Something went wrong. Please try again.",
  );
});
