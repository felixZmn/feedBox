# Tasks: Read, Unread, and Saved Articles

**Input**: Design documents from `/specs/001-read-unread-saved/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included. The FeedBox constitution requires a unit test for each behavior change, and a repository or security IT where persistence or auth changes. Strict test-first ordering is not required.

**Organization**: Tasks are grouped by user story so each story can be implemented and checked on its own.

**Status**: Descriptions match the current spec (circle-check, bookmark, bottom line, thin divider). Every task is already implemented.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Paths follow the current Quarkus module and the ES modules under `src/main/resources/META-INF/resources/`. Tests live in `src/test/java/` and `src/test/js/`.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add the Tabler icons this feature draws, in the same style as the icons already in the reader

- [x] T001 Vendor `circle-check.svg`, `circle-check-filled.svg`, `bookmark.svg`, and `bookmark-filled.svg` into `src/main/resources/META-INF/resources/icons/` from Tabler, using `viewBox="0 0 24 24"`, round caps, and black paint so the existing CSS filters recolor them
- [x] T002 Register those four icon paths in the `SHELL` array in `src/main/resources/META-INF/resources/sw.js`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema and row shell that US1, US2, and US3 all build on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 [P] Add `src/main/resources/db/migration/V7__article_saved.sql` with `article.saved BOOLEAN NOT NULL DEFAULT false`, index `article_saved_published_id_idx` on `(published DESC, id DESC) WHERE saved = true AND published IS NOT NULL`, and index `article_unread_published_id_idx` on `(published DESC, id DESC) WHERE read = false AND published IS NOT NULL`. Comment that horizon `id` comparisons stay residual predicates. Do not edit V1–V6. Do not add `saved` to the article upsert column list
- [x] T004 [P] In `createArticleElement` in `src/main/resources/META-INF/resources/scripts/dom.js`, remove `role="button"` from the article row and keep `tabindex="0"`. Order the row as title, then an optional picture, then a bottom line: source on the left and `.article-meta-end` on the right. The row's accessible name includes the title plus "Read" or "Unread". Do not change the title font, weight, or color
- [x] T005 [P] In `registerArticlesDelegatedEvents` in `src/main/resources/META-INF/resources/scripts/articles.js`, do not open the article when the click or Enter/Space target is a `button` inside the row. Enter and Space on the row itself still open the article
- [x] T006 [P] In `src/main/resources/META-INF/resources/style.css`, lay `.article` out as a column. Put the time, circle-check, and bookmark in `.article-meta-end` on the right of the source line. Keep `.article-mark` and `.article-star` icons at 16px on wide and narrow screens, including under the existing `1100px` rule that enlarges other `.icon` images, so the source line does not grow. Show `:focus-visible` and add no animation. Do not add a title font-weight rule for `.unread`. Keep the existing `.articles-list .article:hover`, `.articles-list .article.selected`, and `.articles-list .article.selected:hover` backgrounds so selection and hover stay visible together with the check and the bookmark. Leave header `.item-spacer` buttons at their existing 44px minimum under 1100px

**Checkpoint**: Migration applies, and an article row can host buttons without opening the article

---

## Phase 3: User Story 1 - See which articles are already read (Priority: P1) 🎯 MVP

**Goal**: Every article row shows a blue outline circle-check when unread and the matching grey filled circle-check when read. Both sit on the bottom line, to the right of the time. Activating the outline check marks that article read and does not open it. The title font stays the same.

**Independent Test**: In All Feeds, a folder, and a single feed, titles share one font, unread checks are outline and blue, and read checks are filled and grey. Activating an outline check marks that article read without opening it. Opening another unread article switches its check to filled without a manual refresh.

### Implementation for User Story 1

- [x] T007 [US1] Add `articleMarkState` in `src/main/resources/META-INF/resources/scripts/article-row.js` that maps effective read to `{ src, label, tone }`: unread → `icons/circle-check.svg`, "Mark as read", `f-blue`; read → `icons/circle-check-filled.svg`, "Read", `f-grey`
- [x] T008 [US1] After T007, cover `articleMarkState` in `src/test/js/article-row.test.js` for both states, including `src`, label, and tone
- [x] T009 [US1] Render the circle-check button `.article-mark` inside `.article-meta-end` from `createArticleElement` in `src/main/resources/META-INF/resources/scripts/dom.js`, after the time and before the bookmark slot, using `img.icon` plus the tone class. Update `setArticleReadState` in the same file to swap the icon `src`, tone, and accessible name in place without removing the button or moving it
- [x] T010 [US1] In `src/main/resources/META-INF/resources/scripts/articles.js`, activating the outline check calls the existing `markArticleAsRead` and does not open the article. Activating the filled check does not open the article and does not mark it unread. A failed mark-read restores the outline check and the previous unread count through the existing rollback

**Checkpoint**: User Story 1 works in All Feeds, folders, and feeds with no saved-articles UI

---

## Phase 4: User Story 2 - Return to articles worth keeping (Priority: P2)

**Goal**: A bookmark immediately after the circle-check saves or unsaves an article. Saved articles appear under a Saved sidebar entry and survive leaving the reader. Saving does not change read state. A thin divider separates Saved from the folders.

**Independent Test**: Save an article from the list and from the open article, leave, return, and open Saved. The article is there. Unsave it and open Saved again; it is gone. Its read state never changed because it was saved. Saved sits under All Feeds, with a thin divider before the folders, and has no options menu.

### Implementation for User Story 2

- [x] T011 [P] [US2] Add `saved` as the last component of `src/main/java/de/_0x2b/model/Article.java` and `src/main/java/de/_0x2b/resource/dto/ArticleSummary.java`. Convenience constructors pass `false`. The JSON name is `saved`. `saved` is `NOT NULL DEFAULT false` and independent of `read` and of both horizons
- [x] T012 [US2] In `src/main/java/de/_0x2b/repository/ArticleRepository.java`, map `saved` on list and detail rows. Add idempotent `save` and `unsave` updates that do not write `read`. Return 0 when the row is already in the target state, and distinguish a missing id the way `markRead` does. Add `findSaved` with `saved = true AND published IS NOT NULL`, the existing search tokens, and `ORDER BY published DESC, id DESC LIMIT 25`. Add `countSaved` for that same dated set
- [x] T013 [US2] After T012, in `src/test/java/de/_0x2b/repository/ArticleRepositoryIT.java`, assert a saved article stays saved after `markRead` and `markAllRead`, the saved list contains both a read and an unread saved article in newest-first order, an unsaved article is absent, and `countSaved` equals the dated saved rows
- [x] T014 [US2] In `src/main/java/de/_0x2b/service/ArticleService.java`, add `save`, `unsave`, `findSaved`, and `countSaved`. Missing id throws `NotFoundException`. SQL failure throws `DataAccessException`. `save` does not mark the article read
- [x] T015 [US2] In `src/main/java/de/_0x2b/resource/ArticleResource.java`, accept `view=saved` on `GET /api/article` and reject it with `400` when `folder` or `feed` is also set. Reject any other `view` with `400`. `view` omitted keeps today's list. Add `POST /api/article/{id}/saved` and `DELETE /api/article/{id}/saved`, both `204` and idempotent, and `GET /api/article/saved/count` returning `{ "count": <int> }`. All three stay `@Authenticated`. Add unit coverage for the `view` rules in `src/test/java/de/_0x2b/resource/ArticleResourceTest.java`. `q` stays trimmed and capped at 100 characters
- [x] T016 [US2] In `src/test/java/de/_0x2b/resource/ArticleResourceSecurityIT.java`, anonymous `POST` and `DELETE` of `/api/article/{id}/saved` and anonymous `GET /api/article/saved/count` return `401`. An authenticated save round-trip returns `saved: true` on the summary and on `GET /api/article/{id}`, and delete returns `saved: false`, without changing `read`. Send `POST` with JSON content type, and pass the search title to `queryParam` without pre-encoding
- [x] T017 [P] [US2] Add `saveArticle`, `unsaveArticle`, and `loadSavedCount` in `src/main/resources/META-INF/resources/scripts/data.js`
- [x] T018 [P] [US2] Document `saved` on the article typedef in `src/main/resources/META-INF/resources/scripts/types.js`
- [x] T019 [US2] Extend `src/main/resources/META-INF/resources/scripts/article-row.js` with `articleStarState`: unsaved uses `icons/bookmark.svg`, "Save article", and `f-grey`; saved uses `icons/bookmark-filled.svg`, "Remove from Saved", and `f-blue`. Cover both in `src/test/js/article-row.test.js`
- [x] T020 [US2] Render the bookmark button `.article-star` immediately after `.article-mark` in `.article-meta-end` from `createArticleElement` in `src/main/resources/META-INF/resources/scripts/dom.js`, and add `setArticleSavedState` to swap the bookmark `src`, tone, and accessible name in place
- [x] T021 [US2] In `src/main/resources/META-INF/resources/scripts/articles.js`, request `view=saved` for the Saved selection, show "No saved articles" when that list is empty and search is off, and keep the existing no-match message while searching. Activating the row bookmark saves or unsaves, does not open the article, does not close it, and does not change the selected article or its read state. Export that toggle so the reader header can call it. Update the bookmark and the saved count immediately; restore both if the request fails. Unsaving a row that is already listed in Saved leaves the row until Saved is opened again or the page is refreshed
- [x] T022 [P] [US2] In `src/main/resources/META-INF/resources/index.html`, add a Saved row `#trigger-show-saved` directly under All Feeds with `icons/bookmark.svg`, the name "Saved", and no options menu. Follow `#saved-count` with an empty `span.tree-options` (`aria-hidden="true"`) so the count lines up with the other sidebar counts, and hide the count at zero. Add a bookmark button `#trigger-save-article` in the reader header, using `.item-spacer`, placed with the existing article actions and defaulting to `icons/bookmark.svg`
- [x] T023 [US2] In `src/main/resources/META-INF/resources/scripts/state.js`, add a Saved selection, build `/saved` and `/saved/article/{id}` from `pathForSelection`, send a Saved refresh without an open article to the article column, and set the document title to `Saved · <app title>` when no article is open
- [x] T024 [US2] Teach `parseRoute` and `buildPath` in `src/main/resources/META-INF/resources/scripts/router.js` to round-trip `/saved` and `/saved/article/{id}`
- [x] T025 [US2] Extend `src/test/js/router.test.js` for those two Saved paths
- [x] T026 [US2] In `src/main/resources/META-INF/resources/scripts/reader.js`, wire `#trigger-save-article` to the toggle from T021. Activating it saves or unsaves, does not close the reader, and does not change the selected article or its read state. Keep the header bookmark's icon, tone, and accessible name matched to the list bookmark. Select Saved from `applyRouteFromLocation`. In `updateNavSelection` in `src/main/resources/META-INF/resources/scripts/dom.js`, highlight `#trigger-show-saved` for the Saved selection with the same selected appearance as All Feeds, and do not leave a feed or folder selected
- [x] T027 [US2] In `src/main/resources/META-INF/resources/scripts/main.js`, choosing Saved loads that list the same way All Feeds does and clears search. In `src/main/resources/META-INF/resources/scripts/feeds.js`, load the saved count with the feed list and update the existing Saved badge in place, hiding it at zero
- [x] T028 [US2] Accept `/saved` and `/saved/article/{id}` in `src/main/java/de/_0x2b/web/SpaFallback.java`. Add `/saved` and `/saved/*` to `quarkus.http.auth.permission.spa.paths` in `src/main/resources/application.properties` with the existing authenticated SPA policy
- [x] T029 [US2] In `src/test/java/de/_0x2b/resource/AuthPathPolicyIT.java`, anonymous `GET /saved` returns `401`
- [x] T030 [P] [US2] Add a saved-count badge helper in `src/main/resources/META-INF/resources/scripts/saved.js` that hides zero and exposes "N saved" when the count is greater than zero. Cover it in `src/test/js/saved.test.js`. In `src/main/resources/META-INF/resources/index.html` and `src/main/resources/META-INF/resources/style.css`, put a `.sidebar-divider` (`role="separator"`, 1px, `var(--border-color)`, no heading) between Saved and `#folder-container`. Leave `.space-top` on Add

**Checkpoint**: Saved works even if Unread is not implemented. The sidebar is All Feeds, Saved, divider, folders, Add

---

## Phase 5: User Story 3 - Scan only what is still unread (Priority: P3)

**Goal**: Unread lists the same dated articles as All Feeds, excluding ones that are effectively read, newest first. The sidebar entry uses the circle-check at the same size as All Feeds and does not repeat the All Feeds count.

**Independent Test**: With a mix of read and unread articles, Unread shows only the unread ones in newest-first order. Leave and come back after reading one; it is absent. Unread does not repeat the All Feeds count and has no options menu.

### Implementation for User Story 3

- [x] T031 [US3] In `src/main/java/de/_0x2b/repository/ArticleRepository.java`, add `findUnread` for dated articles that are not effectively read (`NOT (article.read OR id <= reader_state.mark_read_through_id OR id <= feed.mark_read_through_id)` and `published IS NOT NULL`), with the existing search tokens and `ORDER BY published DESC, id DESC LIMIT 25`
- [x] T032 [US3] Add `findUnread` in `src/main/java/de/_0x2b/service/ArticleService.java`, using the same cursor rules as `getAll`
- [x] T033 [US3] In `src/main/java/de/_0x2b/resource/ArticleResource.java`, accept `view=unread` and reject it with `400` when `folder` or `feed` is also set. Extend the `view` unit tests in `src/test/java/de/_0x2b/resource/ArticleResourceTest.java`
- [x] T034 [US3] After T031, in `src/test/java/de/_0x2b/repository/ArticleRepositoryIT.java`, assert the unread list includes an unread dated article, omits an article with `read = true`, omits an article covered only by the global or feed horizon, and returns newest first
- [x] T035 [US3] Teach `parseRoute` and `buildPath` in `src/main/resources/META-INF/resources/scripts/router.js` to round-trip `/unread` and `/unread/article/{id}`
- [x] T036 [US3] After T035, extend `src/test/js/router.test.js` for those two Unread paths
- [x] T037 [US3] In `src/main/resources/META-INF/resources/scripts/state.js`, add an Unread selection, build its paths from `pathForSelection`, send an Unread refresh without an open article to the article column, and set the document title to `Unread · <app title>` when no article is open
- [x] T038 [P] [US3] In `src/main/resources/META-INF/resources/index.html`, add an Unread row `#trigger-show-unread` with `icons/circle-check.svg` at the same size as the All Feeds icon and the name "Unread", no count, and no options menu. With Saved present the order is All Feeds, Unread, Saved, divider. With Saved absent, Unread sits directly under All Feeds and the divider stays before the folders
- [x] T039 [US3] In `src/main/resources/META-INF/resources/scripts/articles.js`, request `view=unread` for the Unread selection and show "No unread articles" when that list is empty and search is off. A row that becomes read switches to the filled check and stays in the on-screen Unread list. The next load of Unread omits it
- [x] T040 [US3] Select Unread from `applyRouteFromLocation` in `src/main/resources/META-INF/resources/scripts/reader.js`. In `updateNavSelection` in `src/main/resources/META-INF/resources/scripts/dom.js`, highlight `#trigger-show-unread` for the Unread selection with the same selected appearance as All Feeds, and do not leave All Feeds, Saved, a feed, or a folder selected
- [x] T041 [US3] In `src/main/resources/META-INF/resources/scripts/main.js`, choosing Unread loads that list the same way All Feeds does and clears search
- [x] T042 [US3] Accept `/unread` and `/unread/article/{id}` in `src/main/java/de/_0x2b/web/SpaFallback.java`. Add `/unread` and `/unread/*` to `quarkus.http.auth.permission.spa.paths` in `src/main/resources/application.properties`
- [x] T043 [US3] In `src/test/java/de/_0x2b/resource/AuthPathPolicyIT.java`, anonymous `GET /unread` returns `401`

**Checkpoint**: Unread, Saved, and the read check work together. Sidebar order is All Feeds, Unread, Saved, divider, folders, Add

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Keyboard behavior shared by the three sidebar views, then the quickstart checks

- [x] T044 [P] Make the All Feeds, Unread, and Saved rows keyboard operable in `src/main/resources/META-INF/resources/index.html` and `src/main/resources/META-INF/resources/scripts/main.js`: tab stop, visible focus, and Enter/Space select the view. Options-menu clicks on All Feeds still do not select the view
- [x] T045 Run the automated checks in `specs/001-read-unread-saved/quickstart.md` (`mvn test`, `mvn verify -DskipITs=false`, and `npm run test:frontend`) and fix failures in the files those checks point at

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup. Blocks all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational. No dependency on US2 or US3
- **User Story 2 (Phase 4)**: Depends on Foundational. The bookmark is rendered after the US1 check, so finish T009 before T020. Backend tasks T011–T016 can proceed in parallel with US1
- **User Story 3 (Phase 5)**: Depends on Foundational. The unread list UI reuses the US1 check. Sidebar order assumes the US2 Saved row and divider when those exist. The unread query does not require `saved`
- **Polish (Phase 6)**: Depends on the stories being delivered

### User Story Dependencies

- **User Story 1 (P1)**: First shippable slice. Uses the existing mark-read API
- **User Story 2 (P2)**: Adds `saved`. Independently testable once the bottom-line row from US1 is present
- **User Story 3 (P3)**: Adds the unread filter and sidebar row. Independently testable without Saved; with Saved, Unread sits between All Feeds and Saved

### Within Each User Story

- Repository before service, service before resource
- A story's IT and security tests land in that story, after the code they cover
- Frontend wiring follows the helper and the DOM it renders

### Parallel Opportunities

- T003, T004, T005, and T006 touch different files
- T008 starts after T007. T009 also starts after T007 and does not wait on T008
- T013 starts after T012 and can proceed beside T014
- T017, T018, T022, and T030 can start with US2 without waiting for the repository
- T034 starts after T031 and can proceed beside T032
- T036 starts after T035. T035 and T038 can start together
- T044 can start once the three sidebar rows exist

---

## Parallel Example: User Story 2

```bash
# After Foundational, these files do not overlap:
Task: "Add saved to Article.java and ArticleSummary.java"
Task: "Add saveArticle, unsaveArticle, and loadSavedCount in data.js"
Task: "Add the Saved row and reader bookmark in index.html"
Task: "Add savedBadgePresentation in saved.js and saved.test.js"

# After ArticleRepository save/list/count:
Task: "ArticleRepositoryIT saved cases"
Task: "ArticleService save, unsave, findSaved, countSaved"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and Phase 2
2. Complete Phase 3
3. Stop and check the User Story 1 independent test
4. The reader can see and mark read state before Saved or Unread exist

### Incremental Delivery

1. Setup + Foundational
2. User Story 1 → circle-check on the bottom line of every row
3. User Story 2 → bookmark, Saved list, saved count, divider
4. User Story 3 → Unread list between All Feeds and Saved
5. Polish → keyboard on the three sidebar rows, then quickstart commands

### Parallel Team Strategy

1. Finish Setup and Foundational together
2. One person takes the US1 row while another takes the US2 repository, service, and resource
3. Join on T020, when the bookmark is placed after the check
4. Unread query work can start any time after Foundational; the Unread sidebar row lands after the Saved row if both are in progress

---

## Notes

- `[P]` tasks use different files and do not need an unfinished task in the same step. A task that names an earlier task ID is not parallel with that earlier task
- Story labels map to spec.md priorities P1, P2, and P3
- Do not add a dependency, a config key, a public route, or a way to mark a single article unread
- List responses stay summaries. Page size stays 25
- The row check and bookmark stay visible in both states. Color reinforces the outline and filled shapes
- Commit after each task or logical group
