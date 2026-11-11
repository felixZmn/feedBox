import assert from "node:assert/strict";
import test from "node:test";

import { toast } from "../../main/resources/META-INF/resources/scripts/toast.js";

function fakeElement(tag) {
  const classes = new Set();
  return {
    tag,
    children: [],
    listeners: {},
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
      this.listeners[type] = fn;
    },
  };
}

function installDocument() {
  const region = fakeElement("div");
  globalThis.document = {
    getElementById: (id) => (id === "toast-region" ? region : null),
    createElement: fakeElement,
  };
  return region;
}

test("toast without a region does not throw", () => {
  globalThis.document = { getElementById: () => null };
  assert.doesNotThrow(() => toast("hello", { timeoutMs: 0 }));
});

test("toast appends a message element to the region", () => {
  const region = installDocument();
  toast("Saved", { id: "t-append", timeoutMs: 0 });
  assert.equal(region.children.length, 1);
  assert.equal(region.children[0].children[0].textContent, "Saved");
});

test("same id updates the toast text in place", () => {
  const region = installDocument();
  toast("Importing 1/3", { id: "t-progress", timeoutMs: 0 });
  toast("Importing 2/3", { id: "t-progress", timeoutMs: 0 });
  assert.equal(region.children.length, 1);
  const [message] = region.children[0].children;
  assert.equal(message.textContent, "Importing 2/3");
});

test("action button calls onAction and dismisses the toast", async () => {
  const region = installDocument();
  let called = 0;
  toast("Deleted", {
    id: "t-action",
    action: "Undo",
    onAction: () => called++,
    timeoutMs: 0,
  });
  const el = region.children[0];
  const button = el.children[1];
  assert.equal(button.textContent, "Undo");
  assert.equal(button.type, "button");
  button.listeners.click();
  assert.equal(called, 1);
  assert.equal(el.classList.contains("is-leaving"), true);
  await new Promise((resolve) => setTimeout(resolve, 250));
  assert.equal(el.removed, true);
});
