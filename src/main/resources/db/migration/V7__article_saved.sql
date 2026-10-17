-- V7__article_saved.sql
-- Saved is independent of article.read and of the read horizons.
-- Horizon id bounds are runtime values, so they stay residual predicates
-- on the unread list; this index covers the column-unread dated subset
-- ordered the same way as All Feeds.

ALTER TABLE article ADD COLUMN IF NOT EXISTS saved BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS article_saved_published_id_idx
    ON article (published DESC, id DESC)
    WHERE saved = true AND published IS NOT NULL;

CREATE INDEX IF NOT EXISTS article_unread_published_id_idx
    ON article (published DESC, id DESC)
    WHERE read = false AND published IS NOT NULL;
