const VERSION = "v1::2025-11-19::002";
const CACHE_NAME = `feedbox-${VERSION}`;

const CACHE_FILES = [
  "/index.html",
  "/style.css",
  "/icons/export.svg",
  "/icons/external.svg",
  "/icons/feed_add.svg",
  "/icons/folder_open.svg",
  "/icons/folder.svg",
  "/icons/import.svg",
  "/icons/maskable_icon_x384.png",
  "/icons/package.svg",
  "/icons/reader_close.svg",
  "/icons/reader_next.svg",
  "/icons/reader_previous.svg",
  "/icons/refresh.svg",
  "/icons/rss.svg",
  "/icons/search.svg",
  "/scripts/data.js",
  "/scripts/dialog.js",
  "/scripts/dom.js",
  "/scripts/main.js",
  "/scripts/nav.js",
  "/scripts/util.js",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(CACHE_NAME)
      .then((cache) => cache.addAll(CACHE_FILES))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((cacheNames) =>
        Promise.all(
          cacheNames.map((name) =>
            name !== CACHE_NAME ? caches.delete(name) : null
          )
        )
      )
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  const isAppFile =
    CACHE_FILES.includes(url.pathname) ||
    CACHE_FILES.includes(`${url.pathname}/`);

  if (isAppFile) {
    event.respondWith(
      caches
        .match(event.request)
        .then((cached) => cached || fetch(event.request))
    );
  } else {
    event.respondWith(fetch(event.request));
  }
});
