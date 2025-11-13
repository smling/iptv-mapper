-- Add response time (ms) for URL checks on M3U items

ALTER TABLE IF EXISTS m3u_item
    ADD COLUMN IF NOT EXISTS url_checker_ms BIGINT;

-- Optional index if querying by performance is needed later
-- CREATE INDEX IF NOT EXISTS idx_m3u_item_url_checker_ms ON m3u_item(url_checker_ms);

