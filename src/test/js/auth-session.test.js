import assert from "node:assert/strict";
import test, { afterEach } from "node:test";

import {
  enterLoggedOut,
  rememberReturnTo,
  takeReturnTo,
} from "../../main/resources/META-INF/resources/scripts/auth.js";

function memoryStorage() {
  const map = new Map();
  return {
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: (k) => map.delete(k),
  };
}

test("returnTo: restores an app deep link once, only on the root", () => {
  const storage = memoryStorage();
  rememberReturnTo("/feed/3/article/9", storage);
  assert.equal(takeReturnTo("/", storage), "/feed/3/article/9");
  assert.equal(takeReturnTo("/", storage), null);
});

test("returnTo: ignores non-app paths and clears stale values", () => {
  const storage = memoryStorage();
  rememberReturnTo("/logout", storage);
  assert.equal(takeReturnTo("/", storage), null);

  rememberReturnTo("/saved", storage);
  assert.equal(takeReturnTo("/feed/1", storage), null);
  assert.equal(takeReturnTo("/", storage), null);
});

test("returnTo: never restores the root itself", () => {
  const storage = memoryStorage();
  rememberReturnTo("/", storage);
  assert.equal(takeReturnTo("/", storage), null);
});

function fakeDom() {
  const els = {
    "app-shell": { hidden: false },
    "login-panel": { hidden: true },
    "login-reason": { hidden: true, textContent: "" },
  };
  globalThis.document = {
    body: { classList: { add() {}, remove() {} } },
    getElementById: (id) => els[id] ?? null,
    querySelector: () => null,
  };
  return els;
}

afterEach(() => {
  delete globalThis.document;
});

test("enterLoggedOut explains why the session ended", () => {
  const els = fakeDom();
  // Module state is shared across tests; only this test signs out.
  enterLoggedOut("expired");
  assert.equal(els["login-reason"].hidden, false);
  assert.match(els["login-reason"].textContent, /session ended/i);
  assert.equal(els["login-panel"].hidden, false);
  assert.equal(els["app-shell"].hidden, true);
});
