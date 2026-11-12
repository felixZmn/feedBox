import assert from "node:assert/strict";
import test from "node:test";

import {
  buildPath,
  isAppPath,
  isSameContext,
  parseRoute,
  resolveRoute,
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

test("isSameContext matches all, feed, folder, unread and saved", () => {
  assert.equal(
    isSameContext(parseRoute("/"), { type: "", obj: null }),
    true,
  );
  assert.equal(
    isSameContext(parseRoute("/unread"), { type: "unread", obj: null }),
    true,
  );
  assert.equal(
    isSameContext(parseRoute("/unread/article/9"), {
      type: "unread",
      obj: null,
    }),
    true,
  );
  assert.equal(
    isSameContext(parseRoute("/saved"), { type: "saved", obj: null }),
    true,
  );
  assert.equal(
    isSameContext(parseRoute("/feed/12"), { type: "feed", obj: { id: 12 } }),
    true,
  );
  assert.equal(
    isSameContext(parseRoute("/folder/3"), {
      type: "folder",
      obj: { id: 3 },
    }),
    true,
  );
  assert.equal(
    isSameContext(parseRoute("/unread"), { type: "saved", obj: null }),
    false,
  );
  assert.equal(
    isSameContext(parseRoute("/feed/12"), { type: "feed", obj: { id: 99 } }),
    false,
  );
  assert.equal(
    isSameContext(parseRoute("/"), { type: "unread", obj: null }),
    false,
  );
});

test("resolveRoute keeps a live article on the named list", () => {
  const present = { feedExists: true, folderExists: true, articleExists: true };
  assert.deepEqual(resolveRoute(parseRoute("/"), present), {
    status: "ready",
    context: "all",
    contextId: null,
    articleId: null,
    replacePath: null,
  });
  assert.deepEqual(resolveRoute(parseRoute("/article/99"), present), {
    status: "ready",
    context: "all",
    contextId: null,
    articleId: 99,
    replacePath: null,
  });
  assert.deepEqual(resolveRoute(parseRoute("/unread/article/8"), present), {
    status: "ready",
    context: "unread",
    contextId: null,
    articleId: 8,
    replacePath: null,
  });
  assert.deepEqual(resolveRoute(parseRoute("/saved/article/4"), present), {
    status: "ready",
    context: "saved",
    contextId: null,
    articleId: 4,
    replacePath: null,
  });
  assert.deepEqual(
    resolveRoute(parseRoute("/feed/12/article/99"), present),
    {
      status: "ready",
      context: "feed",
      contextId: 12,
      articleId: 99,
      replacePath: null,
    },
  );
  assert.deepEqual(
    resolveRoute(parseRoute("/folder/3/article/99"), present),
    {
      status: "ready",
      context: "folder",
      contextId: 3,
      articleId: 99,
      replacePath: null,
    },
  );
  assert.deepEqual(resolveRoute(parseRoute("/feed/12/"), present), {
    status: "ready",
    context: "feed",
    contextId: 12,
    articleId: null,
    replacePath: null,
  });
});

test("resolveRoute does not use list membership", () => {
  const foreign = resolveRoute(parseRoute("/feed/12/article/99"), {
    feedExists: true,
    folderExists: true,
    articleExists: true,
  });
  assert.equal(foreign.status, "ready");
  assert.equal(foreign.articleId, 99);
  assert.equal(foreign.contextId, 12);
  assert.equal(foreign.replacePath, null);
});

test("resolveRoute keeps the address when the article is gone", () => {
  assert.deepEqual(
    resolveRoute(parseRoute("/feed/12/article/99"), {
      feedExists: true,
      folderExists: true,
      articleExists: false,
    }),
    {
      status: "article-unavailable",
      context: "feed",
      contextId: 12,
      articleId: null,
      replacePath: null,
    },
  );
});

test("resolveRoute sends a missing list or unknown path home", () => {
  const home = {
    status: "replaced-home",
    context: "all",
    contextId: null,
    articleId: null,
    replacePath: "/",
  };
  assert.deepEqual(
    resolveRoute(parseRoute("/feed/12/article/99"), {
      feedExists: false,
      folderExists: true,
      articleExists: true,
    }),
    home,
  );
  assert.deepEqual(
    resolveRoute(parseRoute("/folder/3"), {
      feedExists: true,
      folderExists: false,
      articleExists: true,
    }),
    home,
  );
  assert.deepEqual(resolveRoute(parseRoute("/no-such"), {}), home);
});
