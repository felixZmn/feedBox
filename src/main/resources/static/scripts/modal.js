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

    // close by clicking backdrop
    this.dialog.addEventListener("click", (e) => {
      if (e.target === this.dialog) this.close(null);
    });

    this.confirmBtn.addEventListener("click", () => {
      // collect form data
      const data = this._collectFormData();
      this.close(data);
    });
  }

  /**
   * @param {Object} options
   * @param {string} options.title Title of the modal
   * @param {string} options.content Content HTML string for the modal body
   * @param {string} options.type - "confirm" (default) or "alert"
   * @param {Function} options.onValidate - Async function returning boolean.
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

    // Remove old event listeners to prevent duplicates if instance is reused
    const newConfirmBtn = this.confirmBtn.cloneNode(true);
    this.confirmBtn.parentNode.replaceChild(newConfirmBtn, this.confirmBtn);
    this.confirmBtn = newConfirmBtn;

    this.confirmBtn.addEventListener("click", async () => {
      const data = this._collectFormData(); // Collect current state of DOM

      if (onValidate) {
        const isValid = await onValidate(data, this.bodyEl);
        if (!isValid) return; // Keep dialog open
      }
      this.close(this._collectFormData());
    });

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
   * Helper to collect form data from the modal body.
   * Supports input, select, textarea.
   * Returns an object with name-value pairs.
   */
  _collectFormData() {
    const inputs = this.bodyEl.querySelectorAll("input, select, textarea");

    if (inputs.length === 0) return true;

    const data = {};

    inputs.forEach((input) => {
      if (!input.name) return;

      if (input.type === "checkbox") {
        data[input.name] = input.checked; // Return boolean for checkboxes
      } else if (input.type === "radio") {
        // Only include the radio button that is actually selected
        if (input.checked) {
          data[input.name] = input.value;
        }
      } else {
        // Text, url, select-one, textarea, etc.
        data[input.name] = input.value;
      }
    });

    return data;
  }
}

export const modal = new ModalService();
