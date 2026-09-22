import assert from "node:assert/strict";
import test from "node:test";

import { hasUsableSession } from "../../main/resources/META-INF/resources/scripts/session.js";

const NOW = 1_000_000;

test("hasUsableSession is true with a valid access token", () => {
  assert.equal(
    hasUsableSession({
      accessToken: "access",
      expiresAt: NOW + 60_000,
      refreshToken: null,
      now: NOW,
    }),
    true,
  );
});

test("hasUsableSession is true when access is expired but a refresh token exists", () => {
  assert.equal(
    hasUsableSession({
      accessToken: "access",
      expiresAt: NOW - 1,
      refreshToken: "refresh",
      now: NOW,
    }),
    true,
  );
});

test("hasUsableSession is false when logged out", () => {
  assert.equal(
    hasUsableSession({
      accessToken: null,
      expiresAt: null,
      refreshToken: null,
      now: NOW,
    }),
    false,
  );
  assert.equal(
    hasUsableSession({
      accessToken: "access",
      expiresAt: NOW - 1,
      refreshToken: null,
      now: NOW,
    }),
    false,
  );
});
