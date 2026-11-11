import assert from "node:assert/strict";
import test from "node:test";

function installStoragePolyfill() {
  const create = () => {
    const map = new Map();
    return {
      getItem: (key) => (map.has(key) ? map.get(key) : null),
      setItem: (key, value) => map.set(key, String(value)),
      removeItem: (key) => map.delete(key),
      clear: () => map.clear(),
    };
  };
  globalThis.localStorage = create();
  globalThis.sessionStorage = create();
}

installStoragePolyfill();
const { dataService, parseApiError } = await import(
  "../../main/resources/META-INF/resources/scripts/data.js"
);

function jsonResponse(status, body) {
  return new Response(body == null ? null : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

test("parseApiError reads code and message from JSON", () => {
  assert.deepEqual(
    parseApiError('{"code":"DUPLICATE","message":"Feed exists"}'),
    { code: "DUPLICATE", detail: "Feed exists" },
  );
});

test("parseApiError keeps short plain text as detail without a code", () => {
  assert.deepEqual(parseApiError("Bad gateway"), {
    code: null,
    detail: "Bad gateway",
  });
  assert.deepEqual(parseApiError(""), { code: null, detail: null });
  assert.equal(parseApiError("<html>" + "x".repeat(300)).detail, null);
});

test("_request exposes status, code and detail on API errors", async (t) => {
  t.mock.method(globalThis, "fetch", async () =>
    jsonResponse(422, { code: "NOT_A_FEED", message: "No feed at this address" }),
  );

  await assert.rejects(dataService.checkFeed("https://example.com"), (error) => {
    assert.equal(error.status, 422);
    assert.equal(error.code, "NOT_A_FEED");
    assert.equal(error.detail, "No feed at this address");
    return true;
  });
});
