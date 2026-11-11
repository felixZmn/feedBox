import assert from "node:assert/strict";
import test from "node:test";

// state.js / nav.js touch the DOM at import time.
globalThis.document = {
  getElementById: () => null,
  readyState: "complete",
  addEventListener() {},
};
globalThis.window = globalThis;
globalThis.requestAnimationFrame = (cb) => setTimeout(cb, 0);
globalThis.matchMedia = () => ({
  matches: false,
  addEventListener() {},
  removeEventListener() {},
  addListener() {},
  removeListener() {},
});

const { contextDisplayName } = await import(
  "../../main/resources/META-INF/resources/scripts/search-ui.js"
);

test("contextDisplayName names feeds, folders and built-in views", () => {
  assert.equal(contextDisplayName("feed", { name: "Hackaday" }), "Hackaday");
  assert.equal(contextDisplayName("folder", { name: "News" }), "News");
  assert.equal(contextDisplayName("unread", null), "Unread");
  assert.equal(contextDisplayName("saved", null), "Saved");
  assert.equal(contextDisplayName("", null), "All Feeds");
  assert.equal(contextDisplayName("feed", null), "All Feeds");
});
