# Research: Read, Unread, and Saved Articles

## Saved storage

**Decision**: Add `article.saved BOOLEAN NOT NULL DEFAULT false`. Do not add a saved-articles table.

**Rationale**: The reader is single-tenant and an article is either saved or not. A column travels with the article, so deleting a feed still removes saved articles through the existing `article_feed_id_fkey` `ON DELETE CASCADE`. Feed refresh uses an `INSERT ... ON CONFLICT DO UPDATE` that lists columns explicitly and does not touch `read`; it must not touch `saved` either, so a refresh cannot unsave an article.

**Alternatives considered**: A `saved_article(article_id)` table with its own cascade. Rejected because it adds a join to every list row for a single boolean. Local storage in the browser. Rejected because saved state must survive another browser and the spec requires it to persist with the reader.

## Unread and Saved lists

**Decision**: Extend `GET /api/article` with `view=unread` and `view=saved`. Keep `folder`, `feed`, `q`, and the existing cursor. `view` is mutually exclusive with `folder` and `feed`.

**Rationale**: Both views are the All Feeds list with one extra predicate, still ordered `published DESC, id DESC`, still limited to 25, still omitting HTML bodies. Search tokens stay the existing title/description `ILIKE` filter, applied inside the view.

Unread predicate matches the existing effective-read rule: not (`article.read` OR `id <= reader_state.mark_read_through_id` OR `id <= feed.mark_read_through_id`), and `published IS NOT NULL`. Saved predicate is `saved = true AND published IS NOT NULL`. Articles with no date are already absent from every list, so the reader cannot save them from the UI.

**Alternatives considered**: Separate resources `/api/unread` and `/api/saved`. Rejected because pagination, search, and the summary projection would be copied. A client-side filter of All Feeds. Rejected because All Feeds is paged; unread and saved would be wrong past the first page.

## Save and unsave API

**Decision**: `POST /api/article/{id}/saved` saves. `DELETE /api/article/{id}/saved` unsaves. Both are idempotent and return 204. A missing id returns 404 via `NotFoundException`. Saving does not write `read`. Mark-read paths do not write `saved`.

**Rationale**: `POST /api/article/{id}/read` is already the one-way read action. Saved needs both directions, and DELETE matches folder and feed removal. Idempotent calls let the client retry without a toggle race.

**Alternatives considered**: One `POST` that toggles. Rejected because a retried toggle flips the state twice. A JSON body `{ "saved": true }` on `PUT`. Rejected as more surface for the same two transitions.

## Saved count

**Decision**: `GET /api/article/saved/count` returns `{ "count": <int> }` for dated saved articles. The sidebar hides the number at zero, using the same treatment as folder unread badges. After a successful save or unsave, the client changes that badge on the existing node. On failure it restores the previous number. The count is loaded with the feed list at startup and after a feed refresh, not on every article page.

**Rationale**: The badge is a total, and the list is paged, so the first page length is not the count. Putting the count on the folder or feed payload would mix a reader-wide flag into those resources.

**Alternatives considered**: `X-Total-Count` on every article list. Rejected because All Feeds, folders, and feeds would run an extra count they do not display.

## Indexes

**Decision**: In `V7__article_saved.sql`:

- `article_saved_published_id_idx` on `(published DESC, id DESC)` WHERE `saved = true AND published IS NOT NULL`
- `article_unread_published_id_idx` on `(published DESC, id DESC)` WHERE `read = false AND published IS NOT NULL`

Keep `article_unread_feed_horizon_idx` for the per-feed unread counts.

**Rationale**: Constitution VI requires an index for a new list filter. The saved index matches the Saved list and the count. The unread list is ordered by published, while the existing unread index is `(feed_id, id)` and serves counts, not this order. Horizon bounds (`id > reader_state.mark_read_through_id` and `id > feed.mark_read_through_id`) are runtime values, so they stay residual predicates on top of the partial index. The migration comment states that.

**Alternatives considered**: One shared `(published, id)` index with no predicate. Rejected because All Feeds already has `article_feed_published_id_idx`, and a full index would be larger than the unread and saved subsets.

## Icons

**Decision**: Vendor four Tabler SVGs next to the existing icons, with the same `viewBox="0 0 24 24"`, round caps, and black paint so the existing CSS filters (`.f-base`, `.f-blue`, `.f-grey`, and the dark-scheme overrides) recolor them:

| File | Use |
|------|-----|
| `icons/circle-check.svg` | Unread status mark, and the Unread sidebar icon |
| `icons/circle-check-filled.svg` | Read status mark |
| `icons/bookmark.svg` | Unsaved bookmark, reader bookmark when unsaved, Saved sidebar icon |
| `icons/bookmark-filled.svg` | Saved bookmark on the row and in the reader |

Unread is the blue outline check. Read is the grey filled check. Unsaved is the grey outline bookmark. Saved is the blue filled bookmark. Shape still differs, so color is not the only cue. Both row icons stay about 16px on a wide screen and on a narrow screen. The header bookmark matches the other header buttons. The Saved count is followed by an empty slot the width of the options menu, so the number lines up with the other sidebar counts.

Add the four paths to the service-worker `SHELL` list so they precache with the other icons.

**Rationale**: The reader already attributes icons to Tabler and styles them with filters on `<img>`. Circle-check reads as done or not done. Bookmark reads as save for later. No npm icon package.

**Alternatives considered**: A CSS-only dot. Rejected because the spec requires the same icon library and style. Bold unread titles. Rejected in clarification. Hiding the read check. Rejected because the row would jump and both icons must stay visible. Enlarging the row icons to 44px on a narrow screen. Rejected because that stretched the source line.

## Routes

**Decision**: Client paths `/unread`, `/saved`, `/unread/article/{id}`, and `/saved/article/{id}`. `/` remains All Feeds. `parseRoute` / `buildPath` gain contexts `unread` and `saved`.

`SpaFallback` accepts those paths and still reroutes them to `index.html`. `quarkus.http.auth.permission.spa.paths` gains `/unread`, `/unread/*`, `/saved`, and `/saved/*`, with the same authenticated policy as `/feed/*`, `/folder/*`, and `/article/*`. Both the exact path and the wildcard are listed so `/unread` is not left unmatched by a pattern that only covers a slash suffix.

Choosing Unread or Saved uses the same article-column navigation as choosing All Feeds. Refresh keeps the selection. On a narrow screen, refresh of `/unread` or `/saved` opens the article list (same as a folder). Refresh of `/` keeps today's home behavior.

**Rationale**: FR-028 requires the view to survive refresh. The SPA shell is only served for paths the fallback lists, and those paths are authenticated today. A new public path would violate Principle V.

**Alternatives considered**: Query strings `/?view=unread`. Rejected because feed and folder selections are already path segments, and the router tests round-trip those paths.

## Row behavior

**Decision**: The article row stops being `role="button"` once it contains the check and the bookmark. The row stays keyboard-focusable. Enter and Space on the row open the article. Enter and Space on the check or bookmark do not. Clicks on those controls do not open the article.

The title is the first line. A picture, when present, sits under the title. The bottom line is the source on the left and, on the right, the time, the circle-check, then the bookmark. Swapping outline and filled SVGs does not move those icons. The title's font, weight, and color do not change. `.unread` may remain as a state hook, and it must not restyle the title. A thin divider, using the existing border color, sits between Saved and the folders.

Activating the outline check calls the existing `markArticleAsRead` path. The filled check is named "Read" and does nothing. The bookmark calls save or unsave. Optimistic updates revert when the request fails, following the existing mark-read rollback.

While Unread is already on screen, a row that becomes read switches to the filled check and stays in the list. The next load of Unread omits it. While Saved is on screen, unsaving switches to the outline bookmark and the row stays until the next load of Saved. Sidebar counts update immediately.

**Rationale**: A button inside `role="button"` is invalid, and the spec requires the check, the bookmark, and the row to be separate actions. Keeping the row in place is the clarified non-jumping rule. Putting both icons on the bottom line keeps the title clear. Reusing `markArticleAsRead` keeps unread badges and rollback on one path.

**Alternatives considered**: Removing the unread row as soon as it is marked read. Rejected in the spec because the list would move beside the open article. A leading cluster at the start of the row. Rejected after review because the marks competed with the title.

## Document title

**Decision**: With no article open, Unread sets the document title to `Unread · <app title>` and Saved sets `Saved · <app title>`. All Feeds, feeds, and folders keep their current titles.

**Rationale**: Folders and feeds already put their name in the title. The new views should be recognizable after refresh. This does not change article titles.
