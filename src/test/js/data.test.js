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
const { dataService, parseApiError, normalizeFeed, folderTreesEqual } = await import(
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

test("waitForJob reports every snapshot until the job finishes", async (t) => {
  const snapshots = [
    { id: "j", status: "RUNNING", total: 3, completed: 1, failed: 0 },
    { id: "j", status: "RUNNING", total: 3, completed: 2, failed: 0 },
    { id: "j", status: "COMPLETED", total: 3, completed: 2, failed: 1 },
  ];
  t.mock.method(globalThis, "fetch", async () =>
    jsonResponse(200, snapshots.shift()),
  );

  const seen = [];
  const job = await dataService.waitForJob("j", {
    intervalMs: 1,
    maxIntervalMs: 1,
    onProgress: (snapshot) => seen.push(snapshot.completed + snapshot.failed),
  });

  assert.equal(job.status, "COMPLETED");
  assert.deepEqual(seen, [1, 2, 3]);
});

test("countArticles sends since_id and scope query params", async (t) => {
  const seen = [];
  t.mock.method(globalThis, "fetch", async (url) => {
    seen.push(String(url));
    return jsonResponse(200, { count: 3 });
  });

  const result = await dataService.countArticles({
    sinceId: 40,
    feed: 7,
  });

  assert.equal(result.count, 3);
  assert.match(seen[0], /\/api\/article\/count\?/);
  assert.match(seen[0], /since_id=40/);
  assert.match(seen[0], /feed=7/);
});

test("normalizeFeed keeps lastRefreshedAt and latestArticleId but the tree fingerprint ignores them", () => {
  const feed = {
    id: 1,
    folderId: null,
    name: "Blog",
    feedUrl: "https://example.com/rss",
    lastRefreshedAt: "2026-11-11T10:00:00Z",
    latestArticleId: 99,
  };
  assert.equal(normalizeFeed(feed).lastRefreshedAt, "2026-11-11T10:00:00Z");
  assert.equal(normalizeFeed(feed).latestArticleId, 99);
  assert.equal(normalizeFeed({ ...feed, latestArticleId: undefined }).latestArticleId, 0);

  const tree = (refreshed, error = null, latest = 99) => ({
    folders: [
      {
        id: 10,
        name: "F",
        feeds: [
          {
            ...feed,
            folderId: 10,
            lastRefreshedAt: refreshed,
            lastError: error,
            latestArticleId: latest,
          },
        ],
      },
    ],
    unfiledFeeds: [{ ...feed, id: 2, lastRefreshedAt: refreshed, latestArticleId: latest }],
  });
  assert.equal(
    folderTreesEqual(tree("2026-11-11T10:00:00Z"), tree("2026-11-11T11:00:00Z")),
    true,
  );
  assert.equal(
    folderTreesEqual(tree("2026-11-11T10:00:00Z", null, 99), tree("2026-11-11T10:00:00Z", null, 100)),
    true,
  );
  assert.equal(
    folderTreesEqual(tree("2026-11-11T10:00:00Z"), tree("2026-11-11T10:00:00Z", "HTTP 404")),
    false,
  );
});
