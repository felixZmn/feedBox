# Data Model: Read, Unread, and Saved Articles

## Article

Existing record `de._0x2b.model.Article` and list projection `ArticleSummary`.

| Field | Type | Rules |
|-------|------|--------|
| `id` | long | Identity. Unchanged. |
| `feedId` | int | Owning feed. Deleting the feed deletes the article. |
| `read` | boolean | Stored flag. Effective read is also true when `id` is at or below `reader_state.mark_read_through_id` or the feed's `mark_read_through_id`. |
| `saved` | boolean | New. `NOT NULL DEFAULT false`. Independent of `read` and of both horizons. |

`ArticleSummary` gains `saved` and still omits `content` and `description`. The detail payload gains `saved` and still returns the HTML body only from `GET /api/article/{id}`.

Convenience constructors pass `saved = false`. `INSERT` does not name `saved` or `read`, so the database defaults apply. The feed upsert must not assign either column.

Dated articles (`published IS NOT NULL`) are the only rows lists and the saved count consider. That matches the current list filter.

## Reader state and feed horizons

Unchanged.

| Store | Field | Effect |
|-------|-------|--------|
| `reader_state` | `mark_read_through_id` | Article ids at or below this value are read everywhere. |
| `feed` | `mark_read_through_id` | Article ids at or below this value are read for that feed. |

Mark as read on one article sets `article.read`. Mark as read on a feed, a folder, or All Feeds advances the matching horizon. None of those writes change `saved`.

## Indexes

| Name | Definition | Why |
|------|------------|-----|
| `article_unread_feed_horizon_idx` | Existing `(feed_id, id)` WHERE `read = false AND published IS NOT NULL` | Per-feed unread counts. Keep. |
| `article_unread_published_id_idx` | New `(published DESC, id DESC)` WHERE `read = false AND published IS NOT NULL` | Unread list order. Horizon checks remain residual predicates. |
| `article_saved_published_id_idx` | New `(published DESC, id DESC)` WHERE `saved = true AND published IS NOT NULL` | Saved list and saved count. |
| `article_feed_published_id_idx` | Existing | All Feeds, folder, and feed pages. Keep. |

## Validation

- `view`, when present, is `unread` or `saved`.
- `view` cannot be combined with `folder` or `feed`.
- `folder` and `feed` remain mutually exclusive.
- `q` stays trimmed and capped at 100 characters.
- Save and unsave require an existing article id.
- Save and unsave are idempotent.

## State transitions

### Read

```text
unread --open article-----------------> read
unread --activate filled mark---------> read
unread --mark feed, folder, or all----> read
read   --activate unfilled mark-------> read
```

There is no transition back to unread for a single article.

Effective read is what the list, the mark icon, and Unread filtering use. A row can be read because of a horizon even when `article.read` is false.

### Saved

```text
unsaved --activate star--> saved
saved   --activate star--> unsaved
```

Read transitions do not change this state. Saved transitions do not change read state.

### List membership

| View already on screen | Transition | What the reader sees |
|------------------------|------------|----------------------|
| Unread | article becomes read | Mark becomes unfilled. Row stays. All Feeds unread count drops now. Next load of Unread omits the row. |
| Saved | article is unsaved | Star becomes unfilled. Row stays. Saved count drops now. Next load of Saved omits the row. |
| Saved | saved article becomes read | Mark becomes unfilled. Row stays, including on the next load. |
| Any | save or unsave fails | Star and saved count return to the previous values. |
| Any | mark-read fails | Mark stays filled. Unread count returns to the previous value. |

## Relationships

- An article belongs to one feed.
- Saved is an attribute of the article, not a folder and not a second list the reader files into.
- A saved unread article counts toward the All Feeds unread total and toward the Saved total.
