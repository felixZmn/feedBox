# Data Model: Context Menu Touch and Placement

No database entities. This feature only changes client presentation of the shared chrome context menu.

## Menu density tokens (presentation)

Client CSS tokens on `:root`, not persisted.

| Token | Role | Rules |
|-------|------|--------|
| `--menu-item-min-height` | Minimum height of an actionable menu row | Modestly above today’s 1.5rem; target about `--touch-target` (36px) or near tree-row density (~2rem). Not ~44pt. Same on all viewports. |
| `--menu-item-pad` | Inner padding of actionable rows | Stay compact; support the min-height without large empty gaps. |
| `--menu-pad` | Padding around the menu list | Stay small so the panel remains dense. |

Rules:

- Actionable items (`.context-menu-item`) use these tokens.
- Separators and status lines keep tighter presentation (not forced to the same min-height as actions).
- Feed tree `--tree-row-min-height` is unchanged.

## Placement model (ephemeral UI state)

Derived when a menu opens; not stored.

| Field | Values | Rules |
|-------|--------|--------|
| `triggerRect` | viewport box of the control that opened the menu | Required; from the trigger element, never from press coordinates. |
| `align` | `start` \| `end` | `start` for feed, folder, All feeds, Add. `end` for Profile. |
| `preferredTop` | number | `triggerRect.bottom + gap` (small gap, ~4px). |
| `preferredLeft` | number | `start`: `triggerRect.left`. `end`: `triggerRect.right - menuWidth`. |
| `left`, `top` | number | After clamping so the menu stays fully in the viewport with a small edge inset. |
| `menuOpen` | boolean | True while `#context-menu` has `is-open`. Only one chrome options menu visible. |
| `menuGroup` | `context-feed` \| `context-folder` \| `context-all` \| `context-add` \| `context-chrome` | Which item set is visible. |

## Entities (from spec)

### Options trigger

| Attribute | Notes |
|-----------|--------|
| Element | Feed/folder/All feeds `.tree-options`, Add button, or Profile button |
| Geometry | Bounding box in the viewport at open time |

### Context menu

| Attribute | Notes |
|-----------|--------|
| Overlay | Fixed-position `#context-menu` |
| Groups | Existing item groups; actions unchanged |
| Density | Shared tokens for actionable rows |

### Menu item

| Attribute | Notes |
|-----------|--------|
| Kind | Action, separator, or status |
| Tap target | Actions get modest min-height; separators/status do not inflate to match |

## Relationships

```text
trigger activation ──shows──> menuGroup + align
triggerRect + align + menu size + viewport ──derives──> left, top
CSS menu tokens ──apply to──> actionable menu items
```

## State transitions

```text
closed ──open(group, trigger, align)──> open (positioned, first item focused)
open   ──Escape / outside / choose item──> closed (focus rules unchanged)
```

Re-opening the same trigger with a different press point inside the control MUST yield the same `preferredLeft` / `preferredTop` before clamp (SC-002).
