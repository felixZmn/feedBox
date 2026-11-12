import assert from "node:assert/strict";
import test, { mock } from "node:test";

function fakeElement(tag = "div") {
  const classes = new Set();
  return {
    tag,
    children: [],
    listeners: {},
    textContent: "",
    innerHTML: "",
    disabled: false,
    attributes: {},
    classList: {
      add: (c) => classes.add(c),
      remove: (c) => classes.delete(c),
      contains: (c) => classes.has(c),
      toggle: (c, force) => (force ? classes.add(c) : classes.delete(c)),
    },
    append(...nodes) {
      this.children.push(...nodes);
    },
    replaceChildren() {
      this.children = [];
    },
    remove() {},
    setAttribute(name, value) {
      this.attributes[name] = value;
    },
    getAttribute(name) {
      return this.attributes[name] ?? null;
    },
    removeAttribute(name) {
      delete this.attributes[name];
    },
    addEventListener(type, fn) {
      this.listeners[type] = fn;
    },
    querySelector: () => null,
    querySelectorAll: () => [],
    style: {},
    hidden: false,
    focus() {},
    closest() {
      return null;
    },
  };
}

function installDom() {
  const els = {};
  const dialog = fakeElement("dialog");
  dialog.open = false;
  dialog.showModal = () => {
    dialog.open = true;
  };
  dialog.close = () => {
    dialog.open = false;
  };
  dialog.querySelector = () => fakeElement("button");
  els["generic-modal"] = dialog;
  for (const id of [
    "modal-title",
    "modal-form",
    "modal-body",
    "btn-cancel",
    "btn-confirm",
    "toast-region",
  ]) {
    els[id] = fakeElement();
  }

  const create = () => {
    const map = new Map();
    return {
      getItem: (key) => (map.has(key) ? map.get(key) : null),
      setItem: (key, value) => map.set(key, String(value)),
      removeItem: (key) => map.delete(key),
      clear: () => map.clear(),
    };
  };

  globalThis.document = {
    getElementById: (id) => els[id] ?? fakeElement(),
    createElement: (tag) => fakeElement(tag),
    body: fakeElement("body"),
    documentElement: fakeElement("html"),
    querySelector: () => null,
    querySelectorAll: () => [],
    addEventListener() {},
  };
  globalThis.window = globalThis;
  globalThis.localStorage = create();
  globalThis.sessionStorage = create();
  globalThis.IntersectionObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
  globalThis.history = { pushState() {}, replaceState() {}, state: null };
  globalThis.location = {
    pathname: "/",
    search: "",
    hash: "",
    href: "http://localhost/",
    origin: "http://localhost",
  };
  globalThis.matchMedia = () => ({
    matches: false,
    addEventListener() {},
    removeEventListener() {},
    addListener() {},
    removeListener() {},
  });
  globalThis.CustomEvent = class CustomEvent {
    constructor(type, init) {
      this.type = type;
      Object.assign(this, init || {});
    }
  };
  globalThis.HTMLElement = class {};
  // Avoid nav.js column sync after import (needs fuller button stubs).
  globalThis.requestAnimationFrame = () => 0;
  globalThis.cancelAnimationFrame = () => {};
  globalThis.fetch = async () => ({
    ok: true,
    status: 200,
    headers: { get: () => null },
    text: async () => "[]",
    json: async () => [],
  });
}

installDom();

const { modal } = await import(
  "../../main/resources/META-INF/resources/scripts/modal.js"
);
const { showArticleLoadError } = await import(
  "../../main/resources/META-INF/resources/scripts/articles.js"
);

test("showArticleLoadError skips modal on connectivity failure", async () => {
  const calls = [];
  const show = mock.method(modal, "show", async (opts) => {
    calls.push(opts);
    return null;
  });

  const offline = new Error("Offline");
  offline.code = "OFFLINE";
  await showArticleLoadError(offline);
  await showArticleLoadError(new TypeError("Failed to fetch"));
  await showArticleLoadError(
    new Error("wrapped", { cause: new TypeError("Failed to fetch") }),
  );

  assert.equal(calls.length, 0);
  show.mock.restore();
});

test("showArticleLoadError keeps alert modal for other errors", async () => {
  const calls = [];
  const show = mock.method(modal, "show", async (opts) => {
    calls.push(opts);
    return null;
  });

  await showArticleLoadError(new Error("HTTP 500: boom"));

  assert.equal(calls.length, 1);
  assert.equal(calls[0].title, "Could not load articles");
  assert.equal(calls[0].type, "alert");
  show.mock.restore();
});
