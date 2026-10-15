-- V5__article_guid_uniqueness_indexes.sql
-- Data-model and index fixes for articles (review findings M2 / P2 / P3):
--   * Per-feed identity via guid + UNIQUE (feed_id, guid)
--   * Replace global UNIQUE(link) with UNIQUE (feed_id, link)
--   * Normalise blank links to NULL (so missing links no longer collide)
--   * Global (published, id) index for the "all feeds" pagination path
--   * pg_trgm GIN indexes so leading-wildcard ILIKE search can use an index

-- ---------------------------------------------------------------------------
-- 1. Trigram extension (required for gin_trgm_ops).
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- 2. Normalise blank links → NULL before uniqueness changes.
-- ---------------------------------------------------------------------------
UPDATE article SET link = NULL WHERE link IS NOT NULL AND btrim(link) = '';

-- ---------------------------------------------------------------------------
-- 3. Drop the global unique constraint on link (name varies by how it was
--    created: column "unique" → article_link_key; named constraint otherwise).
-- ---------------------------------------------------------------------------
ALTER TABLE article DROP CONSTRAINT IF EXISTS article_link_key;

DO $$
DECLARE
    v_constraint TEXT;
BEGIN
    SELECT c.conname
      INTO v_constraint
      FROM pg_constraint c
      JOIN pg_class t ON t.oid = c.conrelid
     WHERE t.relname = 'article'
       AND c.contype = 'u'
       AND pg_get_constraintdef(c.oid) ~* '\(link\)'
     LIMIT 1;

    IF v_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE article DROP CONSTRAINT %I', v_constraint);
    END IF;
END
$$;

DROP INDEX IF EXISTS article_link_key;
DROP INDEX IF EXISTS article_link_idx;

-- ---------------------------------------------------------------------------
-- 4. Deduplicate rows that would violate UNIQUE (feed_id, link). Keep the
--    newest id so refresh history prefers the latest insert.
-- ---------------------------------------------------------------------------
DELETE FROM article a
    USING article b
WHERE a.feed_id = b.feed_id
  AND a.link IS NOT NULL
  AND a.link = b.link
  AND a.id < b.id;

-- ---------------------------------------------------------------------------
-- 5. Add guid. Prefer the article link; otherwise a stable hash so every
--    row gets a non-null per-feed identity even without a URL.
-- ---------------------------------------------------------------------------
ALTER TABLE article ADD COLUMN IF NOT EXISTS guid TEXT;

UPDATE article
   SET guid = CASE
        WHEN link IS NOT NULL AND btrim(link) <> '' THEN link
        ELSE 'fb:' || md5(
            id::text || '|' ||
            coalesce(title, '') || '|' ||
            coalesce(published::text, '')
        )
   END
 WHERE guid IS NULL OR btrim(guid) = '';

-- If two rows somehow share the same computed guid in one feed, disambiguate
-- by appending the id before enforcing NOT NULL / UNIQUE.
UPDATE article a
   SET guid = a.guid || '#' || a.id::text
  FROM (
        SELECT feed_id, guid
          FROM article
         WHERE guid IS NOT NULL
         GROUP BY feed_id, guid
        HAVING COUNT(*) > 1
       ) dups
 WHERE a.feed_id = dups.feed_id
   AND a.guid = dups.guid;

ALTER TABLE article ALTER COLUMN guid SET NOT NULL;

-- ---------------------------------------------------------------------------
-- 6. Uniqueness: per-feed guid (insert conflict target) and per-feed link.
--    Multiple NULL links in the same feed are allowed (PG UNIQUE treats
--    NULLs as distinct).
-- ---------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS article_feed_guid_uidx
    ON article (feed_id, guid);

CREATE UNIQUE INDEX IF NOT EXISTS article_feed_link_uidx
    ON article (feed_id, link);

-- ---------------------------------------------------------------------------
-- 7. P3 — pagination index for the dominant "all feeds" ORDER BY.
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS article_published_id_idx
    ON article (published DESC, id DESC)
    WHERE published IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 8. P2 — trigram indexes for ILIKE '%token%' search on title/description.
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS article_title_trgm_idx
    ON article USING gin (title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS article_description_trgm_idx
    ON article USING gin (description gin_trgm_ops);
