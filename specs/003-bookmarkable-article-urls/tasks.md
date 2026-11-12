# Tasks: Bookmarkable Article URLs

**Input**: Design documents from `/specs/003-bookmarkable-article-urls/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included. plan.md and constitution VII require resolution coverage in `src/test/js/router.test.js` and path-policy coverage in `src/test/java/de/_0x2b/resource/AuthPathPolicyIT.java`.

**Organization**: Tasks are grouped by user story so each story can be implemented and checked on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Quarkus app under `src/main/java/de/_0x2b/` and `src/main/resources/`. Reader modules under `src/main/resources/META-INF/resources/scripts/`. Frontend tests under `src/test/js/`. Path-policy IT under `src/test/java/de/_0x2b/resource/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm the existing extension points. No new package, dependency, or config key.

- [X] T001 Confirm `specs/003-bookmarkable-article-urls/` contains `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/addresses.md`, and `quickstart.md`
- [X] T002 [P] Confirm the work stays in `src/main/resources/META-INF/resources/scripts/router.js`, `src/main/resources/META-INF/resources/scripts/reader.js`, `src/main/java/de/_0x2b/web/SpaFallback.java`, and `src/test/java/de/_0x2b/resource/AuthPathPolicyIT.java`, with no new Maven or npm dependency

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: One pure resolution of an address. Every story applies this result.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Add failing tests in `src/test/js/router.test.js` for `resolveRoute(route, { feedExists, folderExists, articleExists })`. Membership of the article in the list is not an input. Assert these outcomes from `data-model.md`: status `ready` when the list exists and no article is named, or the article exists — named list, named article or none, address unchanged (`replacePath` null); status `article-unavailable` when the list exists and the article id does not — named list, no article, address unchanged; status `replaced-home` when the path is not an address, or the named feed or folder does not exist — All Feeds, no article, `replacePath` `"/"`. Cover `/`, `/article/{id}`, `/unread/article/{id}`, `/saved/article/{id}`, `/feed/{id}/article/{id}`, `/folder/{id}/article/{id}`, a trailing slash, and an unknown path such as `/no-such`
- [X] T004 Implement `resolveRoute` in `src/main/resources/META-INF/resources/scripts/router.js` so the tests in T003 pass. Return `{ status, context, contextId, articleId, replacePath }` using only `ready`, `article-unavailable`, and `replaced-home`. Do not treat "article belongs to this list" as an input

**Checkpoint**: Foundation ready — user story work can begin

---

## Phase 3: User Story 1 - Open an article from its address (Priority: P1) 🎯 MVP

**Goal**: A signed-in reader who opens an address for an existing article sees that article, on the named list, with the address unchanged.

**Independent Test**: While signed in, open `/article/{id}` for an existing article and confirm All Feeds is selected, the article is open, and the address is unchanged. Repeat with `/feed/{id}/article/{id}`, `/folder/{id}/article/{id}`, `/unread/article/{id}`, and `/saved/article/{id}`.

### Tests for User Story 1

Resolution cases for a live article are locked by T003. No further test file for this story.

### Implementation for User Story 1

- [X] T005 [US1] In `src/main/resources/META-INF/resources/scripts/reader.js`, delete `articleBelongsToContext`. When `resolveRoute` is `ready` and `articleId` is set, open that article with `loadArticle`, fetching `GET /api/article/{id}` when it is absent from `state.articles` (including when it is not among the first articles of the list). Opening a `ready` article becomes read; saved state is unchanged
- [X] T006 [US1] In `applyRouteFromLocation` in `src/main/resources/META-INF/resources/scripts/reader.js`, select the list from `resolveRoute` before opening: `all` (All Feeds, including `/article/{id}`), `unread`, `saved`, `feed`, or `folder`. A `ready` result with no `articleId` leaves the reader empty
- [X] T007 [US1] In `src/main/resources/META-INF/resources/scripts/reader.js`, call `updateDocumentTitle` from `src/main/resources/META-INF/resources/scripts/state.js` after a `ready` apply so an open article's tab title includes the article title, a list with no article uses that list's name, and All Feeds with no article uses the product name
- [X] T008 [US1] In `src/main/resources/META-INF/resources/scripts/reader.js`, when the address names an article and status is `ready`, show the reader column via `columnForRoute` so a narrow screen shows the article

**Checkpoint**: User Story 1 is testable on its own — an existing article opens from its address

---

## Phase 4: User Story 2 - Refresh and history keep the current place (Priority: P1)

**Goal**: Refresh, back, and forward stay on a resolved address with no failure dialog. A missing article keeps its address. A missing feed, missing folder, or unrecognized path becomes All Feeds.

**Independent Test**: From All Feeds, open a feed, open an article, then Next. Refresh and confirm the same feed, the same article, the same address, and no failure dialog. Then open a missing article id, a missing feed id, and `/no-such`.

### Tests for User Story 2

`article-unavailable` and `replaced-home` are locked by T003. The path-policy shell check for `/no-such` is T019, after the fallback exists.

### Implementation for User Story 2

- [X] T009 [P] [US2] In `src/main/resources/META-INF/resources/index.html`, add `#reader-unavailable` as a sibling of `.article` inside `#reader .column`, hidden by default, with the exact text `This article is unavailable.` It is not a button
- [X] T010 [P] [US2] In `src/main/resources/META-INF/resources/style.css`, show `#reader-unavailable` with the existing reader type in light and dark, with no new chrome and no new control
- [X] T011 [US2] In `src/main/resources/META-INF/resources/scripts/dom.js`, show `#reader-unavailable` only for status `article-unavailable` and hide it otherwise, setting the sentence with `textContent`. While it is shown, `.article` stays hidden so the toolbar, title, and body are not shown
- [X] T012 [US2] In `src/main/resources/META-INF/resources/scripts/reader.js`, handle status `article-unavailable` by keeping the address, keeping the named list selected, showing the unavailable sentence, and not changing read or saved state. On a narrow screen the reader column is the visible pane
- [X] T013 [US2] In `src/main/resources/META-INF/resources/scripts/reader.js`, handle status `replaced-home` by calling `replaceAppUrl("/", columns.FEEDS)` from `src/main/resources/META-INF/resources/scripts/state.js` (`history.replaceState`, not `pushState`), with no article open, even when the old path also named an article. Back does not return to that address
- [X] T014 [US2] In `src/main/resources/META-INF/resources/scripts/reader.js`, do not call `replaceAppUrl` for status `ready` or `article-unavailable`, so a trailing slash and an unused query string stay on the address. Still call `clearSearchFilter()` so search text is not restored from the address
- [X] T015 [US2] In `onPopState` in `src/main/resources/META-INF/resources/scripts/reader.js`, apply the same three statuses as `applyRouteFromLocation` (`ready`, `article-unavailable`, `replaced-home`)
- [X] T016 [US2] In `navigateArticle` in `src/main/resources/META-INF/resources/scripts/reader.js`, when the open article id is not in `state.articles`, return without changing the article and without a dialog. Leave Previous and Next in the toolbar
- [X] T017 [US2] In `loadApp` in `src/main/resources/META-INF/resources/scripts/main.js`, when `isConnectivityFailure` from `src/main/resources/META-INF/resources/scripts/connectivity.js` is true, call `notifyOffline`, leave the address unchanged, and do not show the modal titled "Something went wrong"
- [X] T018 [US2] In `src/main/java/de/_0x2b/web/SpaFallback.java`, serve `index.html` for an HTML GET that is not a real static file and not under `/api/`, `/q/`, `/start`, `/logout`, or `/oidc`. Do not serve HTML for a missing `.js`, `.css`, or other static asset

**Checkpoint**: Refresh and history match the address, and the three failure outcomes are distinct

---

## Phase 5: User Story 3 - Sign-in returns to the requested article (Priority: P2)

**Goal**: A signed-out reader who opens an article address sees sign-in, not the article, and lands on that article after a successful sign-in.

**Independent Test**: Sign out, open `/article/{id}` for an existing article, sign in, and confirm that article is open. Sign out, open `/`, sign in, and confirm All Feeds with no article.

### Tests for User Story 3

- [X] T019 [P] [US3] In `src/test/java/de/_0x2b/resource/AuthPathPolicyIT.java`, with redirects not followed, assert anonymous GET returns 401 for `/article/1`, `/feed/1`, `/feed/1/article/2`, `/folder/1`, `/folder/1/article/2`, `/unread`, `/unread/article/1`, `/saved`, and `/saved/article/1`. Assert `/` and `/index.html` stay 200, `GET /api/article/1` stays 401, and `/no-such` returns the shell (200) whose body is not an article. Depends on T018

### Implementation for User Story 3

- [X] T020 [P] [US3] In `src/main/resources/application.properties`, keep `quarkus.http.auth.permission.spa.paths` covering `/feed/*`, `/folder/*`, `/article/*`, `/unread`, `/unread/*`, `/saved`, and `/saved/*` with policy `authenticated`. Do not add those paths to `quarkus.http.auth.permission.public.paths`
- [X] T021 [P] [US3] In `src/main/resources/META-INF/resources/scripts/auth.js` and `src/main/resources/META-INF/resources/scripts/main.js`, keep the sign-in return path: it is written when a signed-out reader asks to sign in from an app address, read once when the next page is `/`, then cleared. A later sign-in from `/` with nothing stored opens All Feeds and no article

**Checkpoint**: A bookmark opened while signed out returns to that article after sign-in, and the article body is not public

---

## Phase 6: User Story 4 - The same address works for another signed-in person (Priority: P3)

**Goal**: The address copied from the browser opens the same article for anyone who can sign in. This story adds no share control.

**Independent Test**: Copy an article address from one signed-in browser into another signed-in browser and confirm the same title and body. Confirm the shell has no new share or copy-link control.

### Implementation for User Story 4

- [X] T022 [P] [US4] In `src/main/resources/META-INF/resources/index.html` and the reader scripts under `src/main/resources/META-INF/resources/scripts/`, do not add a share control or a copy-link control. The reader keeps an address only as the browser location
- [X] T023 [P] [US4] In `src/main/resources/META-INF/resources/sw.js`, keep `shellNavigate` caching the shell only as `/` and `/index.html`, never under the deep-link path, and do not put article HTML in the signed-out shell

**Checkpoint**: A copied address is enough for a second signed-in person, with no new control

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Run the automated checks and the quickstart

- [X] T024 Run `npm run test:frontend` and `mvn test` from the repository root and fix failures in the files this feature touches
- [ ] T025 Walk `specs/003-bookmarkable-article-urls/quickstart.md` checks 1–15

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup — blocks all user stories
- **User Stories (Phase 3+)**: Depend on Foundational. The `/no-such` assertion in T019 needs T018, so that test follows the document fallback
- **Polish (Phase 7)**: Depends on the stories you intend to ship

### User Story Dependencies

- **User Story 1 (P1)**: After Foundational. No dependency on later stories
- **User Story 2 (P1)**: After Foundational. Uses the same `resolveRoute` as US1 and adds the two failure statuses plus history
- **User Story 3 (P2)**: T019 follows T018. T020 and T021 do not otherwise depend on US1
- **User Story 4 (P3)**: After US1, so a copied address already opens an article. No new behavior beyond refusing a share control

### Within Each User Story

- Tests, where this list includes them, are written so they fail before the implementation that satisfies them (T003 before T004). T019 follows T018 because `/no-such` only returns the shell after the fallback exists
- Story work stays inside the files named on the task
- A story checkpoint is reachable without starting the next story

### Parallel Opportunities

- T002 can run with T001
- After T009 and T010, T011 wires the message; T017 and T018 can run beside the markup
- T020 and T021 can run together once Foundational is done. T019 runs once T018 is done, and can run beside T020 and T021
- T022 and T023 can run together

---

## Parallel Example: User Story 2

```bash
# Markup and the document fallback, different files:
Task: "T009 Add #reader-unavailable in src/main/resources/META-INF/resources/index.html"
Task: "T010 Style #reader-unavailable in src/main/resources/META-INF/resources/style.css"
Task: "T018 Serve index.html for unrecognized HTML GETs in src/main/java/de/_0x2b/web/SpaFallback.java"

# Then the reader.js status handlers, in order: T012, T013, T014, T015, T016
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and Phase 2
2. Complete Phase 3 (T005–T008)
3. Stop and open `/article/{id}` plus one list-plus-article address while signed in

### Incremental Delivery

1. Setup + Foundational → `resolveRoute` tested
2. User Story 1 → existing articles open from an address
3. User Story 2 → refresh, history, unavailable article, All Feeds fallback
4. User Story 3 → sign-in returns to the address; deep links stay authenticated
5. User Story 4 → no share control; the same address works for a second signed-in person

### Parallel Team Strategy

1. Finish Phase 1 and Phase 2 together
2. Then one person can take US1 (`reader.js` open path) while another prepares the US2 markup (T009, T010) and `SpaFallback` (T018)
3. US3 and US4 start after T018 and US1 respectively

---

## Notes

- [P] tasks use different files and do not depend on an unfinished task in the same phase
- [US1]–[US4] map to the stories in `spec.md`
- Do not add a dependency, a config key, a public article path, or a share control
- Commit after each task or logical group
- Stop at any checkpoint and run that story's independent test
