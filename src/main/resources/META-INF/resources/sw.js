"use strict";

// Bump for every frontend release so HTML + modules stay in lockstep.
const VERSION = "2026-07-27-001";
const CACHE = `feedbox-${VERSION}`;
const SHELL = [
  "/",
  "/index.html",
  "/style.css",
  "/manifest.json",
  "/sw.js",
  "/scripts/main.js",
  "/scripts/data.js",
  "/scripts/dom.js",
  "/scripts/dialog.js",
  "/scripts/modal.js",
  "/scripts/nav.js",
  "/scripts/pkce.js",
  "/scripts/util.js",
  "/scripts/types.js",
  "/scripts/boot-cache.js",
  "/scripts/vendor/purify.es.mjs",
  "/icons/maskable_icon_x192.png",
  "/icons/maskable_icon_x512.png",
  "/icons/folder.svg",
  "/icons/folder_open.svg",
  "/icons/package.svg",
  "/icons/feed_add.svg",
  "/icons/refresh.svg",
  "/icons/export.svg",
  "/icons/import.svg",
  "/icons/logout.svg",
  "/icons/search.svg",
  "/icons/nav_back.svg",
  "/icons/external.svg",
  "/icons/reader_previous.svg",
  "/icons/reader_next.svg",
  "/icons/rss.svg",
];

function offline(message = "Offline") {
  return new Response(message, { status: 503 });
}

async function putOk(cache, request, response) {
  if (response.ok) {
    await cache.put(request, response.clone());
  }
}

async function matchShell(cache, request) {
  return (
    (await cache.match(request)) ||
    (await cache.match("/")) ||
    (await cache.match("/index.html"))
  );
}

async function precache(cache) {
  await Promise.allSettled(
    SHELL.map(async (path) => {
      const response = await fetch(path);
      await putOk(cache, path, response);
    }),
  );

  const index = await cache.match("/index.html");
  if (index) {
    await cache.put("/", index.clone());
  }
}

function classify(request, url) {
  if (url.pathname.startsWith("/api/")) {
    return "network-only";
  }
  // Versioned static shell: serve cache immediately, refresh in background.
  if (
    request.mode === "navigate" ||
    url.pathname.endsWith(".html") ||
    url.pathname.endsWith(".css") ||
    url.pathname.endsWith(".js") ||
    url.pathname.endsWith(".mjs") ||
    url.pathname.startsWith("/icons/") ||
    url.pathname === "/manifest.json" ||
    url.pathname === "/sw.js"
  ) {
    return "cache-first";
  }
  return "bypass";
}

async function cacheFirst(request, event) {
  const cache = await caches.open(CACHE);
  const cached = await cache.match(request);

  const update = (async () => {
    try {
      const response = await fetch(request);
      await putOk(cache, request, response);
      const url = new URL(request.url);
      if (
        response.ok &&
        (url.pathname === "/" || url.pathname.endsWith(".html"))
      ) {
        await putOk(cache, "/", response);
        await putOk(cache, "/index.html", response);
      }
      return response;
    } catch {
      return (
        cached ||
        (await matchShell(cache, request)) ||
        offline()
      );
    }
  })();

  if (cached) {
    event.waitUntil(update);
    return cached;
  }

  return update;
}

async function networkOnly(request) {
  try {
    return await fetch(request);
  } catch {
    return offline("Network error");
  }
}

async function onInstall() {
  const cache = await caches.open(CACHE);
  await precache(cache);
  self.skipWaiting();
}

async function onActivate() {
  const names = await caches.keys();
  await Promise.all(
    names
      .filter((name) => name.startsWith("feedbox-") && name !== CACHE)
      .map((name) => caches.delete(name)),
  );
  self.clients.claim();
}

function onFetch(event) {
  const request = event.request;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  const strategy = classify(request, url);
  if (strategy === "bypass") return;

  if (strategy === "cache-first") {
    event.respondWith(cacheFirst(request, event));
  } else if (strategy === "network-only") {
    event.respondWith(networkOnly(request));
  }
}

self.addEventListener("install", (event) => {
  event.waitUntil(onInstall());
});

self.addEventListener("activate", (event) => {
  event.waitUntil(onActivate());
});

self.addEventListener("fetch", onFetch);
