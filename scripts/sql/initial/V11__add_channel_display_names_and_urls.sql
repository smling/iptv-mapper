-- Support multiple display-name and url per channel (no metadata JSON)

CREATE TABLE IF NOT EXISTS channel_display_name (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
    lang TEXT,
    name TEXT NOT NULL,
    pos INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_channel_display_name_channel ON channel_display_name(channel_id);

CREATE TABLE IF NOT EXISTS channel_url (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES channel(id) ON DELETE CASCADE,
    system TEXT,
    url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_channel_url_channel ON channel_url(channel_id);

