import assert from "node:assert/strict";
import test from "node:test";

import {
  articleMarkState,
  articleStarState,
} from "../../main/resources/META-INF/resources/scripts/article-row.js";

test("unread mark is a filled circle named Mark as read", () => {
  assert.deepEqual(articleMarkState(false), {
    src: "icons/circle-check.svg",
    label: "Mark as read",
    tone: "f-blue",
  });
});

test("read mark is the matching unfilled circle named Read", () => {
  assert.deepEqual(articleMarkState(true), {
    src: "icons/circle-check-filled.svg",
    label: "Read",
    tone: "f-grey",
  });
});

test("unsaved star is an outline named Save article", () => {
  assert.deepEqual(articleStarState(false), {
    src: "icons/bookmark.svg",
    label: "Save article",
    tone: "f-grey",
  });
});

test("saved star is filled and named Remove from Saved", () => {
  assert.deepEqual(articleStarState(true), {
    src: "icons/bookmark-filled.svg",
    label: "Remove from Saved",
    tone: "f-blue",
  });
});
