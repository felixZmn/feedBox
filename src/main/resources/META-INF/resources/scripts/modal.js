"use strict";

import { toast } from "./toast.js";

const DEFAULT_CONFIRM_LABEL = "Confirm";

/** Plain text of an HTML fragment, parsed inertly (no loads, no scripts). */
function htmlToText(content) {
  const template = document.createElement("template");
  template.innerHTML = content;
  return template.content.textContent.trim();
}

class ModalService {
  constructor() {
    this.dialog = document.getElementById("generic-modal");
    this.titleEl = document.getElementById("modal-title");
    this.formEl = document.getElementById("modal-form");
    this.bodyEl = document.getElementById("modal-body");
    this.cancelBtn = document.getElementById("btn-cancel");
    this.confirmBtn = document.getElementById("btn-confirm");
    this.closeXBtn = this.dialog.querySelector(".close-modal");
    this._resolve = null;
    this._onValidate = null;
    this._confirmLabel = DEFAULT_CONFIRM_LABEL;
    this._busy = false;
    this._session = 0;

    this.cancelBtn.addEventListener("click", () => this.close(null));
    this.closeXBtn.addEventListener("click", () => this.close(null));

    // The confirm button is the form's default submit button, so Enter in an
    // input submits. Native validation (required, type=url) runs before this.
    this.formEl.addEventListener("submit", (e) => {
      e.preventDefault();
      void this._submit();
    });

    // Escape / native dialog dismiss must resolve the pending promise.
    this.dialog.addEventListener("cancel", (e) => {
      e.preventDefault();
      this.close(null);
    });

    // close by clicking backdrop
    this.dialog.addEventListener("click", (e) => {
      if (e.target === this.dialog) this.close(null);
    });
  }

  /**
   * @param {Object} options
   * @param {string} options.title Title of the modal
   * @param {string} options.content Content HTML string for the modal body
   * @param {string} [options.type="confirm"] - "confirm" (default) or "alert"
   * @param {Function} [options.onValidate] - Async function returning boolean.
   *                                        If false, modal stays open.
   * @param {string} [options.confirmLabel="Confirm"] - Text of the confirm button.
   * @param {boolean} [options.destructive=false] - Style the confirm button as dangerous.
   */
  show({
    title,
    content,
    type = "confirm",
    onValidate = null,
    confirmLabel = DEFAULT_CONFIRM_LABEL,
    destructive = false,
  }) {
    // Never overwrite the body of a dialog the user is working in.
    if (this.dialog.open) {
      if (type === "alert") {
        toast(htmlToText(content));
      } else {
        console.warn("[modal] Ignored a dialog opened over another dialog");
      }
      return Promise.resolve(null);
    }

    this._session += 1;
    this.titleEl.textContent = title;
    this.bodyEl.innerHTML = content;
    this._resolve = null; // Reset resolve
    this._onValidate = onValidate;
    this._confirmLabel = confirmLabel;
    this._busy = false;

    this.confirmBtn.classList.remove("d-none");
    this.confirmBtn.classList.toggle("btn-danger", destructive);
    this.confirmBtn.classList.toggle("btn-primary", !destructive);
    this.cancelBtn.textContent = "Cancel";
    this._renderBusy(null);

    if (type === "alert") {
      this.confirmBtn.classList.add("d-none");
      this.cancelBtn.textContent = "Close";
    }

    this.dialog.showModal();

    return new Promise((resolve) => {
      this._resolve = resolve;
    });
  }

  /**
   * Mark the open dialog as working: confirm is disabled and shows `label`.
   * Call with no label to leave the busy state.
   * @param {string|null} [label]
   */
  setBusy(label = null) {
    if (!this.dialog.open) return;
    this._busy = label != null;
    this._renderBusy(label);
  }

  _renderBusy(label) {
    const busy = label != null;
    this.confirmBtn.disabled = busy;
    this.confirmBtn.textContent = busy ? label : this._confirmLabel;
    this.formEl.setAttribute("aria-busy", String(busy));
  }

  async _submit() {
    if (this._busy) return;
    const session = this._session;
    let data = this._collectFormData();

    if (this._onValidate) {
      this.setBusy("Working…");
      try {
        const isValid = await this._onValidate(data, this.bodyEl);
        if (session !== this._session || !this.dialog.open) return;
        if (!isValid) {
          this.setBusy(null);
          return;
        }
      } catch (error) {
        console.error("Modal validation failed:", error);
        if (session !== this._session || !this.dialog.open) return;
        this.setBusy(null);
        let errorEl = this.bodyEl.querySelector(".dialog-error");
        if (!errorEl) {
          errorEl = document.createElement("div");
          errorEl.className = "dialog-error";
          errorEl.setAttribute("role", "alert");
          this.bodyEl.prepend(errorEl);
        }
        errorEl.textContent =
          "Something went wrong. Please check your connection and try again.";
        return;
      }
      data = this._collectFormData();
    }
    this.close(data);
  }

  close(data) {
    this.dialog.close();
    this._busy = false;
    this._onValidate = null;
    if (this._resolve) {
      this._resolve(data);
      this._resolve = null;
    }
    this.bodyEl.innerHTML = "";
  }

  /**
   * Collect form data from the modal body. Always returns an object
   * (`{ confirmed: true }` when there are no named inputs).
   */
  _collectFormData() {
    const inputs = this.bodyEl.querySelectorAll("input, select, textarea");
    const data = {};

    inputs.forEach((input) => {
      if (!input.name) return;

      if (input.type === "checkbox") {
        data[input.name] = input.checked;
      } else if (input.type === "radio") {
        if (input.checked) {
          data[input.name] = input.value;
        }
      } else {
        data[input.name] = input.value;
      }
    });

    if (Object.keys(data).length === 0) {
      return { confirmed: true };
    }
    return data;
  }
}

export const modal = new ModalService();
