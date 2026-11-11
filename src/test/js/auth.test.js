import assert from "node:assert/strict";
import test, { afterEach } from "node:test";

import {
  apiFetch,
  isLoggedOut,
  probeSession,
} from "../../main/resources/META-INF/resources/scripts/auth.js";

const realFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = realFetch;
});

function stubFetch(result) {
  globalThis.fetch = async () => {
    if (result instanceof Error) throw result;
    return result;
  };
}

function response(status, { headers = {}, type = "basic" } = {}) {
  const map = new Map(
    Object.entries(headers).map(([k, v]) => [k.toLowerCase(), v]),
  );
  return {
    status,
    type,
    ok: status >= 200 && status < 300,
    headers: { get: (name) => map.get(name.toLowerCase()) ?? null },
  };
}

test("probeSession: 200 is signed-in", async () => {
  stubFetch(response(200));
  assert.equal(await probeSession(), "signed-in");
});

test("probeSession: 401, 499 and redirects are signed-out", async () => {
  for (const res of [
    response(401),
    response(499),
    response(302),
    response(0, { type: "opaqueredirect" }),
  ]) {
    stubFetch(res);
    assert.equal(await probeSession(), "signed-out");
  }
});

test("probeSession: network errors are unavailable", async () => {
  stubFetch(new TypeError("Failed to fetch"));
  assert.equal(await probeSession(), "unavailable");
});

test("probeSession: server errors and SW offline responses are unavailable", async () => {
  for (const res of [
    response(500),
    response(503),
    response(503, { headers: { "X-FeedBox-Offline": "1" } }),
  ]) {
    stubFetch(res);
    assert.equal(await probeSession(), "unavailable");
  }
});

test("apiFetch: network failure rethrows without signing out", async () => {
  stubFetch(new TypeError("Failed to fetch"));
  await assert.rejects(() => apiFetch("/api/folder"), TypeError);
  assert.equal(isLoggedOut(), false);
});

test("apiFetch: offline response is returned without signing out", async () => {
  const offline = response(503, { headers: { "X-FeedBox-Offline": "1" } });
  stubFetch(offline);
  assert.equal(await apiFetch("/api/folder"), offline);
  assert.equal(isLoggedOut(), false);
});
