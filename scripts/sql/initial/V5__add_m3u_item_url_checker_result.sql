-- Add dedicated column to store URL reachability result for M3U items

ALTER TABLE IF EXISTS m3u_item
    ADD COLUMN IF NOT EXISTS url_checker_result TEXT;

-- Optional helper index if you plan to filter by this frequently
-- CREATE INDEX IF NOT EXISTS idx_m3u_item_url_checker_result ON m3u_item(url_checker_result);

