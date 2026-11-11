-- Articles without a feed-supplied date used to be hidden from every list.
-- Give them an estimated date (first seen = now) and flag it so the UI can
-- show that the date is not authoritative. Nothing is deleted.
ALTER TABLE article
    ADD COLUMN published_estimated BOOLEAN NOT NULL DEFAULT false;

UPDATE article
SET published = now(),
    published_estimated = true
WHERE published IS NULL;
