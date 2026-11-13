# Research: Context Menu Touch and Placement

## Density: modest bump, not fingertip-minimum rows

**Decision**: Increase shared menu item tokens so actionable rows are modestly taller than today’s `--menu-item-min-height: 1.5rem`, targeting about the existing chrome `--touch-target` (36px) / near `--tree-row-min-height` (2rem)—not ~44pt HIG minimums. Keep `--menu-pad` and inter-item gaps small so the overlay still reads as dense. Same tokens on all viewport sizes.

**Rationale**: Clarification session 2026-11-13 chose a modest bump with limited whitespace. Full fingertip rows would make Profile and feed menus look sparse and contradict the product’s compact chrome. Reusing `--touch-target` (or a menu token set to the same value) keeps one density language with header/tree controls.

**Alternatives considered**: Full ~44×44 display-point rows. Rejected by clarification. Touch-only media-query density. Rejected — same density everywhere. Leaving 1.5rem unchanged. Rejected — fails tap comfort.

## Placement: trigger geometry, not click coordinates

**Decision**: Always position from the trigger element’s `getBoundingClientRect()`. Preferred origin: `top = bottom + small gap` (about 4px, matching Profile today). Horizontal: `align: "start"` → menu `left = trigger.left`; `align: "end"` → menu right edge = `trigger.right`. Clamp into the viewport with the existing edge inset. Ignore `clientX` / `clientY` for position (callers may stop passing them as anchors).

**Rationale**: Spec FR-003–005 and clarification A. Click-pixel anchoring is why feed/folder menus jump. Profile already uses rect + end align; unify the other open paths on the same model with start align.

**Alternatives considered**: Keep client coordinates with only Y from the trigger. Rejected — still horizontal jump. Center under pointer. Rejected — unstable. Flush to a single corner for every menu including Profile. Rejected — Profile stays trailing-edge.

## Caller matrix

**Decision**:

| Opener | Group | Align | Anchor |
|--------|-------|-------|--------|
| Feed options | `context-feed` | start | `.tree-options` |
| Folder options | `context-folder` | start | `.tree-options` |
| All feeds options | `context-all` | start | `.tree-options` |
| Add | `context-add` | start | `#trigger-add` (or current add button) |
| Profile | `context-chrome` | end | Profile button |

`feeds.js` delegated handlers pass the options element as trigger and no longer supply click coordinates as the position source. `main.js` Add path switches from `e.clientX/Y` to trigger-rect start align (same as feed/folder).

**Rationale**: FR-009 and assumptions include All feeds and Add for consistency.

**Alternatives considered**: Fix only feed/folder/Profile. Rejected — Add would still jump. Change Profile to leading-edge. Rejected by clarification.

## Implementation shape in `menu.js`

**Decision**: Extract a pure `placeContextMenu({ triggerRect, menuWidth, menuHeight, viewport, align, gap, edge })` (name flexible) that returns `{ left, top }`. `openContextMenu` measures the open menu, reads the trigger rect, applies the helper, writes `style.left` / `style.top`. Keep group show/hide, focus, and keyboard listeners as they are.

**Rationale**: Constitution VII — placement is pure and unit-testable without a browser. Avoids brittle DOM snapshot tests for the main behavior change.

**Alternatives considered**: Only CSS (`popover` / anchor positioning). Deferred — browser support and current fixed-position menu already work; a pure helper is enough. Rewrite menus as a new component. Rejected — unnecessary scope.

## Testing approach

**Decision**: Add `src/test/js/menu-placement.test.js` covering start vs end align, below-trigger top, and clamping when near bottom/right/left edges. Manual quickstart covers density look and real open paths. No Failsafe IT.

**Rationale**: No persistence/auth/outbound change. Visual density is hard to assert in `node --test`; placement math is not.

**Alternatives considered**: Only manual checks. Rejected by Principle VII. Playwright. Rejected — no harness today.

## Out of scope

**Decision**: Do not change feed tree row height, menu action sets, separators/status content, or article/reader menus outside this chrome pattern.

**Rationale**: Spec assumptions and FR-007.
