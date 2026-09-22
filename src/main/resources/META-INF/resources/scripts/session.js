"use strict";

/**
 * Pure session predicate for tests and runtime gates.
 * True while the access token is still valid, or a refresh token can renew it.
 * @param {{
 *   accessToken?: string|null,
 *   expiresAt?: number|null,
 *   refreshToken?: string|null,
 *   now?: number,
 * }} tokens
 * @returns {boolean}
 */
export function hasUsableSession({
  accessToken = null,
  expiresAt = null,
  refreshToken = null,
  now = Date.now(),
} = {}) {
  const accessValid =
    !!accessToken && expiresAt != null && now < Number(expiresAt);
  return accessValid || !!refreshToken;
}
