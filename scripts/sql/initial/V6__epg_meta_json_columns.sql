-- Add JSONB meta columns to store extended XMLTV DTD fields without schema explosion

ALTER TABLE IF EXISTS channel
    ADD COLUMN IF NOT EXISTS meta JSONB DEFAULT '{}'::jsonb;

ALTER TABLE IF EXISTS programme
    ADD COLUMN IF NOT EXISTS meta JSONB DEFAULT '{}'::jsonb;

