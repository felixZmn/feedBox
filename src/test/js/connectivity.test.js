import assert from "node:assert/strict";
import test, { afterEach } from "node:test";

import {
  dismissOfflineToast,
  isConnectionPending,
  isConnectivityFailure,
  isOfflineResponse,
  notifyOffline,
  onOfflineRetry,
} from "../../main/resources/META-INF/resources/scripts/connectivity.js";

afterEach(() => {
  dismissOfflineToast();
  onOfflineRetry(null);
});

function fakeElement(tag) {
  const classes = new Set();
  return {
    tag,
    children: [],
    textContent: "",
    className: "",
    classList: {
      add: (c) => classes.add(c),
      remove: (c) => classes.delete(c),
      contains: (c) => classes.has(c),
    },
    append(...nodes) {
      this.children.push(...nodes);
    },
    replaceChildren() {
      this.children = [];
    },
    remove() {
      this.removed = true;
    },
    addEventListener(type, fn) {
      this.listeners ??= {};
      this.listeners[type] = fn;
    },
  };
}

function installToastRegion() {
  const region = fakeElement("div");
  globalThis.document = {
    getElementById: (id) => (id === "toast-region" ? region : null),
    createElement: fakeElement,
  };
  globalThis.window = globalThis;
  globalThis.matchMedia = () => ({ matches: true });
  return region;
}

function response(status, { headers = {} } = {}) {
  const map = new Map(
    Object.entries(headers).map(([k, v]) => [k.toLowerCase(), v]),
  );
  return {
    status,
    ok: status >= 200 && status < 300,
    headers: { get: (name) => map.get(name.toLowerCase()) ?? null },
  };
}

test("isOfflineResponse: true only when X-FeedBox-Offline is set", () => {
  assert.equal(
    isOfflineResponse(response(503, { headers: { "X-FeedBox-Offline": "1" } })),
    true,
  );
  assert.equal(isOfflineResponse(response(503)), false);
  assert.equal(isOfflineResponse(null), false);
});

test("isConnectivityFailure: OFFLINE code, TypeError, and wrapped TypeError", () => {
  const offline = new Error("Offline");
  offline.code = "OFFLINE";
  assert.equal(isConnectivityFailure(offline), true);
  assert.equal(isConnectivityFailure(new TypeError("Failed to fetch")), true);
  assert.equal(
    isConnectivityFailure(
      new Error("wrapped", { cause: new TypeError("Failed to fetch") }),
    ),
    true,
  );
  assert.equal(isConnectivityFailure(new Error("HTTP 500")), false);
  assert.equal(isConnectivityFailure(null), false);
  assert.equal(isConnectivityFailure("offline"), false);
});

test("notifyOffline: sticky toast with Retry invokes onOfflineRetry", () => {
  const region = installToastRegion();
  let retries = 0;
  onOfflineRetry(() => {
    retries += 1;
  });

  notifyOffline();
  assert.equal(isConnectionPending(), true);
  assert.equal(region.children.length, 1);
  const toastEl = region.children[0];
  assert.equal(toastEl.children[0].textContent, "Can't reach FeedBox.");
  assert.equal(toastEl.children[1].textContent, "Retry");

  toastEl.children[1].listeners.click();
  assert.equal(retries, 1);
});

test("dismissOfflineToast clears pending and removes the toast", () => {
  const region = installToastRegion();
  notifyOffline();
  assert.equal(isConnectionPending(), true);

  dismissOfflineToast();
  assert.equal(isConnectionPending(), false);
  assert.equal(region.children[0].removed, true);
});
