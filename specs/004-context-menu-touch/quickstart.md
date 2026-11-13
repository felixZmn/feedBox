# Quickstart: Context Menu Touch and Placement

Validation guide for [spec.md](./spec.md). Placement and density rules are in [contracts/context-menu.md](./contracts/context-menu.md). Client state is in [data-model.md](./data-model.md).

## Prerequisites

- Java 25 and Maven
- Node, for `npm run test:frontend`
- A signed-in reader with at least one folder and one feed
- A viewport you can resize; mouse and (if available) touch or a finger-sized pointer

No new configuration key. Do not add a Helm value or a README parameter for this feature.

## Automated checks

From the repository root:

```bash
mvn test
npm run test:frontend
```

Expected:

- Existing unit tests stay green.
- Frontend tests cover menu placement math (start vs end align, below-trigger top, viewport clamping) — see `src/test/js/menu-placement.test.js` (or the helper module it imports).
- No new Failsafe IT is required. Optional: `mvn verify -DskipITs=false` still green.

## Reader checks

Run the app (`mvn quarkus:dev`) and sign in.

1. **Dense but taller rows.** Open a feed options menu. Action rows are slightly taller than before the feature, but the menu still looks compact (no large empty gaps). Profile and folder menus match that density.
2. **Stable feed options.** Open feed options by pressing near the left of the ⋯ control; note position. Close. Open again from the right of the same control. Menu sits just below the control; leading edges line up both times (no horizontal jump).
3. **Stable folder / All feeds.** Repeat for a folder options control and All feeds options — below + leading-edge align, stable across press points.
4. **Add.** Open Add from different points on the Add control. Same below + leading-edge behavior.
5. **Profile.** Open Profile from different points on the Profile control. Menu sits just below; trailing edges line up with Profile both times.
6. **Viewport edge.** Open a menu near the bottom or side of the window so it would overflow. Menu stays fully on screen and as close as possible to the normal below-trigger anchor.
7. **Actions unchanged.** Spot-check feed, folder, and Profile actions (including Import / Export / Shortcuts / Sign Out) still work and appear in today’s order.
8. **Keyboard.** Tab to a feed options control or Profile, open the menu, arrow through items, Escape — focus returns to the trigger. Focus rings remain visible.

## Out of scope checks (must not appear)

- ~44pt roomy / sparse menu rows with large empty padding
- Different menu density on narrow vs wide viewports
- Menus still anchored to the exact click pixel
- Feed tree rows made taller as part of this change
- New or removed menu commands
