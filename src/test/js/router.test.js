import assert from "node:assert/strict";
import test from "node:test";

import {
  buildPath,
  isAppPath,
  parseRoute,
} from "../../main/resources/META-INF/resources/scripts/router.js";

test("parseRoute accepts root as all feeds", () => {
  assert.deepEqual(parseRoute("/"), {
    valid: true,
    context: "all",
    contextId: null,
    articleId: null,
  });
});

test("parseRoute accepts feed and folder list paths", () => {
  assert.deepEqual(parseRoute("/feed/12"), {
    valid: true,
    context: "feed",
    contextId: 12,
    articleId: null,
  });
  assert.deepEqual(parseRoute("/folder/3"), {
    valid: true,
    context: "folder",
    contextId: 3,
    articleId: null,
  });
});

test("parseRoute accepts article under a list or all feeds", () => {
  assert.deepEqual(parseRoute("/feed/12/article/99"), {
    valid: true,
    context: "feed",
    contextId: 12,
    articleId: 99,
  });
  assert.deepEqual(parseRoute("/folder/3/article/99"), {
    valid: true,
    context: "folder",
    contextId: 3,
    articleId: 99,
  });
  assert.deepEqual(parseRoute("/article/99"), {
    valid: true,
    context: "all",
    contextId: null,
    articleId: 99,
  });
});

test("parseRoute rejects unknown paths", () => {
  assert.equal(parseRoute("/feeds").valid, false);
  assert.equal(parseRoute("/feed/abc").valid, false);
  assert.equal(parseRoute("/api/article").valid, false);
  assert.equal(parseRoute("/feed/1/extra").valid, false);
});

test("parseRoute trims a trailing slash", () => {
  assert.deepEqual(parseRoute("/feed/12/"), {
    valid: true,
    context: "feed",
    contextId: 12,
    articleId: null,
  });
});

test("parseRoute accepts unread and saved", () => {
  assert.deepEqual(parseRoute("/unread"), {
    valid: true,
    context: "unread",
    contextId: null,
    articleId: null,
  });
  assert.deepEqual(parseRoute("/saved/article/4"), {
    valid: true,
    context: "saved",
    contextId: null,
    articleId: 4,
  });
  assert.equal(buildPath({ context: "unread" }), "/unread");
  assert.equal(buildPath({ context: "unread", articleId: 8 }), "/unread/article/8");
  assert.equal(buildPath({ context: "saved" }), "/saved");
  assert.equal(buildPath({ context: "saved", articleId: 4 }), "/saved/article/4");
  assert.equal(isAppPath("/unread/article/8"), true);
  assert.equal(isAppPath("/saved"), true);
});

test("buildPath mirrors parseRoute", () => {
  assert.equal(buildPath({}), "/");
  assert.equal(buildPath({ context: "feed", contextId: 12 }), "/feed/12");
  assert.equal(buildPath({ context: "folder", contextId: 3 }), "/folder/3");
  assert.equal(
    buildPath({ context: "feed", contextId: 12, articleId: 99 }),
    "/feed/12/article/99",
  );
  assert.equal(buildPath({ articleId: 99 }), "/article/99");
});

test("isAppPath matches valid routes only", () => {
  assert.equal(isAppPath("/"), true);
  assert.equal(isAppPath("/feed/1/article/2"), true);
  assert.equal(isAppPath("/q/health"), false);
});
