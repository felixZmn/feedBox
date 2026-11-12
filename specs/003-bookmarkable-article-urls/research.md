# Research: Bookmarkable Article URLs

## Keep the existing address grammar

**Decision**: Keep the path grammar already implemented in `scripts/router.js` and `SpaFallback`. No new address shape, hash routing, or article slug.

| Place | Address |
|-------|---------|
| All Feeds | `/` |
| All Feeds + article | `/article/{id}` |
| Unread | `/unread` and `/unread/article/{id}` |
| Saved | `/saved` and `/saved/article/{id}` |
| Feed | `/feed/{id}` and `/feed/{id}/article/{id}` |
| Folder | `/folder/{id}` and `/folder/{id}/article/{id}` |

`{id}` is a positive decimal integer. A trailing slash is the same place. Unknown query parameters are ignored for selection and are left on the address.

**Rationale**: Story 1 and story 2 already match this grammar. `parseRoute` / `buildPath` and the authenticated path policy (`quarkus.http.auth.permission.spa.paths`) already use it. Bookmarks and later sharing are copies of that browser address (FR-012).

**Alternatives considered**: Title slugs. Rejected — titles change and are not unique. Hash addresses. Rejected — a hash is not the path the server and the service worker already treat as a document navigation. Always rewriting a list-plus-article address to `/article/{id}`. Rejected — clarifications require the named list to stay in the address.

## Open any existing article; do not test list membership

**Decision**: If the named article still exists, open it and keep the address, even when it is not a member of the named list. Delete the membership gate in `articleBelongsToContext`. All Feeds, Unread, Saved, a feed, and a folder all follow this rule. The article still becomes read through the existing `loadArticle` path.

**Rationale**: FR-006 and FR-020. Today `articleBelongsToContext` returns false for Unread and Saved, and for a feed or folder the article does not belong to. `openArticleById` then treats the article as missing and `replaceAppUrl` strips it. A refresh of `/unread/article/{id}` leaves the article. An article that is not on the first page of its list is already loaded with `GET /api/article/{id}` when it is absent from `state.articles`; that single-article read stays the way to open it (FR-003). Do not scan later pages.

**Alternatives considered**: Redirect a foreign article to its own feed. Rejected — the address would change. Show the foreign article as unavailable. Rejected by clarification. Keep the membership gate for feeds only. Rejected — Unread and Saved are the refresh bug, and feeds were given the same rule.

## Missing list and unrecognized path replace the address with All Feeds

**Decision**: When the named feed or folder no longer exists, or the path is not an address this reader understands, replace the current history entry with `/`, select All Feeds, and open no article. Do this even if the old address also named an article. Back does not return to the abandoned address because the entry was replaced, not pushed.

**Rationale**: Clarification on 2026-11-12, FR-014 and FR-015. Missing feeds and folders already call `selectAndLoadArticles(ALL, …, { syncUrl: "replace" })`, which uses `history.replaceState`. Keep that. An unrecognized path must reach the shell first (see below) and then use the same replace.

**Alternatives considered**: Leave the bad address in the bar while showing All Feeds. Rejected by clarification. HTTP redirect to `/`. Rejected — a redirect leaves the bad address on the back stack, so Back returns to it and redirects again.

## A missing article keeps its address

**Decision**: When the list exists and the article does not (`GET /api/article/{id}` is 404), keep the address, keep the named list selected, and show the plain sentence "This article is unavailable." in the reader column. Do not open a modal. On a narrow screen the reader column is the visible pane. Do not mark anything read.

**Rationale**: FR-013. Today a 404 sets `opened` false and rewrites the address to the list with no article, and the reader column is cleared with no message. The message is app copy, inserted with `textContent`, in a sibling of the article stack so the toolbar stays hidden with the article.

**Alternatives considered**: Reuse the "Something went wrong" init modal. Rejected — that is the failure the spec forbids for a missing article. Rewrite the address to `/article/{id}` or `/`. Rejected — the address stays.

## Do not rewrite an address that already resolved

**Decision**: `loadApp` may still call `applyRouteFromLocation({ replaceHistory: true })`, but a successful resolution does not call `replaceState`. Replacement runs only for the All Feeds fallback. Trailing slashes and unused query parameters therefore survive refresh (FR-005, FR-016).

**Rationale**: Today every successful apply replaces the address with `pathForSelection`, which drops the query string and the trailing slash.

**Alternatives considered**: Canonicalize on load. Rejected — refresh would change the address the reader bookmarked.

## Unrecognized documents get the shell; known deep links stay authenticated

**Decision**: Extend `SpaFallback` so a GET navigation that accepts HTML, is not a real static file, and is not under `/api/`, `/q/`, `/start`, `/logout`, or `/oidc`, receives `index.html`. Do not add known article, feed, folder, unread, or saved paths to the public permission list. Those stay on `quarkus.http.auth.permission.spa.policy=authenticated`.

Signed-out behavior for a known address stays as it is:

- Without the service worker, the document navigation is authenticated and Quarkus restores the path after the identity-provider redirect (`restore-path-after-redirect=true`).
- With the service worker, `shellNavigate` serves the public login shell on an auth failure and the browser address stays put. The existing login-link listener stores that path, and `takeReturnTo` restores it when login lands on `/`.

Either way the article body is not in the signed-out response. After a successful sign-in the stored address is opened once.

**Rationale**: FR-010, FR-011, FR-015, and Principle V. A bare 404 for `/no-such` never runs the client replace to All Feeds, so the reader cannot satisfy FR-015. Making `/article/*` public would show the shell before sign-in and is a constitution exception this spec does not grant. The service worker already refuses to cache a shell per deep-link path.

**Alternatives considered**: Public deep links plus the in-app login panel only. Rejected — widens the public surface and is unnecessary because restore-path and `rememberReturnTo` already return the reader to the address. Catch-all fallback for every missing file, including `.js`. Rejected — a missing script would be served HTML.

## Connectivity failures stay on the address

**Decision**: A failure to reach the instance during route apply uses the existing offline notice (`notifyOffline` / `isConnectivityFailure`). It does not open the init modal "The app failed to load", and it does not change the address. A later successful retry runs the same route apply.

**Rationale**: FR-018. `loadApp` currently catches every error from `applyRouteFromLocation`, including a failed article fetch, and shows that modal. Article 404 is not a throw. Other connectivity failures must not be.

**Alternatives considered**: New offline copy. Rejected — the spec says the existing notice.

## Previous and Next stay list actions

**Decision**: When the open article is not in the loaded list, Previous and Next return without changing the article and without a dialog. Leave the buttons in the article toolbar. When the unavailable message is showing, the toolbar stays hidden because no article is open.

**Rationale**: FR-019. `navigateArticle` already returns when the id is absent from `state.articles`. No new control.

**Alternatives considered**: Hide Previous and Next in that state. Rejected — the spec asks for no error and an unchanged article, and this feature adds no toolbar rule beyond that.
