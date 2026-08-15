-- PIKOP V3 MIGRATION FIX
-- Clears the migration history table so re-sequenced files can run from scratch.

TRUNCATE TABLE pgmigrations;

-- End of script
