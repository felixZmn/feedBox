# Implementation Plan: Unified Page Header

**Branch**: `002-unified-page-header` | **Date**: 2026-11-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-unified-page-header/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Finish the unified page-header shell so chrome, search, article actions, and surfaces match the clarified spec.

The signed-in shell keeps one continuous `.page-header` above `.layout`. Leading: optional back, then the FeedBox mark when shown. Middle: article search. Trailing: Refresh feeds, then Profile. Profile opens the chrome menu (Import / Export OPML, Keyboard shortcuts, Sign Out) with hairline separators and end-aligned placement. Feed and article column headers are gone. Article actions live in an in-article toolbar at the top of the open article stack and scroll with the article. Surfaces use a two-tone model: recessed chrome for page header + feed sidebar; elevated content for article list + reader. No backend, schema, or config changes.

Much of the structure already exists on this branch. Remaining work is aligning CSS surfaces to the two-tone model, confirming search stays in the header (not the article list), and keeping mark/back/Profile/reader-stack behavior covered by tests and quickstart checks.

## Technical Context

**Language/Version**: Java 25, Quarkus, vanilla ES modules (UI-only change)

**Primary Dependencies**: Existing Tabler-style SVGs under `icons/`. No new runtime dependency, npm package, or Quarkus extension.

**Storage**: N/A — no schema or persistence change

**Testing**: `node --test` via `npm run test:frontend` for back visibility, reader chrome gating, and surface-related helpers where pure. Existing `mvn test` / `mvn verify -DskipITs=false` stay green; no new `*IT`.

**Target Platform**: The existing self-hosted reader (wide three-pane, medium dual/reader, narrow one-pane)

**Project Type**: Self-hosted web application (Quarkus + static SPA shell)

**Performance Goals**: Header, back, and reader-toolbar visibility update with existing pane/article work; no extra full-pane rebuild for refresh, Profile, or back.

**Constraints**: Two-tone surfaces (chrome vs content), not three pane fills. Page header one continuous chrome fill (not column-segmented). Search in header middle. Profile fixed person icon, no IdP photo. Constitution IX (HIG): tokens in `style.css`, keyboard, visible focus, named controls, `prefers-reduced-motion`, comfortable touch targets. CSP and security headers unchanged. No new public route, config key, or third-party asset host.

**Scale/Scope**: Signed-in shell chrome and reader presentation only. Four Profile actions. One back control. In-article toolbar gated on open article.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Result |
|-----------|------|--------|
| I. Self-hosted reader | No tracker, third-party script, or IdP avatar fetch. Icons stay same-origin under `icons/`. | Pass |
| II. Layered modules | No Java layer change. UI stays ES modules under `META-INF/resources/scripts` loaded by the existing shell. | Pass |
| III. Explicit failures | No new failure path. Refresh, import, export, and sign out keep today’s error handling. | Pass |
| IV. Hostile input | No new fetch. Menu and header strings stay static or existing. Visibility via `classList` / `hidden`. | Pass |
| V. Authenticated by default | No new JAX-RS method or public path. Page header only in signed-in `#app-shell`. | Pass |
| VI. Schema integrity | No migration, list filter, or article delete path. | Pass |
| VII. Regression tests | Frontend unit tests for header/back/reader-chrome helpers. No new IT required. | Pass |
| VIII. Bounded work | No new list or unbounded query. Update back/Profile/reader chrome in place. | Pass |
| IX. Human Interface Guidelines | Continuous chrome; two-tone surfaces; article actions with the open article; existing tokens, focus, names, reduced motion, safe-area insets. | Pass |

Post-design re-check: pass. No exceptions.

## Project Structure

### Documentation (this feature)

```text
specs/002-unified-page-header/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── page-header.md
└── tasks.md             # /speckit-tasks, not this command
```

### Source Code (repository root)

```text
src/main/resources/META-INF/resources/
├── index.html                 # page header (search middle); article stack with reader-actions
├── style.css                  # .page-header; two-tone surfaces; mark hide; reader stack spacing
├── sw.js                      # precache header/profile icons as needed
├── icons/package.svg          # FeedBox mark
├── icons/profile_placeholder.svg  # Profile control
└── scripts/
    ├── page-header.js         # back visibility matrix; has-article chrome
    ├── main.js                # Profile, refresh, back wiring
    ├── state.js               # page-header / Profile / search / reader refs
    ├── menu.js                # openChromeContextMenu; end-align; separators
    ├── nav.js                 # sync page-header back with primary column
    ├── dom.js                 # clear/render reader toggles article stack
    ├── reader.js              # open/clear article with toolbar
    └── feeds.js               # refresh spinner on page-header control

src/test/js/
└── page-header.test.js        # back matrix, empty vs open reader chrome
```

**Structure Decision**: Stay in the existing Quarkus static shell. Chrome lives in `#app-shell` above `.layout`. Do not add a UI framework, backend endpoint, or IdP userinfo surface. Prefer CSS token remapping (`--surface-sidebar` / content tokens) over new structural wrappers for the two-tone model.

## Complexity Tracking

No constitution violations. This section is intentionally empty.
