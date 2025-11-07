-- =========================================================
-- PostgreSQL schema for IPTV/EPG data model
-- Derived from Java records: M3UPlaylist, M3UItem, Tv, Channel, Programme, Icon, EpisodeNumber
-- =========================================================

-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for gen_random_uuid()

-- Distinguish source types
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'data_source_type') THEN
    CREATE TYPE data_source_type AS ENUM ('EPG', 'M3U');
  END IF;
END$$;

-- Master table of data sources
CREATE TABLE data_source (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type               data_source_type NOT NULL,     -- EPG or M3U
    url                TEXT NOT NULL,                 -- canonical fetch URL
    label              TEXT,                          -- optional friendly name
    country_code       TEXT,                          -- e.g. "AL", "AR" for EPGs
    enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    priority           INT NOT NULL DEFAULT 100,      -- smaller = higher priority
    notes              TEXT,

    -- fetch/health metadata (optional but handy)
    last_http_status   INT,
    last_fetched_at    TIMESTAMPTZ,
    last_etag          TEXT,
    last_modified_hdr  TEXT,
    content_checksum   TEXT,                          -- e.g. sha256 over payload

    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_data_source_type_url UNIQUE (type, url)
);

-- optional index helpers
CREATE INDEX IF NOT EXISTS idx_data_source_enabled ON data_source(enabled);
CREATE INDEX IF NOT EXISTS idx_data_source_type ON data_source(type);
CREATE INDEX IF NOT EXISTS idx_data_source_country ON data_source(country_code);
CREATE INDEX IF NOT EXISTS idx_data_source_priority ON data_source(priority);


-- =========================================================
-- 1. M3U playlist root
-- =========================================================
CREATE TABLE m3u_playlist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    global_attributes JSONB,              -- key=value pairs from M3U header (url-tvg, x-tvg-url, etc.)
    data_source_id UUID REFERENCES data_source(id) ON DELETE SET NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_m3u_playlist_source ON m3u_playlist(data_source_id);

-- =========================================================
-- 2. M3U items (each EXTINF + media URL)
-- =========================================================
CREATE TABLE m3u_item (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    playlist_id     UUID REFERENCES m3u_playlist(id) ON DELETE CASCADE,
    duration_sec    BIGINT,               -- store Duration in seconds (-1 for stream)
    title           TEXT,
    url             TEXT,                 -- media URI
    tvg_id          TEXT,
    tvg_name        TEXT,
    tvg_logo        TEXT,
    group_title     TEXT,
    attributes      JSONB,                -- tvg-id, group-title, tvg-logo, language, etc.
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_m3u_item_playlist ON m3u_item(playlist_id);
CREATE INDEX IF NOT EXISTS idx_m3u_item_tvg_id      ON m3u_item (tvg_id);
CREATE INDEX IF NOT EXISTS idx_m3u_item_group_title ON m3u_item (group_title);
CREATE INDEX IF NOT EXISTS idx_m3u_item_url ON m3u_item(url);

-- =========================================================
-- 3. EPG TV root info
-- =========================================================
CREATE TABLE tv (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    generator_info_name TEXT,
    generator_info_url  TEXT,
    data_source_id UUID REFERENCES data_source(id) ON DELETE SET NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tv_source ON tv(data_source_id);

-- =========================================================
-- 4. Channel info
-- =========================================================
CREATE TABLE channel (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id          TEXT,          -- XML <channel id="...">
    tv_id               UUID REFERENCES tv(id) ON DELETE CASCADE,
    display_name        TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_channel_tv_id ON channel(tv_id);

-- =========================================================
-- 5. Programme entries (EPG schedule)
-- =========================================================
CREATE TABLE programme (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id      UUID REFERENCES channel(id) ON DELETE CASCADE,
    start_time      TIMESTAMP,                      -- parsed from "start"
    stop_time       TIMESTAMP,                      -- parsed from "stop"
    title           TEXT,
    description     TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_programme_channel ON programme(channel_id);
CREATE INDEX IF NOT EXISTS idx_programme_start_stop ON programme(start_time, stop_time);


-- =========================================================
-- 6. (Optional) EpisodeNumber – if you later re-enable it
-- =========================================================
CREATE TABLE episode_number (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    programme_id    UUID REFERENCES programme(id) ON DELETE CASCADE,
    system          TEXT,
    value           TEXT
);

-- m3u_item  <—>  channel
CREATE TABLE IF NOT EXISTS m3u_item_channel_map (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    m3u_item_id     UUID NOT NULL REFERENCES m3u_item(id) ON DELETE CASCADE,
    channel_id      UUID NOT NULL REFERENCES channel(id) ON DELETE CASCADE,

    is_manual       BOOLEAN NOT NULL DEFAULT FALSE,  -- TRUE = curated; skip auto-updates
    confidence      NUMERIC(5,4),                    -- e.g. 0.0000 .. 1.0000 for auto matches
    method          TEXT,                            -- e.g. "jaroWinkler", "cosine", "admin"

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_item_unique UNIQUE (m3u_item_id)   -- one active mapping per item
);

-- helpful indexes
CREATE INDEX IF NOT EXISTS idx_map_item ON m3u_item_channel_map(m3u_item_id);
CREATE INDEX IF NOT EXISTS idx_map_channel ON m3u_item_channel_map(channel_id);
CREATE INDEX IF NOT EXISTS idx_map_manual ON m3u_item_channel_map(is_manual);