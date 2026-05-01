"use strict";

const VERSION = "v1::2026-04-30::007";
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
  "/scripts/modal.js",
  "/scripts/nav.js",
  "/scripts/pkce.js",
  "/scripts/types.js",
  "/scripts/util.js",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    (async () => {
      const cache = await caches.open(CACHE_NAME);
      await cache.addAll(CACHE_FILES);
      self.skipWaiting();
    })(),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      const cacheNames = await caches.keys();
      await Promise.all(
        cacheNames.map((name) =>
          name !== CACHE_NAME ? caches.delete(name) : null,
        ),
      );
      self.clients.claim();
    })(),
  );
});

self.addEventListener("fetch", (event) => {
  const url = new URL(event.request.url);
  const isAppFile = CACHE_FILES.includes(url.pathname);

  if (isAppFile) {
    event.respondWith(
      (async () => {
        const cache = await caches.open(CACHE_NAME);
        const cachedResponse = await cache.match(event.request);

        // Start network fetch in the background to update the cache
        const networkFetch = (async () => {
          try {
            const networkResponse = await fetch(event.request);
            cache.put(event.request, networkResponse.clone());
            return networkResponse;
          } catch {
            return cachedResponse || new Response("Offline", { status: 503 });
          }
        })();

        // Return cached response immediately if available, otherwise wait for network
        return cachedResponse || networkFetch;
      })(),
    );
  } else {
    // For API calls, go network only and provide a generic error
    event.respondWith(
      (async () => {
        try {
          return await fetch(event.request);
        } catch {
          return new Response("Network error", { status: 503 });
        }
      })(),
    );
  }
});
