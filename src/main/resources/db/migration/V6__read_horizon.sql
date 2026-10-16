-- V6__read_horizon.sql
-- Catch-up mark-read without rewriting every article row.
-- Global and per-feed high-water marks (article id); unread queries treat
-- id <= mark as effectively read.

CREATE TABLE IF NOT EXISTS reader_state (
    id                    SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    mark_read_through_id  BIGINT NOT NULL DEFAULT 0
);

INSERT INTO reader_state (id, mark_read_through_id)
VALUES (1, 0)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE feed
    ADD COLUMN IF NOT EXISTS mark_read_through_id BIGINT NOT NULL DEFAULT 0;

-- Horizon-aware unread scans: feed_id + id above the high-water marks.
CREATE INDEX IF NOT EXISTS article_unread_feed_horizon_idx
    ON article (feed_id, id)
    WHERE read = false AND published IS NOT NULL;

DROP INDEX IF EXISTS article_unread_feed_id_idx;
