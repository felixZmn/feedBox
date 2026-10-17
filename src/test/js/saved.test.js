import assert from "node:assert/strict";
import test from "node:test";

import { savedBadgePresentation } from "../../main/resources/META-INF/resources/scripts/saved.js";

test("saved badge hides zero and missing counts", () => {
  assert.deepEqual(savedBadgePresentation(0), {
    hidden: true,
    text: "",
    label: null,
  });
  assert.deepEqual(savedBadgePresentation(undefined), {
    hidden: true,
    text: "",
    label: null,
  });
  assert.deepEqual(savedBadgePresentation(null), {
    hidden: true,
    text: "",
    label: null,
  });
});

test("saved badge shows a positive count with an accessible name", () => {
  assert.deepEqual(savedBadgePresentation(4), {
    hidden: false,
    text: "4",
    label: "4 saved",
  });
});
