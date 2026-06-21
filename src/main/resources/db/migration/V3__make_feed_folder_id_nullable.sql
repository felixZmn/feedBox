-- V3__make_feed_folder_id_nullable.sql
-- Remove the sentinel root folder and make feed.folder_id nullable.
--
-- The root folder (id=0, name='root') was a workaround so that every feed
-- had a non-null folder_id. Now feed.folder_id is truly nullable: feeds
-- that don't belong to a folder simply store NULL.
--
-- This migration is safe to run against either a fresh database (where V1
-- has just been applied) or an existing production database.  Rows that
-- already have folder_id = 0 are updated to NULL, then the sentinel row
-- itself is deleted.

-- ---------------------------------------------------------------------------
-- 1. Drop the CHECK constraint that protected the root row.
-- ---------------------------------------------------------------------------
ALTER TABLE folder DROP CONSTRAINT IF EXISTS folder_id_not_zero;

-- ---------------------------------------------------------------------------
-- 2. Make the column nullable first (before the UPDATE that sets NULLs).
-- ---------------------------------------------------------------------------
ALTER TABLE feed ALTER COLUMN folder_id DROP NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Reassign feeds that pointed to the root folder.
-- ---------------------------------------------------------------------------
UPDATE feed SET folder_id = NULL WHERE folder_id = 0;

-- ---------------------------------------------------------------------------
-- 4. Delete the root sentinel row.
-- ---------------------------------------------------------------------------
DELETE FROM folder WHERE id = 0;