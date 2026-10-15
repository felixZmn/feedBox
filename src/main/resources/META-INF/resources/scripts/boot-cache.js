"use strict";

// Inject renderer-generated cache HTML before deferred modules load.
// Session is an httpOnly cookie — if Quarkus served this page, paint the cache.
(function () {
  try {
    var raw = localStorage.getItem("folder-tree-cache");
    if (!raw) return;
    var data = JSON.parse(raw);
    if (
      !data ||
      data.version !== 3 ||
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
