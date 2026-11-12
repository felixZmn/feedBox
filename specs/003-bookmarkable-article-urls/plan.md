# Implementation Plan: Bookmarkable Article URLs

**Branch**: `003-bookmarkable-article-urls` | **Date**: 2026-11-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-bookmarkable-article-urls/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Make the browser address a reliable way to open a list and an article, and make refresh stay there without a failure dialog.

The path grammar already exists (`/`, `/article/{id}`, `/unread`, `/saved`, `/feed/{id}`, `/folder/{id}`, plus an article under a list). The signed-in reader already writes those paths while navigating. What fails the spec is the membership gate and the failure handling around it: Unread, Saved, and a feed or folder the article does not belong to are treated as a missing article, and the address is rewritten. A missing article should keep its address and show "This article is unavailable." A missing feed or folder, or a path that is not an address, should replace the history entry with All Feeds. Known deep links stay authenticated. No new API, schema, config key, or share control.

## Technical Context

**Language/Version**: Java 25, Quarkus, vanilla ES modules

**Primary Dependencies**: Existing router, history helpers, `GET /api/article/{id}`, Quarkus path policy, and `SpaFallback`. No new runtime dependency.

**Storage**: N/A — no schema or persistence change. Sign-in return keeps the existing session-storage path.

**Testing**: `node --test` via `npm run test:frontend` for path grammar and route resolution. Update `AuthPathPolicyIT` for deep-link auth and the unrecognized HTML shell. No new outbound-HTTP test.

**Target Platform**: The existing self-hosted reader (wide, medium, and narrow)

**Project Type**: Self-hosted web application (Quarkus + static SPA shell)

**Performance Goals**: Opening an addressed article costs the same single-article read as choosing it in the list. No extra page scan when the article is not in the loaded list.

**Constraints**: Known deep links stay authenticated (Principle V). Article HTML stays on `GET /api/article/{id}` only. Unavailable copy is static `textContent`. Refresh of a resolved address does not call `replaceState`. All Feeds fallback uses `replaceState`, not `pushState`. Connectivity failures use the existing offline notice. No share control, public article page, or new config key.

**Scale/Scope**: Client route resolution, one reader message, and the HTML document fallback. Lists, search, save, and open-in-browser stay as they are.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Result |
|-----------|------|--------|
| I. Self-hosted reader | No share endpoint, third-party script, or outbound call carrying article text. Sharing later is a copy of this instance's address. | Pass |
| II. Layered modules | No new resource, service, or repository. Route behavior stays in ES modules. `SpaFallback` stays the existing Vert.x document fallback. | Pass |
| III. Explicit failures | Article 404 stays 404 and becomes the unavailable message, not an empty success and not the init modal. Connectivity uses the existing offline notice. Unexpected init failures can still use today's modal. | Pass |
| IV. Hostile input | No new fetch. The unavailable sentence is app copy via `textContent`. Feed HTML still goes through `sanitizeHTML`. | Pass |
| V. Authenticated by default | `/article/*`, `/feed/*`, `/folder/*`, `/unread`, and `/saved` stay on the authenticated SPA policy. They are not added to the public path list. Anonymous `GET /api/article/{id}` stays 401. | Pass |
| VI. Schema integrity | No migration. Full article HTML remains only on the single-article read. No article delete path. | Pass |
| VII. Regression tests | Pure resolution tests under `src/test/js`. Path-policy IT updated because document fallback and auth paths change. | Pass |
| VIII. Bounded work | One article fetch by id. No scan of later list pages. Address updates stay a history entry, not a pane rebuild. | Pass |
| IX. Human Interface Guidelines | One plain sentence in the reader, not a new control. Existing toolbar, focus, and names stay. Narrow layout shows the article or the message when the address names an article. | Pass |

Post-design re-check: pass. The HTML fallback is limited to document navigations and does not publish article paths or article bodies. No exceptions.

## Project Structure

### Documentation (this feature)

```text
specs/003-bookmarkable-article-urls/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── addresses.md
└── tasks.md             # /speckit-tasks, not this command
```

### Source Code (repository root)

```text
src/main/java/de/_0x2b/web/
└── SpaFallback.java             # HTML document fallback for unrecognized paths

src/main/resources/
├── application.properties       # deep-link paths stay authenticated; no new key
└── META-INF/resources/
    ├── index.html               # reader unavailable sentence, hidden by default
    ├── style.css                # message uses existing reader type; no new chrome
    └── scripts/
        ├── router.js            # parse/build plus resolution status
        ├── reader.js            # apply route without the membership gate; Previous/Next stay a no-op off-list
        ├── state.js             # replace history only for the All Feeds fallback
        ├── dom.js               # show/hide the unavailable sentence
        └── main.js              # connectivity during route apply is not the init modal

src/test/java/de/_0x2b/resource/
└── AuthPathPolicyIT.java        # deep links still require a session; unknown HTML returns the shell

src/test/js/
└── router.test.js               # grammar and resolution table
```

**Structure Decision**: Stay in the existing shell. Resolution is a pure function next to `parseRoute`, applied by `applyRouteFromLocation` and `onPopState`. Do not add a router library, a public article route, or an API.

## Complexity Tracking

No constitution violations. This section is intentionally empty.
