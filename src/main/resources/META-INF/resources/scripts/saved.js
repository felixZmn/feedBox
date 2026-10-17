"use strict";

/**
 * Sidebar badge for the dated saved-article total. Zero hides the badge.
 * @param {number|null|undefined} count
 * @returns {{ hidden: true, text: string, label: null } | { hidden: false, text: string, label: string }}
 */
export function savedBadgePresentation(count) {
  const n = Number(count) || 0;
  if (n <= 0) {
    return { hidden: true, text: "", label: null };
  }
  return { hidden: false, text: String(n), label: `${n} saved` };
}
