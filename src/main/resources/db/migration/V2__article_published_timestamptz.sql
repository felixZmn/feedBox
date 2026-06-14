-- V2__article_published_timestamptz.sql
-- Convert article.published from VARCHAR(255) to TIMESTAMPTZ.
--
-- Background: the V1 schema stored `published` as text. The pagination
-- query ("WHERE a.published < ? OR (a.published = ? AND a.id < ?)")
-- relied on lexicographic string comparison, which only works because
-- ArticleMapper formatted the date as a fixed-width "yyyy-MM-dd HH:mm:ss 'UTC'"
-- string. That stopped working as soon as we needed sub-second precision
-- or non-fixed-width representations, and a TIMESTAMPTZ column lets
-- PostgreSQL use a real range index for the pagination query plan.
--
-- The migration is safe to run while a previous version of feedBox is
-- still inserting: any inserts that race with the backfill will be lost
-- only if they land during the (very short) ALTER TABLE window, and
-- Flyway runs migrations inside a transaction so concurrent inserts are
-- blocked until the commit.

-- ---------------------------------------------------------------------------
-- 1. Add the new column (nullable for now; will become NOT NULL after backfill).
-- ---------------------------------------------------------------------------
ALTER TABLE article ADD COLUMN IF NOT EXISTS published_tz TIMESTAMPTZ;

-- ---------------------------------------------------------------------------
-- 2. Best-effort backfill from the existing text column.
--    Strings that don't match the expected pattern are set to NULL; the
--    schema now allows NULL and the read query filters them out.
-- ---------------------------------------------------------------------------
UPDATE article
   SET published_tz = CASE
        WHEN published IS NULL OR published = '' THEN NULL
        -- The original ArticleMapper wrote "yyyy-MM-dd HH:mm:ss 'UTC'".
        -- The trailing "' UTC'" is literal text in DateTimeFormatter, so
        -- strip it before parsing.
        WHEN published ~ '^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}( UTC)?$'
            THEN (regexp_replace(published, ' UTC$', '') || ' UTC')::timestamptz
        ELSE NULL
   END
 WHERE published_tz IS NULL;

-- ---------------------------------------------------------------------------
-- 3. Replace the column.
--    Drop the index first (it references the old column), then drop the
--    old column, then rename. Wrapped in IF EXISTS for idempotency on
--    databases that have already been migrated.
-- ---------------------------------------------------------------------------
DROP INDEX IF EXISTS article_feed_published_id_idx;

ALTER TABLE article DROP COLUMN IF EXISTS published;

ALTER TABLE article RENAME COLUMN published_tz TO published;

-- ---------------------------------------------------------------------------
-- 4. Recreate the composite index against the new column.
--    The pagination query uses "published DESC" with id as a tie-breaker.
--    NULLS LAST preserves the current "missing pubDates are filtered out"
--    behaviour if the read query ever switches to including them.
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS article_feed_published_id_idx
    ON article (feed_id, published DESC NULLS LAST, id DESC);
