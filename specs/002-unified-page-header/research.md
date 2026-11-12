# Research: Unified Page Header

## Page header placement

**Decision**: Keep a single `.page-header` inside `#app-shell`, as a sibling above `.layout`, not inside any column.

**Rationale**: The spec requires one continuous bar above the three panes. Column headers each inherited a pane surface and read as bolted boxes. A shell-level header is real app chrome (FR-001).

**Alternatives considered**: Three joined `.column-header` rows. Rejected — backgrounds still segment. A CSS grid row on `.layout`. Rejected — weaker separation from pane scrolling.

## Two-tone surface model

**Decision**: Recessed chrome surface for page header + feed/folder sidebar. Elevated content surface for article list + reader. Hairlines separate columns; no third pane fill. Same hierarchy in light and dark. When the sidebar is hidden, article list/reader stay elevated and the page header stays recessed chrome. When only feeds are shown, the pane uses recessed chrome under the header.

**Rationale**: Clarification session 2026-11-12. Matching header to sidebar removes the white-on-gray L-seam; matching list to reader removes the middle “slab.” Today’s tokens (`--surface-sidebar`, `--surface-list`, `--surface-reader`) should be remapped so list equals reader (elevated) and header uses the sidebar chrome token (or a shared `--surface-chrome` alias).

**Alternatives considered**: One shared surface for everything. Rejected by the user in favor of chrome vs content. Sidebar-only recessed with header matching content. Rejected — user grouped header with sidebar. Three distinct pane tones. Rejected — reads as boxes screwed together.

## Search in the page header

**Decision**: Keep article search in the page-header middle on the recessed chrome surface. Article list has no search strip. Earlier “search stays on the article list” clarification is superseded.

**Rationale**: User confirmed current location; header background is already the chrome surface, so search needs no separate strip fill (FR-004 / FR-010).

**Alternatives considered**: Search at top of article list with empty header middle. Superseded. Full-width search strip on the content surface. Rejected — recreates stacked boxes.

## Profile control and menu

**Decision**: `#trigger-profile` with `icons/profile_placeholder.svg`, name "Profile", opens `openChromeContextMenu` / `context-chrome`. Order: Import OPML, Export OPML, hairline, Keyboard shortcuts, hairline, Sign Out. Menu trailing edge end-aligned to Profile. No IdP picture.

**Rationale**: Clarifications lock icon, name, separators, and end alignment. Reusing the context menu keeps keyboard behavior.

**Alternatives considered**: ⋯ More named Profile. Rejected. IdP `picture` avatar. Rejected for this feature. Separate dropdown component. Rejected — shared menu already works.

## Refresh placement

**Decision**: `#trigger-refresh` in the page-header trailing group, immediately before Profile. Keep `refreshFeeds()` and spinner toggle unchanged.

**Rationale**: Spec places Refresh outside Profile, left of Profile.

**Alternatives considered**: Refresh inside Profile. Rejected. Duplicate refresh in feeds. Rejected.

## Back control and mark visibility

**Decision**: One page-header back button. Hide when feeds are visible as a peer or alone. Show for articles-only ("Back to feeds") and reader-only ("Back to articles"). Hide the FeedBox mark whenever back is shown and on narrow viewports (about ≤700px). Wire to today’s `navigateTo` targets. No back in the in-article toolbar.

**Rationale**: FR-013–015 and mark clarification. Hiding the mark with back keeps search and trailing actions usable.

**Alternatives considered**: Always show mark with back. Rejected — crowds the leading side. Keep column-local back buttons. Rejected by the spec.

## In-article toolbar (not sticky reader header)

**Decision**: When an article is open, show a left-aligned icon toolbar inside the article stack: Save, Open in browser, Previous, Next, then headline, source/publisher, date/author, hairline divider, body. Toolbar scrolls with the article. Empty reader is blank (no toolbar, no placeholder title).

**Rationale**: Clarification replaced the sticky second reader header with an in-article stack. Page header remains the only sticky app chrome.

**Alternatives considered**: Sticky reader-only action bar under the page header. Rejected in clarification. Always-visible disabled Save row. Rejected earlier.

## Feed list after header removal

**Decision**: No feeds column header. Tree starts under the page header. Row option menus stay on rows.

**Rationale**: FR-007 / FR-008.

**Alternatives considered**: Empty feeds header for alignment. Rejected as wasted chrome.

## Testing approach

**Decision**: Keep/extend `src/test/js/page-header.test.js` for back visibility matrix and empty vs open reader chrome. Manual quickstart covers two-tone surfaces and header search. No new Failsafe IT.

**Rationale**: Constitution VII. No persistence/auth/outbound change.

**Alternatives considered**: Only manual checks. Rejected by Principle VII. Playwright e2e. Rejected — no harness today.

## Safe areas and sticky chrome

**Decision**: Safe-area padding on `#app-shell` so the page header clears the status area. Page header stays non-scrolling; article toolbar scrolls with content; columns scroll under the header.

**Rationale**: FR-002, FR-018.

**Alternatives considered**: Safe-area only on `.layout` with header under the notch. Rejected by FR-018.
