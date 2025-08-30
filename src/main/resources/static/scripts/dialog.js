export function hideDialog() {
  document.getElementById("modal").style.display = "none";
  document.getElementById("folder-add-edit").style.display = "none";
  document.getElementById("feed-add-edit").style.display = "none";
  document.getElementById("confirm-dialog").style.display = "none";
}

export function showConfirmDialog(
  headline,
  message,
  confirmAction,
  cancelAction
) {
  document.getElementById("modal-headline").textContent = headline;
  document.getElementById("confirm-message").textContent = message;
  document.getElementById("confirm-dialog").style.display = "grid";
  var yesButton = document.getElementById("confirm-yes");
  yesButton = removeAllEventListeners(yesButton);
  yesButton.addEventListener("click", () => {
    if (confirmAction) confirmAction();
    hideDialog();
  });
  var noButton = document.getElementById("confirm-no");
  noButton = removeAllEventListeners(noButton);
  noButton.addEventListener("click", () => {
    if (cancelAction) cancelAction();
    hideDialog();
  });
  document.getElementById("modal").style.display = "block";
}

export function showAddFolderDialog(confirmAction, cancelAction) {
  folderDialog("Add Folder", confirmAction, cancelAction);
}

export function showEditFolderDialog(folder, confirmAction, cancelAction) {
  document.getElementById("folder-name").value = folder.name;
  document.getElementById("folder-color").value = folder.color;
  folderDialog("Edit Folder", confirmAction, cancelAction);
}

function folderDialog(headline, confirmAction, cancelAction) {
  document.getElementById("modal-headline").textContent = headline;
  document.getElementById("folder-add-edit").style.display = "grid";
  var saveButton = document.getElementById("trigger-save-folder");
  saveButton = removeAllEventListeners(saveButton);
  saveButton.addEventListener("click", () => {
    if (confirmAction) confirmAction();
    hideDialog();
  });
  document.getElementById("modal").style.display = "block";
}

export function showAddFeedDialog(confirmAction, cancelAction) {
  feedDialog("Add Feed", confirmAction, cancelAction);
}

export function showEditFeedDialog(feed, confirmAction, cancelAction) {
  document.getElementById("feed-url").value = feed.feedUrl;
  document.getElementById("feed-folder").value = feed.folderId;
  feedDialog("Edit Feed", confirmAction, cancelAction);
}

function feedDialog(headline, confirmAction, cancelAction) {
  document.getElementById("modal-headline").textContent = headline;
  document.getElementById("feed-add-edit").style.display = "grid";
  var saveButton = document.getElementById("trigger-save-feed");
  saveButton = removeAllEventListeners(saveButton);
  saveButton.addEventListener("click", () => {
    if (confirmAction) confirmAction();
    hideDialog();
  });
  document.getElementById("modal").style.display = "block";
}

/**
 * Remove all event listeners from a DOM element
 * by replacing it with a deep clone.
 * @param {HTMLElement} el - The element to clean
 * @returns {HTMLElement} - The cleaned clone
 */
function removeAllEventListeners(el) {
  const newEl = el.cloneNode(true);
  el.parentNode.replaceChild(newEl, el);
  return newEl;
}
