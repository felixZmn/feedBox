import assert from "node:assert/strict";
import test from "node:test";

import {
  CHROME_MENU_ORDER,
  PROFILE_ICON_PATH,
  PROFILE_LABEL,
  layoutBandFromWidth,
  layoutBands,
  pageBackState,
  readerActionsVisible,
} from "../../main/resources/META-INF/resources/scripts/page-header.js";

test("Profile control uses fixed person icon and name Profile", () => {
  assert.equal(PROFILE_LABEL, "Profile");
  assert.equal(PROFILE_ICON_PATH, "icons/profile_placeholder.svg");
  assert.doesNotMatch(PROFILE_ICON_PATH, /http|picture|avatar/i);
});

test("chrome menu order is Import, Export, Shortcuts, Sign Out", () => {
  assert.deepEqual([...CHROME_MENU_ORDER], [
    "Import OPML",
    "Export OPML",
    "Keyboard shortcuts",
    "Sign Out",
  ]);
});

test("readerActionsVisible matches articleOpen", () => {
  assert.equal(readerActionsVisible(true), true);
  assert.equal(readerActionsVisible(false), false);
  assert.equal(readerActionsVisible(null), false);
});

test("layoutBandFromWidth matches contract breakpoints", () => {
  assert.equal(layoutBandFromWidth(375), layoutBands.NARROW);
  assert.equal(layoutBandFromWidth(700), layoutBands.NARROW);
  assert.equal(layoutBandFromWidth(701), layoutBands.MEDIUM);
  assert.equal(layoutBandFromWidth(1100), layoutBands.MEDIUM);
  assert.equal(layoutBandFromWidth(1101), layoutBands.WIDE);
});

test("pageBackState matrix: wide never shows back", () => {
  for (const column of ["feeds-list", "articles-list", "reader"]) {
    const state = pageBackState(column, layoutBands.WIDE);
    assert.equal(state.visible, false);
    assert.equal(state.target, null);
  }
});

test("pageBackState matrix: medium feeds/articles hidden, reader back to articles", () => {
  assert.equal(
    pageBackState("feeds-list", layoutBands.MEDIUM).visible,
    false,
  );
  assert.equal(
    pageBackState("articles-list", layoutBands.MEDIUM).visible,
    false,
  );
  const reader = pageBackState("reader", layoutBands.MEDIUM);
  assert.equal(reader.visible, true);
  assert.equal(reader.label, "Back to articles");
  assert.equal(reader.target, "articles");
});

test("pageBackState matrix: narrow feeds/articles/reader", () => {
  assert.equal(
    pageBackState("feeds-list", layoutBands.NARROW).visible,
    false,
  );
  const articles = pageBackState("articles-list", layoutBands.NARROW);
  assert.equal(articles.visible, true);
  assert.equal(articles.label, "Back to feeds");
  assert.equal(articles.target, "feeds");
  const reader = pageBackState("reader", layoutBands.NARROW);
  assert.equal(reader.visible, true);
  assert.equal(reader.label, "Back to articles");
  assert.equal(reader.target, "articles");
});
