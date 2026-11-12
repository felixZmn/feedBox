# Contract: Page header and reader chrome

The bundled reader is the only client. This contract is UI structure and behavior. No HTTP API changes.

## Shell structure

Inside `#app-shell` (signed-in only):

1. `.page-header` — full-width continuous bar, recessed chrome surface, above the panes
2. `.layout` — existing three panes (`#feeds-list`, `#articles-list`, `#reader`)

The sign-in `#login-panel` does not include `.page-header`.

## Surfaces

| Region | Surface | Notes |
|--------|---------|--------|
| `.page-header` | Recessed chrome | One fill across full width; matches feed sidebar |
| `#feeds-list` | Recessed chrome | Same as header |
| `#articles-list` | Elevated content | Same as reader |
| `#reader` | Elevated content | Same as article list |

Column separation: hairline borders only. No third pane fill. Relationship holds in light and dark, and when panes are hidden (list/reader stay elevated when shown; header stays chrome; feeds-only stays chrome).

## Page header controls

| Control | Role | Notes |
|---------|------|--------|
| Back | `button`, present when back is visible | Names: "Back to feeds" or "Back to articles". Leading side; mark hidden while shown. |
| FeedBox mark | decorative (`icons/package.svg`) | Not a control. Hidden when back is shown or viewport ≤ about 700px. |
| Search | `search` input in header middle | Filters current article list; clear control as today. Sits on chrome surface; no article-list search strip. |
| Refresh feeds | `button` `#trigger-refresh` | Existing refresh behavior and spinner. Trailing group, before Profile. |
| Profile | `button`, `aria-haspopup="menu"`, name "Profile" | Fixed person icon (`icons/profile_placeholder.svg`). Opens chrome menu. |

Removed:

- Feeds column header
- Articles column search strip / back
- `#trigger-header-menu` (More)
- Sticky reader `.column-header` action bar
- `#trigger-back-feeds` / `#trigger-close` as column controls

## Profile menu (`context-chrome`)

Opened from Profile via the existing context menu. Visible items, in order:

1. Import OPML (`#trigger-import`)
2. Export OPML (`#trigger-export`)
3. Hairline separator
4. Keyboard shortcuts (`#trigger-shortcuts-help`)
5. Hairline separator
6. Sign Out (`#trigger-logout`)

Placement: menu trailing edge aligns with Profile’s trailing edge; menu stays fully on screen.

Keyboard: arrow move, Enter/Space activate, Escape closes and returns focus to Profile. Outside activation closes and returns focus to Profile.

Row menus (`context-all`, `context-folder`, `context-feed`, `context-add`) keep their actions; hairline separators inside those menus are presentation only.

## Articles column

- No search bar and no back control in this column.
- Starts with list content (and any in-list banners) on the elevated content surface under the page header.

## Reader column

When no article is open:

- No toolbar
- No article title/body placeholder; column is empty (elevated content surface)

When an article is open:

- In-article stack (scrolls with the article): left-aligned icon toolbar (Save, Open in browser if usable URL, Previous, Next) → headline → source/publisher → date/author → hairline divider → body
- Those four actions appear only here
- No back control in the toolbar

## Back visibility matrix

| Layout | Primary pane | Back | Mark |
|--------|--------------|------|------|
| Wide (≥1101px) | any | Hidden | Shown |
| Medium (701–1100px), feeds+articles | feeds or articles | Hidden | Shown |
| Medium, reader full page | reader | "Back to articles" | Hidden |
| Narrow (≤700px) | feeds | Hidden | Hidden |
| Narrow | articles | "Back to feeds" | Hidden |
| Narrow | reader | "Back to articles" | Hidden |

Destinations match today’s navigation: articles → feeds, reader → articles.

## Unchanged behaviors

- Refresh feeds job start, progress, failure reporting
- Import / export / shortcuts help / sign out outcomes
- Save / open original / previous / next article
- Keyboard shortcuts for refresh, save, previous, next
- Connectivity banner placement (outside the page header)
