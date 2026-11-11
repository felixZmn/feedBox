import assert from "node:assert/strict";
import test from "node:test";

import {
  shouldIgnoreKey,
  resolveShortcutAction,
} from "../../main/resources/META-INF/resources/scripts/shortcuts.js";

function keyEvent(partial) {
  return {
    key: "",
    ctrlKey: false,
    altKey: false,
    metaKey: false,
    shiftKey: false,
    target: { tagName: "BODY" },
    ...partial,
  };
}

test("shouldIgnoreKey skips typing surfaces and open dialogs", () => {
  assert.equal(shouldIgnoreKey(keyEvent({}), { dialogOpen: true }), true);
  assert.equal(
    shouldIgnoreKey(keyEvent({ target: { tagName: "INPUT" } })),
    true,
  );
  assert.equal(
    shouldIgnoreKey(keyEvent({ target: { tagName: "TEXTAREA" } })),
    true,
  );
  assert.equal(
    shouldIgnoreKey(keyEvent({ target: { tagName: "SELECT" } })),
    true,
  );
  assert.equal(
    shouldIgnoreKey(
      keyEvent({ target: { tagName: "DIV", isContentEditable: true } }),
    ),
    true,
  );
  assert.equal(shouldIgnoreKey(keyEvent({ key: "j" })), false);
});

test("shouldIgnoreKey skips modifiers except Shift+?", () => {
  assert.equal(shouldIgnoreKey(keyEvent({ key: "j", ctrlKey: true })), true);
  assert.equal(shouldIgnoreKey(keyEvent({ key: "j", metaKey: true })), true);
  assert.equal(shouldIgnoreKey(keyEvent({ key: "j", altKey: true })), true);
  assert.equal(shouldIgnoreKey(keyEvent({ key: "J", shiftKey: true })), true);
  assert.equal(
    shouldIgnoreKey(keyEvent({ key: "?", shiftKey: true })),
    false,
  );
});

test("resolveShortcutAction maps keys when enabled", () => {
  assert.equal(resolveShortcutAction(keyEvent({ key: "j" })), "next");
  assert.equal(resolveShortcutAction(keyEvent({ key: "k" })), "previous");
  assert.equal(resolveShortcutAction(keyEvent({ key: "o" })), "open");
  assert.equal(resolveShortcutAction(keyEvent({ key: "m" })), "markRead");
  assert.equal(resolveShortcutAction(keyEvent({ key: "s" })), "save");
  assert.equal(resolveShortcutAction(keyEvent({ key: "v" })), "external");
  assert.equal(resolveShortcutAction(keyEvent({ key: "/" })), "search");
  assert.equal(resolveShortcutAction(keyEvent({ key: "r" })), "refresh");
  assert.equal(resolveShortcutAction(keyEvent({ key: "u" })), "back");
  assert.equal(resolveShortcutAction(keyEvent({ key: "Escape" })), "back");
  assert.equal(resolveShortcutAction(keyEvent({ key: "?" })), "help");
  assert.equal(resolveShortcutAction(keyEvent({ key: "x" })), null);
});

test("resolveShortcutAction keeps help when shortcuts are off", () => {
  assert.equal(
    resolveShortcutAction(keyEvent({ key: "?" }), { enabled: false }),
    "help",
  );
  assert.equal(
    resolveShortcutAction(keyEvent({ key: "j" }), { enabled: false }),
    null,
  );
  assert.equal(
    resolveShortcutAction(keyEvent({ key: "r" }), { enabled: false }),
    null,
  );
});
