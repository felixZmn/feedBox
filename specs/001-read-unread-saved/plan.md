# Implementation Plan: Read, Unread, and Saved Articles

**Branch**: `001-read-unread-saved` | **Date**: 2026-10-17 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-read-unread-saved/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Show read state on every article row with a filled or unfilled mark, let that mark mark the article read, and add a star that saves an article for later. The sidebar gains Unread and Saved above the folders, separated by the same spacing already used above Add.

Read state already exists (`article.read` plus the global and per-feed read horizons). This feature does not add a way to mark one article unread. Saved is a new boolean on `article`, independent of read state. Unread and Saved are filters on the existing paginated article list. Icons are Tabler SVGs, vendored like the icons already in the reader.

## Technical Context

**Language/Version**: Java 25, Quarkus, vanilla ES modules

**Primary Dependencies**: Quarkus REST, Agroal, Flyway, PostgreSQL, existing Tabler icon SVGs. No new runtime dependency.

**Storage**: PostgreSQL. New Flyway migration `V7__article_saved.sql` adds `article.saved` and two partial indexes. Existing `ON DELETE CASCADE` from feed to article removes saved rows with the feed.

**Testing**: JUnit unit tests, Failsafe `*IT` against Dev Services PostgreSQL, `node --test` via `npm run test:frontend`

**Target Platform**: The existing self-hosted reader (desktop and the current narrow layout)

**Performance Goals**: A visible row switches to the read appearance, and the All Feeds unread count updates, within 1 second of the mark-read call succeeding. Saved and unread lists stay on the existing page of 25.

**Constraints**: Title font does not change for read state. The status mark and star stay in fixed positions. Lists stay paginated summaries. New JAX-RS methods stay `@Authenticated`. No new config key, public route, or third-party call. UI reuses `style.css` tokens, `item-spacer` hit area, `html` escaping, and `classList`.

**Scale/Scope**: One signed-in reader. One saved flag per article. Two new sidebar rows and two icons on each article row.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Result |
|-----------|------|--------|
| I. Self-hosted reader | No new outbound call, tracker, or third-party asset host. Icons are copied into `icons/`. | Pass |
| II. Layered modules | HTTP stays in `ArticleResource`. Behavior stays in `ArticleService`. SQL stays in `ArticleRepository`. Models stay records. UI stays ES modules under `META-INF/resources/scripts`. | Pass |
| III. Explicit failures | Missing article → `NotFoundException` (404). Illegal `view` / `folder` / `feed` combination → `BadRequestException` (400). SQL failure → `DataAccessException`. | Pass |
| IV. Hostile input | No new fetch. Search stays capped at 100 characters. New UI strings use the `html` tag. Icon visibility uses `classList` or `img` `src` swaps of same-origin SVGs. | Pass |
| V. Authenticated by default | New article methods are `@Authenticated`. `/unread` and `/saved` join the existing authenticated SPA paths. `/`, `/index.html`, and `/icons/*` stay as they are. | Pass |
| VI. Schema integrity | `V7` is append-only. Both new list filters get partial indexes. List responses stay `ArticleSummary` pages of 25. Full HTML stays on `GET /api/article/{id}`. No article delete path is added. | Pass |
| VII. Regression tests | Repository IT covers saved and unread filters. Security IT covers 401 on the new methods. Router and badge behavior get `node --test`. | Pass |
| VIII. Bounded work | Page size stays 25. The saved count is one indexed count, not a full scan returned to the client. Row and badge updates happen on the existing nodes. | Pass |
| IX. Human Interface Guidelines | Same type, accent, radii, light and dark tokens. Keyboard, visible focus, accessible names. No new animation. Hit area matches `.item-spacer`, including the 44px minimum already applied under 1100px. | Pass |

Post-design re-check: pass. No exceptions.

## Project Structure

### Documentation (this feature)

```text
specs/001-read-unread-saved/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── article-views.md
└── tasks.md             # /speckit-tasks, not this command
```

### Source Code (repository root)

```text
src/main/java/de/_0x2b/
├── model/Article.java                          # add saved
├── repository/ArticleRepository.java           # saved flag, view filters, count
├── resource/ArticleResource.java               # view query, save, unsave, count
├── resource/dto/ArticleSummary.java            # add saved
├── service/ArticleService.java                 # save, unsave, view lists, count
└── web/SpaFallback.java                        # /unread and /saved

src/main/resources/
├── application.properties                      # authenticated SPA paths
├── db/migration/V7__article_saved.sql
└── META-INF/resources/
    ├── index.html                              # Unread, Saved, reader star
    ├── style.css                               # flag cluster, sidebar gap, no title-weight rule
    ├── sw.js                                   # precache the four new SVGs
    ├── icons/circle.svg
    ├── icons/circle-filled.svg
    ├── icons/star.svg
    ├── icons/star-filled.svg
    └── scripts/
        ├── articles.js                         # view params, mark, save, empty copy
        ├── data.js                             # save, unsave, saved count
        ├── dom.js                              # row flags, nav selection, badges
        ├── main.js                             # sidebar activation
        ├── reader.js                           # header star, route apply
        ├── router.js                           # /unread and /saved
        ├── state.js                            # item types, paths, titles
        └── types.js                            # saved on Article

src/test/java/de/_0x2b/
├── repository/ArticleRepositoryIT.java
└── resource/ArticleResourceSecurityIT.java
└── resource/AuthPathPolicyIT.java

src/test/js/
├── router.test.js
└── saved.test.js                               # count badge, alongside unread.test.js
```

**Structure Decision**: Stay inside the current Quarkus module and the ES modules the shell already loads. Do not add a frontend package, a second service, or a saved-articles table.

## Complexity Tracking

No constitution violations. This section is intentionally empty.
