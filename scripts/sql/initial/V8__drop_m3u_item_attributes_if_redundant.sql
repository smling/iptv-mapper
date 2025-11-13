-- Cleanup duplicated keys from m3u_item.attributes and drop column if empty everywhere

-- 1) Remove overlapping keys that are stored in typed columns already
UPDATE m3u_item
SET attributes = attributes
    - 'tvg-id'
    - 'tvg-name'
    - 'tvg-logo'
    - 'group-title'
    - 'urlCheckerResult'
    - 'urlCheckerMs'
WHERE attributes IS NOT NULL
  AND (
      attributes ? 'tvg-id'
   OR attributes ? 'tvg-name'
   OR attributes ? 'tvg-logo'
   OR attributes ? 'group-title'
   OR attributes ? 'urlCheckerResult'
   OR attributes ? 'urlCheckerMs'
  );

-- 2) If, after cleanup, no rows contain any remaining attributes, drop the column
DO $$
BEGIN
  IF NOT EXISTS (
      SELECT 1 FROM m3u_item WHERE attributes IS NOT NULL AND attributes::text <> '{}'::text
  ) THEN
    ALTER TABLE IF EXISTS m3u_item DROP COLUMN IF EXISTS attributes;
  END IF;
END$$;

