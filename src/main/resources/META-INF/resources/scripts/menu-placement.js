"use strict";

/**
 * Pure placement for the chrome context menu (trigger geometry, not click pixels).
 *
 * @param {{
 *   triggerRect: { left: number, right: number, top: number, bottom: number },
 *   menuWidth: number,
 *   menuHeight: number,
 *   viewport: { width: number, height: number },
 *   align?: "start" | "end",
 *   gap?: number,
 *   edge?: number,
 * }} params
 * @returns {{ left: number, top: number }}
 */
export function placeContextMenu({
  triggerRect,
  menuWidth,
  menuHeight,
  viewport,
  align = "start",
  gap = 4,
  edge = 8,
}) {
  const preferredTop = triggerRect.bottom + gap;
  const preferredLeft =
    align === "end" ? triggerRect.right - menuWidth : triggerRect.left;

  const maxLeft = Math.max(edge, viewport.width - menuWidth - edge);
  const maxTop = Math.max(edge, viewport.height - menuHeight - edge);

  const left = Math.max(edge, Math.min(preferredLeft, maxLeft));
  const top = Math.max(edge, Math.min(preferredTop, maxTop));

  return { left, top };
}
