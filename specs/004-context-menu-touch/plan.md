# Implementation Plan: Context Menu Touch and Placement

**Branch**: `004-context-menu-touch` | **Date**: 2026-11-13 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-context-menu-touch/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Make chrome context menus (feed, folder, All feeds, Add, Profile) modestly easier to tap while staying visually dense, and open them from the trigger’s box instead of the click pixel so they no longer jump.

Approach: raise shared menu CSS tokens slightly (toward existing `--touch-target` / tree-row density, not ~44pt roomy rows); change `openContextMenu` and its callers so placement is always `below trigger` with leading-edge align (`align: "start"`) or trailing-edge align (`align: "end"` for Profile); keep viewport clamping and keyboard behavior. Frontend unit tests cover the pure placement math. No backend, schema, or config changes.

## Technical Context

**Language/Version**: Java 25, Quarkus, vanilla ES modules (UI-only change)

**Primary Dependencies**: Existing shared `#context-menu` in the SPA shell. No new runtime dependency, npm package, or Quarkus extension.

**Storage**: N/A — no schema or persistence change

**Testing**: `node --test` via `npm run test:frontend` for menu placement helper(s). Existing `mvn test` stays green; no new `*IT`.

**Target Platform**: The existing self-hosted reader (wide / medium / narrow layouts; mouse and touch)

**Project Type**: Self-hosted web application (Quarkus + static SPA shell)

**Performance Goals**: Opening a menu remains a single layout measure + position write; no pane rebuild.

**Constraints**: Modest density bump only (clarification). Same spacing on all viewports. Trigger-anchored placement; feed/folder/All feeds/Add leading-edge below; Profile trailing-edge below. Constitution IX (HIG): tokens in `style.css`, keyboard, visible focus, named controls, `prefers-reduced-motion`. CSP unchanged. No new public route or config key.

**Scale/Scope**: Shared chrome context menu only. Five open paths (feed, folder, all, add, profile). Token + placement change; actions unchanged.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Result |
|-----------|------|--------|
| I. Self-hosted reader | No tracker or third-party asset. | Pass |
| II. Layered modules | No Java layer change. UI stays ES modules under `META-INF/resources/scripts`. | Pass |
| III. Explicit failures | No new failure path. | Pass |
| IV. Hostile input | No new fetch; menu strings unchanged; visibility via `classList`. | Pass |
| V. Authenticated by default | No new JAX-RS method or public path. | Pass |
| VI. Schema integrity | No migration. | Pass |
| VII. Regression tests | Frontend unit test for placement math / align behavior. No new IT. | Pass |
| VIII. Bounded work | Single menu overlay; update position in place. | Pass |
| IX. Human Interface Guidelines | Modest touch improvement without sparse whitespace; consistent anchors; keyboard and focus preserved. Spec explicitly rejects oversized empty padding. | Pass |

Post-design re-check: pass. No exceptions.

## Project Structure

### Documentation (this feature)

```text
specs/004-context-menu-touch/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── context-menu.md
└── tasks.md             # /speckit-tasks, not this command
```

### Source Code (repository root)

```text
src/main/resources/META-INF/resources/
├── style.css                 # --menu-item-min-height / --menu-item-pad / --menu-pad tokens
└── scripts/
    ├── menu.js               # openContextMenu; trigger-rect placement; align start|end
    ├── feeds.js              # options click: pass trigger, stop passing clientX/Y as position
    └── main.js               # Add + Profile open paths use trigger geometry

src/test/js/
└── menu-placement.test.js    # pure placeContextMenu(rect, menuSize, viewport, align, gap)
```

**Structure Decision**: Stay in the existing Quarkus static shell and shared `#context-menu`. Prefer extracting a small pure placement function from `menu.js` (testable without DOM) over adding a framework or second menu component. Density is token-only in `style.css`.

## Complexity Tracking

No constitution violations. This section is intentionally empty.
