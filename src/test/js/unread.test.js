import assert from "node:assert/strict";
import test from "node:test";

import {
  unreadBadgeHtml,
  totalUnreadCount,
} from "../../main/resources/META-INF/resources/scripts/unread.js";

function installStoragePolyfill() {
  const create = () => {
    const map = new Map();
    return {
      getItem: (key) => (map.has(key) ? map.get(key) : null),
      setItem: (key, value) => {
        map.set(key, String(value));
      },
      removeItem: (key) => {
        map.delete(key);
      },
      clear: () => map.clear(),
    };
  };
  globalThis.localStorage = create();
  globalThis.sessionStorage = create();
}

test("folderTreesEqual is false when only unreadCount differs", async () => {
  installStoragePolyfill();
  const { folderTreesEqual, normalizeFeed } = await import(
    "../../main/resources/META-INF/resources/scripts/data.js"
  );

  const baseFeed = {
    id: 1,
    folderId: 10,
    name: "Blog",
    url: "https://example.com",
    feedUrl: "https://example.com/rss",
    lastError: null,
    unreadCount: 3,
  };
  const a = {
    folders: [
      {
        id: 10,
        name: "Blogs",
        color: "f-base",
        feeds: [baseFeed],
      },
    ],
    unfiledFeeds: [],
  };
  const b = {
    folders: [
      {
        id: 10,
        name: "Blogs",
        color: "f-base",
        feeds: [{ ...baseFeed, unreadCount: 0 }],
      },
    ],
    unfiledFeeds: [],
  };

  assert.equal(folderTreesEqual(a, a), true);
  assert.equal(folderTreesEqual(a, b), false);
  assert.equal(
    normalizeFeed({
      id: 1,
      folderId: null,
      name: "x",
      feedUrl: "https://example.com/rss",
    }).unreadCount,
    0,
  );
});

test("unreadBadgeHtml omits zero and missing counts", () => {
  assert.equal(unreadBadgeHtml(0), "");
  assert.equal(unreadBadgeHtml(undefined), "");
  assert.equal(unreadBadgeHtml(null), "");
  assert.match(unreadBadgeHtml(4), /class="tree-unread"/);
  assert.match(unreadBadgeHtml(4), />4</);
});

test("totalUnreadCount sums folder feeds and unfiled feeds", () => {
  const total = totalUnreadCount(
    [
      {
        feeds: [{ unreadCount: 2 }, { unreadCount: 3 }],
      },
    ],
    [{ unreadCount: 1 }, { unreadCount: 0 }],
  );
  assert.equal(total, 6);
});
