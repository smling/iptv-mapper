-- Remove JSONB meta columns from EPG tables (no more metadata attributes)

ALTER TABLE IF EXISTS channel
    DROP COLUMN IF EXISTS meta;

ALTER TABLE IF EXISTS programme
    DROP COLUMN IF EXISTS meta;

