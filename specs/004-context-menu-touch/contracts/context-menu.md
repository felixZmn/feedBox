# Contract: Chrome context menu density and placement

The bundled reader is the only client. This contract is UI behavior for the shared `#context-menu`. No HTTP API changes.

## Scope

Applies to:

| Trigger | Menu group | Horizontal align |
|---------|------------|------------------|
| Feed row options (`.tree-options`) | `context-feed` | start (leading edges) |
| Folder options (`.tree-options`) | `context-folder` | start |
| All feeds options | `context-all` | start |
| Add control | `context-add` | start |
| Profile | `context-chrome` | end (trailing edges) |

Out of scope: article-list / reader menus outside this chrome pattern; feed tree row height; adding or removing menu actions.

## Density

- Actionable rows use shared tokens so each row is modestly taller than the pre-feature 1.5rem minimum.
- Target density: about the existing chrome touch target (~36px) / near tree-row height — still dense, no large empty gaps between labels.
- Same density on all viewport widths (no touch-only override).
- Separators and status lines stay compact; they are not inflated to match action-row height.

## Placement

1. Position from the trigger’s viewport rectangle only. Do not use click/press coordinates as the anchor.
2. Vertical preference: menu top just below the trigger (`trigger.bottom +` small gap, ~4px).
3. Horizontal:
   - **start**: menu leading edge aligns with trigger leading edge.
   - **end**: menu trailing edge aligns with trigger trailing edge.
4. After computing the preferred origin, clamp so the full menu stays inside the viewport with a small edge inset (existing ~8px behavior).
5. Reopening the same trigger from different points inside it MUST produce the same preferred origin (before clamp).

## Actions and keyboard

- Visible actions, separators, and status content for each group remain as today (labels such as dynamic “Mark N as read” still update as today).
- Keyboard: ArrowUp/ArrowDown move among actionable items; Enter/Space activate; Escape closes and returns focus to the trigger.
- Outside activation closes and returns focus to the trigger (existing rules for item choose vs restore focus unchanged).

## Visual / a11y

- Focus-visible styles on menu items remain.
- `prefers-reduced-motion`: existing menu open animation suppression stays.
- Named controls on triggers unchanged.
