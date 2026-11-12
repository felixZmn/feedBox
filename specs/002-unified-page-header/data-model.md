# Data Model: Unified Page Header

No database entities. This feature only changes client chrome state already implied by navigation and article selection.

## Surfaces (presentation tokens)

Client CSS surfaces, not persisted data.

| Surface | Members | Hierarchy |
|---------|---------|-----------|
| Chrome | Page header, feed/folder sidebar | Recessed |
| Content | Article list, reader | Elevated |

Rules:

- Page header fill equals sidebar chrome (one continuous chrome bar; not column-segmented).
- Article list fill equals reader content.
- Hairlines separate columns; no third fill.
- Light and dark keep the same recessed-vs-elevated relationship.
- Hiding a pane does not reassign surfaces: list/reader stay elevated when shown; page header stays chrome; feeds-only uses chrome under the header.

## Page header chrome

| Field | Values | Rules |
|-------|--------|--------|
| `backVisible` | boolean | True only when the article list is the only pane, or the reader is the only pane. False when the feed list is visible alone or beside the article list. |
| `backTarget` | `feeds` \| `articles` | `feeds` when primary pane is articles ("Back to feeds"). `articles` when primary pane is reader ("Back to articles"). |
| `markVisible` | boolean | False when `backVisible` or viewport ≤ about 700px; true otherwise. |
| `searchInHeader` | boolean | Always true for this feature: search lives in the page-header middle on the chrome surface. |
| `refreshBusy` | boolean | True while a feed refresh job is in progress. Second activation is ignored. |
| `profileOpen` | boolean | True while the chrome context menu is open from Profile. Escape / outside / item choose closes it and returns focus to Profile when appropriate. |

Derived from existing `NavigationService.currentState.column` and today’s breakpoints (`max-width: 700px` one-pane, `701–1100px` medium, `≥1101px` wide).

## Reader chrome

| Field | Values | Rules |
|-------|--------|--------|
| `articleOpen` | boolean | True when `state.selectedArticle` is set and the reader has rendered that article. |
| `readerToolbarVisible` | boolean | Equals `articleOpen`. When false, the reader column shows no toolbar and no article body. |
| `openInBrowserAvailable` | boolean | True only when `articleOpen` and the article has a usable original URL (same rule as today’s `#trigger-external-open`). |

Open-article stack order: toolbar icons → headline → source/publisher → date/author → divider → body. Toolbar is not sticky.

## Relationships

```text
NavigationService.column ──derives──> back visibility / label / mark visibility
viewport width            ──derives──> mark visibility (narrow)
state.selectedArticle     ──derives──> in-article toolbar + article body
refresh job state         ──derives──> Refresh feeds busy indication
chrome vs content tokens  ──paint──>  header/sidebar vs list/reader
```

## Validation

- Profile menu contents are exactly the four chrome items plus two hairline separators (after Export OPML, after Keyboard shortcuts); no IdP picture URL is stored or displayed.
- Menu is end-aligned to Profile and stays on screen.
- Back is never shown when feeds is primary.
- Reader actions never appear in the page header, feed list, or article list.
- Search never appears as a full-width strip on the article list.
- Clearing the selection sets `articleOpen` false and removes both the toolbar and the article content.

## State transitions

### Page-header back

```text
feeds primary     --narrow: open articles--> articles primary (back → feeds; mark hidden)
articles primary  --open article----------> reader primary (back → articles; mark hidden)
reader primary    --back------------------> articles primary
articles primary  --back------------------> feeds primary
```

Wide layout never enters a back-visible state while feeds remain on screen.

### Reader chrome

```text
empty reader --choose article--> toolbar + article stack visible
toolbar + article --clear / back / close selection--> empty reader
```
