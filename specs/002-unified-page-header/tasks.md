# Tasks: Unified Page Header

**Input**: Design documents from `/specs/002-unified-page-header/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included — constitution VII / plan require frontend coverage for back matrix and reader chrome (`src/test/js/page-header.test.js`).

**Organization**: Tasks grouped by user story. Checkboxes reflect branch status after the clarified plan (done work `[x]`; remaining surface/polish work `[ ]`).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

UI under `src/main/resources/META-INF/resources/`; frontend tests under `src/test/js/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm shell modules and assets for the page header

- [x] T001 Confirm feature docs exist under `specs/002-unified-page-header/` (plan, research, data-model, contracts, quickstart)
- [x] T002 [P] Confirm Profile and FeedBox mark icons exist at `src/main/resources/META-INF/resources/icons/profile_placeholder.svg` and `icons/package.svg`
- [x] T003 [P] Ensure `src/main/resources/META-INF/resources/scripts/page-header.js` is imported from `nav.js` / `main.js` and listed in `src/main/resources/META-INF/resources/sw.js` precache

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shell-level page header structure and continuous chrome bar

**⚠️ CRITICAL**: User stories assume this structure

- [x] T004 Place continuous `.page-header` above `.layout` inside `#app-shell` in `src/main/resources/META-INF/resources/index.html` (not inside a column)
- [x] T005 Style `.page-header` as a full-width non-scrolling bar with safe-area on `#app-shell` in `src/main/resources/META-INF/resources/style.css`
- [x] T006 Remove feeds and articles column headers (and `#trigger-header-menu`) from `src/main/resources/META-INF/resources/index.html`
- [x] T007 Wire page-header DOM refs in `src/main/resources/META-INF/resources/scripts/state.js`

**Checkpoint**: Foundation ready — story work can proceed

---

## Phase 3: User Story 1 - Find app actions in one page header (Priority: P1) 🎯 MVP

**Goal**: One continuous header with mark (when shown), search middle, Refresh + Profile trailing; Profile menu with separators; no feeds More; two-tone chrome/content surfaces

**Independent Test**: Wide screen — one chrome header matching sidebar; search in middle; Refresh then Profile; Profile menu Import/Export/Shortcuts/Sign Out with separators; feed tree starts under header; list+reader share elevated surface

### Tests for User Story 1

- [x] T008 [P] [US1] Assert Profile label/icon and chrome menu order in `src/test/js/page-header.test.js` (imports from `scripts/page-header.js`)

### Implementation for User Story 1

- [x] T009 [US1] Add leading FeedBox mark, trailing Refresh (`#trigger-refresh`) and Profile (`#trigger-profile`, `aria-label="Profile"`) in `src/main/resources/META-INF/resources/index.html`
- [x] T010 [US1] Move article search form into `.page-header-middle` in `src/main/resources/META-INF/resources/index.html` (no search strip on `#articles-list`)
- [x] T011 [US1] Keep refresh behavior on page-header control via `src/main/resources/META-INF/resources/scripts/feeds.js` and `main.js`
- [x] T012 [US1] Open chrome menu from Profile via `openChromeContextMenu` in `src/main/resources/META-INF/resources/scripts/menu.js` / `main.js`
- [x] T013 [US1] Add hairline separators after Export OPML and after Keyboard shortcuts in `context-chrome` items in `src/main/resources/META-INF/resources/index.html` and separator styles in `style.css`
- [x] T014 [US1] End-align Profile menu to Profile’s trailing edge in `src/main/resources/META-INF/resources/scripts/menu.js`
- [x] T015 [US1] Remap surfaces in `src/main/resources/META-INF/resources/style.css`: page header + `#feeds-list` share recessed chrome (`--surface-sidebar` or `--surface-chrome`); `#articles-list` + `#reader` share elevated content (list token equals reader); light and dark; hairlines only between columns (no third fill)
- [x] T016 [US1] Point `.page-header` background at the recessed chrome token (not elevated reader) in `src/main/resources/META-INF/resources/style.css` so header continues the sidebar

**Checkpoint**: US1 shell + surfaces verifiable per quickstart items 1–4

---

## Phase 4: User Story 2 - Keep article actions with the article (Priority: P1)

**Goal**: Empty reader until selection; in-article toolbar (icons → title → source → date → divider → body) that scrolls with the article

**Independent Test**: Empty reader blank; open article shows toolbar stack; actions absent from page header/other columns; scroll moves toolbar, page header stays

### Tests for User Story 2

- [x] T017 [P] [US2] Assert `readerActionsVisible` equals article-open in `src/test/js/page-header.test.js`

### Implementation for User Story 2

- [x] T018 [US2] Embed `#reader-actions` toolbar at top of `.article` stack in `src/main/resources/META-INF/resources/index.html` (Save, Open in browser, Previous, Next)
- [x] T019 [US2] Style left-aligned in-article toolbar + title/meta/divider spacing (not sticky second header) in `src/main/resources/META-INF/resources/style.css`
- [x] T020 [US2] Show/hide article stack with selection in `src/main/resources/META-INF/resources/scripts/dom.js` / `reader.js` / `page-header.js` (`has-article`)
- [x] T021 [US2] Keep Save / Open / Previous / Next behavior and shortcuts wired through `main.js` / `reader.js` / `shortcuts.js`

**Checkpoint**: US2 independently testable with US1 header present

---

## Phase 5: User Story 3 - Leave a one-pane view from the page header (Priority: P1)

**Goal**: Page-header back per layout matrix; mark hidden with back and on narrow; no back in article toolbar; surfaces unchanged when panes hide

**Independent Test**: Narrow/medium back names and destinations; mark hide rules; wide never shows back; one-pane keeps elevated content + recessed header

### Tests for User Story 3

- [x] T022 [P] [US3] Assert `pageBackState` / `layoutBandFromWidth` matrix in `src/test/js/page-header.test.js` per `contracts/page-header.md`

### Implementation for User Story 3

- [x] T023 [US3] Add `#trigger-page-back` in page-header leading group in `src/main/resources/META-INF/resources/index.html`
- [x] T024 [US3] Implement back visibility/labels in `src/main/resources/META-INF/resources/scripts/page-header.js` and sync from `nav.js`
- [x] T025 [US3] Hide FeedBox mark when back is shown and at `max-width: 700px` in `src/main/resources/META-INF/resources/style.css`
- [x] T026 [US3] Ensure in-article toolbar has no back control in `src/main/resources/META-INF/resources/index.html`
- [x] T027 [US3] Verify one-pane / sidebar-hidden layouts keep recessed header chrome and elevated list/reader fills after T015–T016 in `src/main/resources/META-INF/resources/style.css` (no surface reassignment when panes hide)

**Checkpoint**: All three stories independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validation across stories

- [x] T028 Run `npm run test:frontend` and `mvn test` from repository root; fix regressions
- [x] T029 Walk `specs/002-unified-page-header/quickstart.md` reader checks (surfaces, search in header, in-article scroll, back/mark, light/dark)
- [x] T030 [P] Confirm no new config/Helm/README keys and sign-in screen unchanged (no page header on `#login-panel`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Done
- **Foundational (Phase 2)**: Done — blocks stories historically
- **US1 → US2 → US3**: Structure done; remaining US1 surface tasks (T015–T016) should complete before T027 / quickstart polish
- **Polish**: Depends on remaining unchecked tasks

### User Story Dependencies

- **US1 (P1)**: Surfaces T015–T016 still open
- **US2 (P1)**: Complete; relies on US1 header
- **US3 (P1)**: Back/mark complete; T027 waits on surface remap

### Parallel Opportunities

- T015 and documentation reads can proceed while US2/US3 behavior stays untouched
- T028 automated tests parallel with manual T029 after CSS lands

---

## Parallel Example: Remaining US1 surfaces

```bash
# Single-file surface remap (sequential within style.css):
Task: "T015 Remap chrome vs content tokens in style.css"
Task: "T016 Point .page-header at recessed chrome token in style.css"
```

---

## Implementation Strategy

### MVP / next increment

1. Complete T015–T016 (two-tone surfaces)
2. Complete T027 (one-pane surface check)
3. Run T028–T029 (tests + quickstart)
4. Stop and validate against quickstart surfaces + header search

### Incremental Delivery

1. Setup + Foundational + US1 structure — done
2. US2 in-article toolbar — done
3. US3 back/mark — done
4. Surface polish (T015–T016, T027) — remaining MVP gap
5. Quickstart sign-off

### Parallel Team Strategy

Not required — remaining work is concentrated in `style.css`.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to user story
- Remaining open work is primarily the clarified two-tone surface model
- Do not move search back to the article list
- Do not restore a sticky reader action header
