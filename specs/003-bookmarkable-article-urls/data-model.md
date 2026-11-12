# Data Model: Bookmarkable Article URLs

No new database entity, migration, or stored user preference. Addresses are browser locations. Articles, feeds, and folders stay the rows that already exist.

## Address

The current browser location. Not persisted by FeedBox except for the one sign-in return path already stored in session storage.

| Field | Values | Rules |
|-------|--------|--------|
| `path` | See [addresses.md](./contracts/addresses.md) | Identifies a list place and, optionally, one article. A trailing slash is ignored when matching. |
| `query` | Browser query string | Not interpreted. Preserved when the address resolves. Dropped only when the address is replaced with `/`. |
| `context` | `all` \| `unread` \| `saved` \| `feed` \| `folder` | List selected from the path. `all` is All Feeds, including `/article/{id}`. |
| `contextId` | number or empty | Feed or folder id when `context` is `feed` or `folder`. |
| `articleId` | number or empty | Article named by the path. |

## Resolution

Derived when a path is opened, refreshed, or restored. Inputs are the parsed address plus whether that feed, folder, and article still exist. Membership of the article in the list is not an input.

| Status | When | List shown | Article | Address |
|--------|------|------------|---------|---------|
| `ready` | List exists, and no article is named or the article exists | Named list | Named article, or none | Unchanged |
| `article-unavailable` | List exists, article id does not | Named list | None. Reader shows "This article is unavailable." | Unchanged |
| `replaced-home` | Path is not an address, or the named feed or folder does not exist | All Feeds | None | Replaced with `/` |

Opening a `ready` article uses the existing read transition: the article becomes read, saved state is unchanged. `article-unavailable` and `replaced-home` do not change read or saved state.

## Sign-in return

One session-storage path, already used by the login link. It is written when a signed-out reader asks to sign in from an app address, read once when the next page is `/`, then cleared. The identity-provider restore path is the other way back onto a known address. Neither survives a later, unrelated sign-in.

## Reader presentation

| Field | Values | Rules |
|-------|--------|--------|
| `articleOpen` | boolean | True only for status `ready` with an article id. Toolbar and body follow it, as they do today. |
| `unavailable` | boolean | True only for status `article-unavailable`. Message is visible in the reader column. Toolbar and body are hidden. |
| `column` | feeds, articles, or reader | An address with an article id, including unavailable, shows the reader on a narrow screen. A list with no article shows that list. `replaced-home` shows feeds. |

Search text is not part of the address. Applying an address clears the current search.
