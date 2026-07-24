"use strict";

// Bump when shell assets change (index.html, style.css, manifest, app icons).
// Script/icon updates use stale-while-revalidate and do not require a bump.
const VERSION = "2026-07-24-005";
const CACHE = `feedbox-${VERSION}`;
const SHELL = [
  "/index.html",
  "/style.css",
  "/manifest.json",
  "/icons/maskable_icon_x192.png",
  "/icons/maskable_icon_x512.png",
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
  if (request.mode === "navigate" || url.pathname.endsWith(".html") || url.pathname.endsWith(".css")) {
    return "network-first";
  }
  if (url.pathname.startsWith("/api/")) {
    return "network-only";
  }
  if (
    url.pathname.startsWith("/scripts/") ||
    url.pathname.startsWith("/icons/") ||
    url.pathname === "/manifest.json"
  ) {
    return "stale-while-revalidate";
  }
  return "bypass";
}

async function networkFirst(request) {
  const cache = await caches.open(CACHE);
  const url = new URL(request.url);

  try {
    const response = await fetch(request);
    await putOk(cache, request, response);

    if (response.ok && (url.pathname === "/" || url.pathname.endsWith(".html"))) {
      await putOk(cache, "/", response);
      await putOk(cache, "/index.html", response);
    }

    return response;
  } catch {
    return (await matchShell(cache, request)) || offline();
  }
}

async function staleWhileRevalidate(request, event) {
  const cache = await caches.open(CACHE);
  const cached = await cache.match(request);

  const update = (async () => {
    try {
      const response = await fetch(request);
      await putOk(cache, request, response);
      return response;
    } catch {
      return cached || offline();
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

  if (strategy === "network-first") {
    event.respondWith(networkFirst(request));
  } else if (strategy === "stale-while-revalidate") {
    event.respondWith(staleWhileRevalidate(request, event));
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
