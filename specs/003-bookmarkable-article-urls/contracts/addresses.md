# Contract: Reader addresses

The bundled reader is the only client. This contract is the browser address and how it resolves. No HTTP API is added or changed. `GET /api/article/{id}` remains the single-article read.

Grammar and outcomes are defined in [data-model.md](../data-model.md).

## Paths

Decimal ids only. Anything else is not an address.

- `/`
- `/article/{id}`
- `/unread` and `/unread/article/{id}`
- `/saved` and `/saved/article/{id}`
- `/feed/{id}` and `/feed/{id}/article/{id}`
- `/folder/{id}` and `/folder/{id}/article/{id}`

A trailing slash matches the same path. The query string is not part of the match.

## Resolution

| Condition | Status | Address after | What the reader shows |
|-----------|--------|---------------|------------------------|
| Known list, no article id | `ready` | Same | That list, empty reader |
| Known list, article exists | `ready` | Same | That list and that article, even when the article is not a member of the list and even when it is not among the first articles loaded for the list |
| Known list, article does not exist | `article-unavailable` | Same | That list, and the sentence "This article is unavailable." in the reader column. No modal |
| Feed or folder id does not exist, with or without an article id | `replaced-home` | `/` | All Feeds, empty reader. Back does not return to the old address |
| Path is not in the grammar | `replaced-home` | `/` | All Feeds, empty reader. Back does not return to the old address |

Choosing a list or an article, and Previous or Next while the open article is in the loaded list, updates the address to the path for that place. Previous or Next when the open article is not in the loaded list leaves the article and the address as they are and shows no dialog.

Refresh of a `ready` or `article-unavailable` address shows the same row again. Refresh of `/` after a replacement stays on All Feeds.

## Signed-out

Known paths stay authenticated. A signed-out document navigation does not receive the article body.

- The in-app sign-in panel or the identity-provider sign-in screen is what the reader sees.
- After a successful sign-in, the reader opens the address that was requested.
- Signing in from `/` with no stored address opens All Feeds and no article.
- A person who cannot sign in cannot read the article.

Unrecognized paths may serve the public shell so the client can replace them with `/`. They must not be added to the authenticated deep-link list, and they must not be able to return article HTML.

## Shell

`SpaFallback` serves `index.html` for an HTML GET that is not a real static file and not `/api/*`, `/q/*`, `/start`, `/logout`, or `/oidc`. Known deep links remain authenticated. Missing scripts and styles keep their current not-found behavior.

## Unavailable message

Inside `#reader .column`, a sibling of `.article`, hidden unless status is `article-unavailable`.

- Text: `This article is unavailable.`
- Set with `textContent`. Not a control. No button.
- `.article` stays hidden, so the article toolbar, title, and body are not shown.
- The rest of the shell stays usable, including choosing All Feeds.

## Out of scope

No share control, no copy-link control, and no public article page. Search is not written into the address.
