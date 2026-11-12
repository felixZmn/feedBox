"use strict";

import { toast } from "./toast.js";

const OFFLINE_HEADER = "X-FeedBox-Offline";

/** @type {(() => void) | null} */
let offlineRetryHandler = null;
/** @type {{ dismiss: () => void } | null} */
let offlineToastHandle = null;
let connectionPending = false;

/**
 * @param {Response | null | undefined} response
 * @returns {boolean}
 */
export function isOfflineResponse(response) {
  return Boolean(response?.headers?.get?.(OFFLINE_HEADER));
}

/**
 * True when the failure is the same connectivity outage that already
 * triggered the offline toast from apiFetch (skip a second blocking modal).
 * @param {unknown} error
 * @returns {boolean}
 */
export function isConnectivityFailure(error) {
  if (!error || typeof error !== "object") return false;
  if (/** @type {{ code?: string }} */ (error).code === "OFFLINE") return true;
  if (error instanceof TypeError) return true;
  const cause = /** @type {{ cause?: unknown }} */ (error).cause;
  return cause instanceof TypeError;
}

/**
 * Register the Retry action for the shared offline toast (boot + mid-session).
 * @param {(() => void) | null} handler
 */
export function onOfflineRetry(handler) {
  offlineRetryHandler = typeof handler === "function" ? handler : null;
}

export function isConnectionPending() {
  return connectionPending;
}

/** Sticky offline toast used for boot probe and apiFetch outages. */
export function notifyOffline() {
  connectionPending = true;
  try {
    offlineToastHandle = toast("Can't reach FeedBox.", {
      id: "offline",
      timeoutMs: 0,
      action: "Retry",
      onAction: () => {
        offlineRetryHandler?.();
      },
    });
  } catch {
    /* no DOM (tests) */
  }
}

export function dismissOfflineToast() {
  connectionPending = false;
  offlineToastHandle?.dismiss();
  offlineToastHandle = null;
}
