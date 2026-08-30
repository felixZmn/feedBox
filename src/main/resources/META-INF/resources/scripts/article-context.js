"use strict";

/**
 * Coordinates replacing the current article filter (folder, feed, all feeds,
 * or search) while preventing stale asynchronous transitions from restoring
 * infinite scrolling.
 *
 * @param {{
 *   pauseObserver: () => void,
 *   resumeObserver: () => void,
 *   clearReader: () => void,
 *   resetPagination: () => void,
 *   clearArticles: () => void,
 *   loadArticles: () => Promise<void>,
 *   showError: (error: Error) => Promise<void>,
 * }} actions
 * @returns {() => Promise<void>}
 */
export function createArticleContextReloader(actions) {
  let contextToken = 0;

  return async function reloadArticlesForCurrentContext() {
    const token = ++contextToken;
    actions.pauseObserver();
    actions.clearReader();
    actions.resetPagination();
    actions.clearArticles();

    try {
      await actions.loadArticles();
    } catch (error) {
      if (token === contextToken) {
        await actions.showError(error);
      }
    } finally {
      if (token === contextToken) {
        actions.resumeObserver();
      }
    }
  };
}
