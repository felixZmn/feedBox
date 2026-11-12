# Quickstart: Unified Page Header

Validation guide for [spec.md](./spec.md). Chrome state is in [data-model.md](./data-model.md). Control and visibility rules are in [contracts/page-header.md](./contracts/page-header.md).

## Prerequisites

- Java 25 and Maven
- Node, for `npm run test:frontend`
- A signed-in reader with at least one feed and several articles
- A viewport you can resize across narrow (≤700px), medium (701–1100px), and wide (≥1101px)

No new configuration key. Do not add a Helm value or a README parameter for this feature.

## Automated checks

From the repository root:

```bash
mvn test
npm run test:frontend
```

Expected:

- Existing unit tests stay green.
- Frontend tests cover page-header back visibility and empty vs open reader chrome (see `src/test/js/page-header.test.js` or the helper module it imports).
- No new Failsafe IT is required for this feature. Optional: `mvn verify -DskipITs=false` still green.

## Reader checks

Run the app (`mvn quarkus:dev`) and sign in.

1. **Continuous header + surfaces.** On a wide screen, one bar sits above all three panes. The header and feed sidebar share one recessed chrome surface. The article list and reader share one elevated content surface (hairline between them, not different fills). Left: FeedBox mark. Middle: search. Right: Refresh feeds, then a person Profile icon (not an IdP photo). No feeds-column header and no ⋯ More button.
2. **Profile.** Open Profile. Menu is end-aligned under Profile. Order is Import OPML, Export OPML, separator, Keyboard shortcuts, separator, Sign Out. Each action still works. Escape returns focus to Profile. Option menus on All Feeds, folders, and feeds are unchanged.
3. **Refresh.** Activate Refresh feeds. Progress shows on that control. A second click during refresh does nothing extra. Failure still reports an error and the control returns to idle.
4. **Search.** Search is in the page-header middle on the chrome surface. The article list has no search strip. Search still filters the current list and can be cleared.
5. **Empty reader.** With nothing selected on a wide screen, the reader column is blank: no toolbar and no "No article selected" chrome.
6. **Open article.** Choose an article. The stack appears as icons → title → source → date → divider → body. Save, Open in browser (when available), Previous, and Next work as today and are absent from the page header and the other columns. Scroll the article: the toolbar scrolls away; the page header stays put.
7. **Clear selection.** Close or clear the article so none is selected. The toolbar and article both leave; the reader is empty again.
8. **Narrow back / mark.** At ≤700px: FeedBox mark is hidden. Feeds has no back; articles shows "Back to feeds"; reader shows "Back to articles" in the page header only (not in the article toolbar). Each back returns one step.
9. **Medium back.** At 701–1100px with feeds+articles side by side, no back; mark shown. Open an article so the reader fills the page: page header shows "Back to articles", mark hidden, and back returns to the list pair. Surfaces stay chrome header + elevated content pane.
10. **Wide back.** At ≥1101px, the page header never shows back; mark is shown.
11. **Keyboard and appearance.** Tab to search, Refresh, Profile, back (when visible), and article toolbar controls. Focus is visible; names match the contract. Light and dark keep the recessed-vs-elevated relationship readable. On a phone, the header clears the system status area.

## Out of scope checks (must not appear)

- Identity-provider profile photo on Profile
- Search field as a strip on the article list
- Sticky second reader action header under the page header
- Article actions in the page header
- Three differently filled pane “boxes” (sidebar / list / reader each unique)
- Sign-in screen gaining the page header
