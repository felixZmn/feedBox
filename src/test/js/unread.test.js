import assert from "node:assert/strict";
import test from "node:test";

import {
  unreadBadgeHtml,
  totalUnreadCount,
  syncUnreadBadge,
  setFeedUnreadBadge,
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

test("syncUnreadBadge creates updates and removes badge", () => {
  const parent = {
    children: [],
    querySelector(sel) {
      if (sel === ":scope > .tree-unread") {
        return this.children.find((c) => c.className === "tree-unread") ?? null;
      }
      return null;
    },
    appendChild(el) {
      this.children.push(el);
      el.parentNode = this;
      return el;
    },
    insertBefore(el, ref) {
      const idx = this.children.indexOf(ref);
      if (idx >= 0) this.children.splice(idx, 0, el);
      else this.children.push(el);
      el.parentNode = this;
      return el;
    },
  };
  const options = { className: "tree-options", parentNode: parent };
  parent.children.push(options);

  // Minimal Element stub for created badges
  globalThis.document = {
    createElement(tag) {
      return {
        tagName: tag,
        className: "",
        textContent: "",
        attrs: {},
        parentNode: null,
        setAttribute(k, v) {
          this.attrs[k] = v;
        },
        remove() {
          const i = parent.children.indexOf(this);
          if (i >= 0) parent.children.splice(i, 1);
          this.parentNode = null;
        },
      };
    },
  };

  syncUnreadBadge(parent, 3, options);
  const badge = parent.querySelector(":scope > .tree-unread");
  assert.ok(badge);
  assert.equal(badge.textContent, "3");
  assert.equal(badge.attrs["aria-label"], "3 unread");

  syncUnreadBadge(parent, 1, options);
  assert.equal(parent.querySelector(":scope > .tree-unread").textContent, "1");

  syncUnreadBadge(parent, 0, options);
  assert.equal(parent.querySelector(":scope > .tree-unread"), null);
});

test("setFeedUnreadBadge is a no-op without matching DOM", () => {
  globalThis.document = {
    querySelector() {
      return null;
    },
  };
  assert.doesNotThrow(() => setFeedUnreadBadge(99, 2));
});
