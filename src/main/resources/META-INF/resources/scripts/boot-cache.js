"use strict";

// Inject renderer-generated cache HTML before deferred modules load.
// Keep session key names in sync with pkce.js / data.js.
(function () {
  try {
    var params = new URLSearchParams(window.location.search);
    if (
      params.has("code") ||
      params.has("state") ||
      params.has("session_state")
    ) {
      return;
    }
    var access = sessionStorage.getItem("access_token");
    var expiresAt = Number(sessionStorage.getItem("expires_at")) || 0;
    var hasSession =
      (access && expiresAt > Date.now()) ||
      !!localStorage.getItem("refresh_token");
    if (!hasSession) return;

    var raw = localStorage.getItem("folder-tree-cache");
    if (!raw) return;
    var data = JSON.parse(raw);
    if (
      !data ||
      data.version !== 1 ||
      typeof data.html !== "string" ||
      !Array.isArray(data.folders) ||
      !Array.isArray(data.unfiledFeeds)
    ) {
      return;
    }

    var container = document.getElementById("folder-container");
    if (!container) return;
    container.innerHTML = data.html;
    container.dataset.cachePainted = "1";
    document.getElementById("feeds-loading")?.classList.add("d-none");
  } catch (e) {}
})();
