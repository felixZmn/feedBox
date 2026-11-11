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
    addEventListener(type, fn) {
      this.listeners[type] = fn;
    },
    querySelector: () => null,
    querySelectorAll: () => [],
  };
}

function installDocument() {
  const els = {};
  for (const id of [
    "modal-title",
    "modal-form",
    "modal-body",
    "btn-cancel",
    "btn-confirm",
    "toast-region",
  ]) {
    els[id] = fakeElement(id.startsWith("btn-") ? "button" : "div");
  }
  const dialog = fakeElement("dialog");
  dialog.open = false;
  dialog.showModal = () => {
    dialog.open = true;
  };
  dialog.close = () => {
    dialog.open = false;
  };
  const closeX = fakeElement("button");
  dialog.querySelector = () => closeX;
  els["generic-modal"] = dialog;

  globalThis.document = {
    getElementById: (id) => els[id] ?? null,
    createElement(tag) {
      if (tag !== "template") return fakeElement(tag);
      const template = { content: { textContent: "" } };
      Object.defineProperty(template, "innerHTML", {
        set(value) {
          template.content.textContent = value.replace(/<[^>]*>/g, "");
        },
      });
      return template;
    },
  };
  return els;
}

const els = installDocument();
const { modal } = await import(
  "../../main/resources/META-INF/resources/scripts/modal.js"
);

test("show sets the confirm label and destructive styling", () => {
  void modal.show({
    title: "Delete",
    content: "<p>Sure?</p>",
    confirmLabel: "Delete feed",
    destructive: true,
  });
  const confirm = els["btn-confirm"];
  assert.equal(confirm.textContent, "Delete feed");
  assert.equal(confirm.classList.contains("btn-danger"), true);
  assert.equal(confirm.classList.contains("btn-primary"), false);
  modal.close(null);
});

test("confirm label defaults to Confirm and is not destructive", () => {
  void modal.show({ title: "Edit", content: "<p>x</p>" });
  const confirm = els["btn-confirm"];
  assert.equal(confirm.textContent, "Confirm");
  assert.equal(confirm.classList.contains("btn-danger"), false);
  assert.equal(confirm.classList.contains("btn-primary"), true);
  modal.close(null);
});

test("setBusy disables confirm with a label and restores it", () => {
  void modal.show({ title: "Add", content: "<p>x</p>", confirmLabel: "Add" });
  const confirm = els["btn-confirm"];
  modal.setBusy("Checking…");
  assert.equal(confirm.disabled, true);
  assert.equal(confirm.textContent, "Checking…");
  modal.setBusy(null);
  assert.equal(confirm.disabled, false);
  assert.equal(confirm.textContent, "Add");
  modal.close(null);
});

test("an alert over an open dialog becomes a toast and keeps the body", async () => {
  mock.timers.enable({ apis: ["setTimeout"] });
  const pending = modal.show({ title: "Edit", content: "<p>form</p>" });
  els["modal-body"].innerHTML = "<input name='a'>";

  const result = await modal.show({
    title: "Error",
    content: "<p>Boom</p>",
    type: "alert",
  });

  assert.equal(result, null);
  assert.equal(els["modal-body"].innerHTML, "<input name='a'>");
  assert.equal(els["generic-modal"].open, true);
  const toastEl = els["toast-region"].children.at(-1);
  assert.equal(toastEl.children[0].textContent, "Boom");

  modal.close(null);
  assert.equal(await pending, null);
  mock.timers.reset();
});
