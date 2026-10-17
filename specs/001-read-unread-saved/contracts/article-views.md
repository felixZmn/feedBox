# Contract: Article views, saved state, and reader routes

The bundled reader is the only client. List responses remain summaries. Authenticated methods use the existing session cookie. Paths below are relative to the application origin.

## List articles

`GET /api/article`

Existing query parameters stay: `folder`, `feed`, `pagination_id`, `pagination_date`, `q`.

New optional parameter:

| `view` | List |
|--------|------|
| omitted | All dated articles, or the folder/feed selection. Current behavior. |
| `unread` | Dated articles that are not effectively read. Same order as All Feeds. |
| `saved` | Dated articles with `saved = true`, read or unread. Same order as All Feeds. |

Rules:

- `folder` and `feed` together remain `400`.
- `view` together with `folder` or `feed` is `400`.
- Any other `view` value is `400`.
- `q` still applies inside the chosen list. No matches use the client's existing "No articles match …" message.
- Page size remains 25. The cursor remains the last row's `published` and `id`.

Each summary gains `saved` (boolean) and keeps `read` (boolean, effective read). Bodies stay absent.

`GET /api/article/{id}` gains `saved` on the full article. `404` when the id does not exist.

## Mark read

Unchanged: `POST /api/article/{id}/read`, `POST /api/article/read`, `POST /api/feed/{id}/read`, `POST /api/folder/{id}/read`. They do not change `saved`.

## Save and unsave

| Method | Path | Success | Missing article |
|--------|------|---------|-----------------|
| `POST` | `/api/article/{id}/saved` | `204`, article is saved | `404` |
| `DELETE` | `/api/article/{id}/saved` | `204`, article is not saved | `404` |

Repeating either call when the article is already in the target state still returns `204`. Neither call changes read state.

Anonymous calls to these paths, and to `GET /api/article/saved/count`, return `401`.

## Saved count

`GET /api/article/saved/count`

```json
{ "count": 0 }
```

`count` is the number of dated saved articles. It is the number the Saved sidebar row shows when greater than zero. Zero hides the badge.

## Client routes

| Path | Selection | Open article |
|------|-----------|--------------|
| `/` | All Feeds | — |
| `/article/{id}` | All Feeds | that article |
| `/unread` | Unread | — |
| `/unread/article/{id}` | Unread | that article |
| `/saved` | Saved | — |
| `/saved/article/{id}` | Saved | that article |
| `/feed/{id}` and `/folder/{id}` | unchanged | unchanged |

`/unread` and `/saved` are authenticated SPA paths, like `/feed/*`. The shell fallback serves `index.html` for the four new paths. Unknown paths stay invalid and still fall back to All Feeds in the client.

## Row and sidebar contract

The title is the first line. A picture, when the article has one, sits under the title. The bottom line has the source on the left. On the right, in order: the time, the circle-check, then the bookmark.

1. Circle-check. Blue outline, name "Mark as read", when the article is unread. Grey filled, name "Read", when it is read. Activating the outline check marks the article read and does not open it. Activating the filled check does not open it and does not mark it unread.
2. Bookmark. Grey outline, name "Save article", when unsaved. Blue filled, name "Remove from Saved", when saved. Activating it does not open the article and does not change read state.

On a narrow screen those two row icons stay the same small size as on a wide screen. The header bookmark matches the other header buttons.

The row's accessible name includes the title and either "Unread" or "Read". The title font is the same in both states. Selection and hover keep their current backgrounds.

Sidebar, top to bottom: All Feeds (existing unread count and options menu), Unread (circle-check icon, no count, no options menu), Saved (bookmark icon, saved count only when greater than zero, lined up with the other sidebar counts, no options menu), a thin divider, folders, Add.

Empty Unread copy: "No unread articles". Empty Saved copy: "No saved articles".
