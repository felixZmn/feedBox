"use strict";

class ModalService {
  constructor() {
    this.dialog = document.getElementById("generic-modal");
    this.titleEl = document.getElementById("modal-title");
    this.bodyEl = document.getElementById("modal-body");
    this.cancelBtn = document.getElementById("btn-cancel");
    this.confirmBtn = document.getElementById("btn-confirm");
    this.closeXBtn = this.dialog.querySelector(".close-modal");
    this._resolve = null;

    this.cancelBtn.addEventListener("click", () => this.close(null));
    this.closeXBtn.addEventListener("click", () => this.close(null));

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
   */
  show({ title, content, type = "confirm", onValidate = null }) {
    this.titleEl.textContent = title;
    this.bodyEl.innerHTML = content;
    this._resolve = null; // Reset resolve

    this.confirmBtn.classList.remove("d-none");
    this.cancelBtn.textContent = "Cancel";

    if (type === "alert") {
      this.confirmBtn.classList.add("d-none");
      this.cancelBtn.textContent = "Close";
    }

    if (this._confirmHandler) {
      this.confirmBtn.removeEventListener("click", this._confirmHandler);
    }
    this._confirmHandler = async () => {
      let data = this._collectFormData();

      if (onValidate) {
        try {
          const isValid = await onValidate(data, this.bodyEl);
          if (!isValid) return;
        } catch (error) {
          console.error("Modal validation failed:", error);
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
    };
    this.confirmBtn.addEventListener("click", this._confirmHandler);

    this.dialog.showModal();

    return new Promise((resolve) => {
      this._resolve = resolve;
    });
  }

  close(data) {
    this.dialog.close();
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
