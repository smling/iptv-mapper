-- Add URL checker result and timing to tv

ALTER TABLE IF EXISTS tv
    ADD COLUMN IF NOT EXISTS url_checker_result TEXT;

ALTER TABLE IF EXISTS tv
    ADD COLUMN IF NOT EXISTS url_checker_ms BIGINT;

