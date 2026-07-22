-- V1__init.sql
-- Initial schema for feedBox.
-- This migration is idempotent: every CREATE/ALTER uses IF [NOT] EXISTS so it
-- can be applied safely to a database that was previously created by the
-- hand-rolled Java migration array. With quarkus.flyway.baseline-on-migrate=true
-- Flyway will record a baseline at V0 for such databases and treat the V1
-- statements as no-ops on first run after the upgrade.
--
-- Notes on fixes bundled into V1 (all additive, no destructive changes):
--   * icon.feed_id is changed from ON DELETE SET NULL to ON DELETE CASCADE to
--     stop the icon-bytea leak on feed delete.
--   * folder gets CHECK (id <> 0) so id 0 cannot be used as a folder PK
--     (legacy "root" sentinel cleanup is handled in V3).
--   * feed gets last_refreshed_at and last_error columns to surface
--     per-feed refresh failures (currently silently logged away).
--   * Missing FK indexes and a composite (feed_id, published DESC, id DESC)
--     index are added for the pagination query plan.

-- ---------------------------------------------------------------------------
-- folder
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS folder (
    id    SERIAL PRIMARY KEY,
    name  VARCHAR(255) UNIQUE NOT NULL,
    color VARCHAR(255) DEFAULT 'f-base'
);

-- Forbid folder id 0. Legacy installs may still have a hand-rolled `id = 0`
-- "root" sentinel row; adding CHECK (id <> 0) against that row would fail
-- with SQLSTATE 23514. Only add the constraint if (a) it does not exist yet
-- AND (b) no row has id = 0. Fresh installs get the CHECK; upgrade installs
-- keep the sentinel until V3 drops the constraint and deletes the row.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'folder_id_not_zero'
    ) AND NOT EXISTS (
        SELECT 1 FROM folder WHERE id = 0
    ) THEN
        ALTER TABLE folder
            ADD CONSTRAINT folder_id_not_zero CHECK (id <> 0);
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- feed
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS feed (
    id        SERIAL PRIMARY KEY,
    folder_id INT          NOT NULL,
    name      VARCHAR(255) NOT NULL,
    url       VARCHAR(2048) NOT NULL,
    feed_url  VARCHAR(2048) NOT NULL UNIQUE,
    CONSTRAINT feed_folder_id_fkey
        FOREIGN KEY (folder_id) REFERENCES folder(id) ON DELETE CASCADE
);

-- Refresh-health columns. Added with IF NOT EXISTS so the migration is
-- re-runnable against an existing schema.
ALTER TABLE feed ADD COLUMN IF NOT EXISTS last_refreshed_at TIMESTAMPTZ;
ALTER TABLE feed ADD COLUMN IF NOT EXISTS last_error       TEXT;

-- ---------------------------------------------------------------------------
-- icon
-- ---------------------------------------------------------------------------
-- Note: the original schema declared ON DELETE SET NULL on icon.feed_id, which
-- leaked icon rows (and their bytea payloads) on every feed delete. We
-- recreate the table only if it doesn't already exist; for an existing
-- hand-rolled DB we drop the old FK and re-add it as CASCADE.
CREATE TABLE IF NOT EXISTS icon (
    id        SERIAL PRIMARY KEY,
    feed_id   INTEGER,
    image     BYTEA       NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    CONSTRAINT icon_feed_id_fkey
        FOREIGN KEY (feed_id) REFERENCES feed(id) ON DELETE CASCADE
);

-- For pre-existing tables with ON DELETE SET NULL, swap the constraint.
DO $$
DECLARE
    v_constraint TEXT;
BEGIN
    SELECT c.conname
      INTO v_constraint
      FROM pg_constraint c
      JOIN pg_class t ON t.oid = c.conrelid
     WHERE t.relname = 'icon'
       AND c.contype = 'f'
       AND pg_get_constraintdef(c.oid) LIKE '%feed(id)%ON DELETE SET NULL%';

    IF v_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE icon DROP CONSTRAINT %I', v_constraint);
        EXECUTE 'ALTER TABLE icon
                 ADD CONSTRAINT icon_feed_id_fkey
                 FOREIGN KEY (feed_id) REFERENCES feed(id) ON DELETE CASCADE';
    END IF;
END
$$;

-- ---------------------------------------------------------------------------
-- article
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS article (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    feed_id     INTEGER NOT NULL,
    title       TEXT    NOT NULL,
    description TEXT    NOT NULL,
    content     TEXT    NOT NULL,
    link        TEXT    NULL unique,
    published   VARCHAR(255) NULL,
    authors     TEXT    NOT NULL,
    image_url   TEXT    NULL,
    categories  TEXT    NULL,
    CONSTRAINT article_feed_id_fkey
        FOREIGN KEY (feed_id) REFERENCES feed(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------
-- Foreign keys are NOT automatically indexed in PostgreSQL. Add the missing
-- ones so the join / per-feed lookups don't degrade into seq scans.

CREATE INDEX IF NOT EXISTS feed_folder_id_idx  ON feed (folder_id);
CREATE INDEX IF NOT EXISTS article_feed_id_idx ON article (feed_id);
CREATE INDEX IF NOT EXISTS icon_feed_id_idx    ON icon (feed_id);

-- Composite index for the article pagination query
-- (WHERE a.published < ? OR (a.published = ? AND a.id < ?) ORDER BY ...).
-- Covers both findAll / findByFeed / findByFolder path variants.
CREATE INDEX IF NOT EXISTS article_feed_published_id_idx
    ON article (feed_id, published DESC, id DESC);
