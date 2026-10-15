-- V4__article_read.sql
-- Persist per-article read state and index unread counts by feed.

ALTER TABLE article ADD COLUMN IF NOT EXISTS read BOOLEAN NOT NULL DEFAULT false;

-- Matches the article list filter: only dated articles are shown / counted.
CREATE INDEX IF NOT EXISTS article_unread_feed_id_idx
    ON article (feed_id)
    WHERE read = false AND published IS NOT NULL;
