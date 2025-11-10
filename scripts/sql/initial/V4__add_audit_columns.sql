-- Add missing audit columns so all entities can inherit AuditEntity

-- m3u_item: add updated_at
ALTER TABLE IF EXISTS m3u_item
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- channel: add updated_at
ALTER TABLE IF EXISTS channel
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- programme: add updated_at
ALTER TABLE IF EXISTS programme
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- episode_number: add created_at and updated_at
ALTER TABLE IF EXISTS episode_number
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

