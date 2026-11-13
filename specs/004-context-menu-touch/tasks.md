# Tasks: Context Menu Touch and Placement

**Input**: Design documents from `/specs/004-context-menu-touch/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included — constitution VII / plan require frontend coverage for placement math (`src/test/js/menu-placement.test.js`). Density is validated visually via quickstart.

**Organization**: Tasks grouped by user story so each story can be implemented and tested independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

UI under `src/main/resources/META-INF/resources/`; frontend tests under `src/test/js/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm feature docs and touch points for the shared chrome menu

- [x] T001 Confirm feature docs exist under `specs/004-context-menu-touch/` (plan.md, research.md, data-model.md, contracts/context-menu.md, quickstart.md)
- [x] T002 [P] Confirm shared menu entry points in `src/main/resources/META-INF/resources/scripts/menu.js`, `feeds.js`, `main.js`, and tokens `--menu-item-min-height` / `--menu-item-pad` / `--menu-pad` in `src/main/resources/META-INF/resources/style.css`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Pure placement helper and open API shape shared by all stories

**⚠️ CRITICAL**: No user story work that depends on stable anchors should begin until the helper exists and is exported for tests

- [x] T003 Extract and export pure `placeContextMenu({ triggerRect, menuWidth, menuHeight, viewport, align, gap, edge })` returning `{ left, top }` in `src/main/resources/META-INF/resources/scripts/menu.js` (preferred top = `triggerRect.bottom + gap`; `align: "start"` → leading edges; `align: "end"` → trailing edges; then clamp into viewport with `edge`)
- [x] T004 Wire `openContextMenu` in `src/main/resources/META-INF/resources/scripts/menu.js` to measure the open menu, read the trigger’s `getBoundingClientRect()`, call `placeContextMenu`, and set `style.left` / `style.top` (ignore click coordinates as the position source)

**Checkpoint**: Foundation ready — density tokens and caller wiring can proceed

---

## Phase 3: User Story 1 - Tap menu items comfortably (Priority: P1) 🎯 MVP

**Goal**: Modestly taller actionable menu rows that stay visually dense (same density on all viewports)

**Independent Test**: Open feed, folder, and Profile menus — rows slightly taller than pre-change 1.5rem minimum, still compact (no large empty gaps); separators/status not inflated to action height

### Implementation for User Story 1

- [x] T005 [US1] Raise `--menu-item-min-height` in `src/main/resources/META-INF/resources/style.css` from `1.5rem` to about `--touch-target` (36px) or near `--tree-row-min-height` (2rem); keep `--menu-pad` and `--menu-item-pad` compact so the menu stays dense (not ~44pt roomy rows)
- [x] T006 [US1] Confirm `.context-menu-item` uses the updated min-height/pad tokens and that `.context-menu-separator` / `.context-menu-status` keep tighter presentation in `src/main/resources/META-INF/resources/style.css` (do not force separators/status to action min-height)
- [x] T007 [US1] Leave `--tree-row-min-height` and feed tree row sizing unchanged in `src/main/resources/META-INF/resources/style.css`

**Checkpoint**: US1 density verifiable per quickstart item 1 (even if placement still uses old callers until US2)

---

## Phase 4: User Story 2 - Open menus in a stable place (Priority: P1)

**Goal**: All chrome menu openers anchor below the trigger; feed/folder/All feeds/Add leading-edge; Profile trailing-edge; no jump from press point

**Independent Test**: Open the same options control from left vs right of the control — same below-trigger origin; Profile trailing-edge; near-edge clamp keeps menu on screen

### Tests for User Story 2

> Write these tests first; they should fail against pre-helper behavior or incomplete wiring, then pass after T003–T004 and caller updates.

- [x] T008 [P] [US2] Add unit tests for `placeContextMenu` start vs end align, below-trigger top, and left/right/bottom clamping in `src/test/js/menu-placement.test.js` (import from `scripts/menu.js`)

### Implementation for User Story 2

- [x] T009 [US2] Update feed/folder/All feeds options handlers in `src/main/resources/META-INF/resources/scripts/feeds.js` to pass the `.tree-options` trigger into context-menu openers without using `e.clientX` / `e.clientY` as the position anchor
- [x] T010 [US2] Ensure `feedContextMenu`, `folderContextMenu`, and `allFeedsContextMenu` in `src/main/resources/META-INF/resources/scripts/menu.js` open with `align: "start"` (below + leading-edge) via `showContextMenuGroup` / `openContextMenu`
- [x] T011 [US2] Update Add button open path in `src/main/resources/META-INF/resources/scripts/main.js` to use trigger geometry with `align: "start"` (stop anchoring on `e.clientX` / `e.clientY`)
- [x] T012 [US2] Keep Profile open path in `src/main/resources/META-INF/resources/scripts/main.js` / `openChromeContextMenu` on below-trigger `align: "end"` (trailing edges), using the shared `placeContextMenu` path

**Checkpoint**: US2 independently testable via `npm run test:frontend` + quickstart items 2–6

---

## Phase 5: User Story 3 - Keep keyboard and existing actions intact (Priority: P2)

**Goal**: Keyboard open/navigate/activate/dismiss and existing menu actions unchanged after density and placement work

**Independent Test**: Keyboard-only open, arrow, Escape (focus returns to trigger); spot-check feed/folder/Profile action order and behavior

### Implementation for User Story 3

- [x] T013 [US3] Verify `initContextMenuListeners` keyboard handling (ArrowUp/Down, Enter/Space, Escape + focus restore) in `src/main/resources/META-INF/resources/scripts/menu.js` still works after placement changes; fix only if regressed
- [x] T014 [US3] Confirm menu groups and action markup in `src/main/resources/META-INF/resources/index.html` are unchanged (no new/removed commands for `context-feed`, `context-folder`, `context-all`, `context-add`, `context-chrome`)

**Checkpoint**: US3 satisfied; no action-set drift

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validation across stories

- [x] T015 Run `npm run test:frontend` and `mvn test` from repository root; fix any failures from menu changes
- [x] T016 [P] Walk `specs/004-context-menu-touch/quickstart.md` reader checks (density, stable anchors, Add, Profile, viewport edge, keyboard)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS US2 (and recommended before final polish); US1 CSS can proceed in parallel with T003–T004 if needed, but prefer foundation first
- **User Story 1 (Phase 3)**: Density tokens — independent of placement callers; can run after Setup
- **User Story 2 (Phase 4)**: Depends on Foundational (T003–T004)
- **User Story 3 (Phase 5)**: After US1 + US2 changes land (regression check)
- **Polish (Phase 6)**: After desired stories complete

### User Story Dependencies

- **User Story 1 (P1)**: After Setup — no dependency on US2
- **User Story 2 (P1)**: After Foundational — independently testable with placement tests
- **User Story 3 (P2)**: After US1/US2 implementation — verify no keyboard/action regressions

### Within Each User Story

- US2: tests (T008) before or alongside caller wiring; helper (T003–T004) before callers (T009–T012)
- Prefer completing US1 + US2 before treating MVP as done for both density and placement

### Parallel Opportunities

- T001 and T002 in parallel
- T005–T007 (US1 CSS) can run in parallel with T008 (US2 tests) once T003 exists
- T009 and T011 touch different files (`feeds.js` vs `main.js`) and can proceed in parallel after T004
- T015 and T016 after implementation

---

## Parallel Example: User Story 2

```bash
# After T003–T004:
Task: "Add unit tests for placeContextMenu in src/test/js/menu-placement.test.js"
Task: "Update feeds.js options handlers to pass trigger without clientX/Y anchor"
Task: "Update Add path in main.js to align start from trigger geometry"
```

---

## Implementation Strategy

### MVP First (User Story 1 + placement foundation)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational placement helper
3. Complete Phase 3: User Story 1 (density tokens)
4. Complete Phase 4: User Story 2 (stable anchors + tests)
5. **STOP and VALIDATE**: quickstart items 1–6 + `npm run test:frontend`
6. Phase 5–6: keyboard/actions check and full quickstart

### Incremental Delivery

1. Setup + Foundational → helper ready
2. US1 density → visual MVP for tap comfort
3. US2 placement → stable open position MVP
4. US3 + polish → ship confidence

### Parallel Team Strategy

1. Shared: Setup + Foundational
2. Developer A: US1 CSS tokens
3. Developer B: US2 tests + caller wiring
4. Together: US3 verification + quickstart

---

## Notes

- [P] tasks = different files, no dependencies on incomplete work
- Do not enlarge menu rows to ~44pt or add touch-only media-query density
- Do not change feed tree row height or menu action sets
- Commit after each task or logical group
- Suggested next command: `/speckit-implement`
