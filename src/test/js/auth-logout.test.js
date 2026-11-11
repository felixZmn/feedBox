import assert from "node:assert/strict";
import test, { afterEach } from "node:test";

import {
  handleLogoutStorageEvent,
  isLoggedOut,
  logout,
} from "../../main/resources/META-INF/resources/scripts/auth.js";

afterEach(() => {
  delete globalThis.localStorage;
  delete globalThis.window;
  delete globalThis.document;
});

test("logout writes the cross-tab marker before navigating", () => {
  const written = {};
  const removed = [];
  let assigned = null;
  globalThis.localStorage = {
    setItem: (k, v) => (written[k] = v),
    removeItem: (k) => removed.push(k),
  };
  globalThis.window = { location: { assign: (url) => (assigned = url) } };

  logout();

  assert.match(written["feedbox-logout"], /^\d+$/);
  assert.equal(assigned, "/logout");
});

test("storage events for other keys are ignored", () => {
  assert.equal(handleLogoutStorageEvent({ key: "folder-state", newValue: "1" }), false);
  assert.equal(handleLogoutStorageEvent({ key: "feedbox-logout", newValue: null }), false);
  assert.equal(isLoggedOut(), false);
});

test("a logout in another tab signs this tab out with a reason", () => {
  const els = { "login-reason": { hidden: true, textContent: "" } };
  globalThis.document = {
    body: { classList: { add() {}, remove() {} } },
    getElementById: (id) => els[id] ?? null,
    querySelector: () => null,
  };
  globalThis.localStorage = { removeItem() {} };

  assert.equal(
    handleLogoutStorageEvent({ key: "feedbox-logout", newValue: "123" }),
    true,
  );
  assert.equal(isLoggedOut(), true);
  assert.match(els["login-reason"].textContent, /another tab/i);
});
