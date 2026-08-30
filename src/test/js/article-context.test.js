import assert from "node:assert/strict";
import test from "node:test";

import { createArticleContextReloader } from "../../main/resources/META-INF/resources/scripts/article-context.js";

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((promiseResolve, promiseReject) => {
    resolve = promiseResolve;
    reject = promiseReject;
  });
  return { promise, resolve, reject };
}

function createActions(loadArticles) {
  const calls = [];
  return {
    calls,
    actions: {
      pauseObserver: () => calls.push("pause"),
      resumeObserver: () => calls.push("resume"),
      clearReader: () => calls.push("clearReader"),
      resetPagination: () => calls.push("resetPagination"),
      clearArticles: () => calls.push("clearArticles"),
      loadArticles,
      showError: async () => calls.push("showError"),
    },
  };
}

test("restores the observer after a failed context load", async () => {
  const { calls, actions } = createActions(async () => {
    throw new Error("network unavailable");
  });
  const reload = createArticleContextReloader(actions);

  await reload();

  assert.deepEqual(calls, [
    "pause",
    "clearReader",
    "resetPagination",
    "clearArticles",
    "showError",
    "resume",
  ]);
});

test("only the newest context switch restores the observer", async () => {
  const firstLoad = deferred();
  const secondLoad = deferred();
  let callCount = 0;
  const { calls, actions } = createActions(() => {
    callCount += 1;
    return callCount === 1 ? firstLoad.promise : secondLoad.promise;
  });
  const reload = createArticleContextReloader(actions);

  const firstSwitch = reload();
  const secondSwitch = reload();
  firstLoad.resolve();
  await firstSwitch;
  assert.equal(calls.filter((call) => call === "resume").length, 0);

  secondLoad.resolve();
  await secondSwitch;
  assert.equal(calls.filter((call) => call === "resume").length, 1);
});

test("suppresses errors from a superseded context switch", async () => {
  const firstLoad = deferred();
  const secondLoad = deferred();
  let callCount = 0;
  const { calls, actions } = createActions(() => {
    callCount += 1;
    return callCount === 1 ? firstLoad.promise : secondLoad.promise;
  });
  const reload = createArticleContextReloader(actions);

  const firstSwitch = reload();
  const secondSwitch = reload();
  firstLoad.reject(new Error("aborted"));
  await firstSwitch;
  assert.equal(calls.includes("showError"), false);

  secondLoad.resolve();
  await secondSwitch;
  assert.equal(calls.filter((call) => call === "resume").length, 1);
});
