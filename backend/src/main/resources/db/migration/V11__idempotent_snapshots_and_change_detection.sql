-- =============================================================================
-- Migration: V11__idempotent_snapshots_and_change_detection.sql
-- Description: Campos de idempotência, proveniência detalhada, validade, status de coleta e detecção de alterações
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64);
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS source_url TEXT;
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS source_type VARCHAR(50) DEFAULT 'OFFICIAL_API';
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS valid_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS collection_status VARCHAR(50) DEFAULT 'SUCCESS';
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS previous_snapshot_id UUID;
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS has_price_change BOOLEAN DEFAULT FALSE;
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS price_change_amount NUMERIC(15, 4) DEFAULT 0.0000;
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS price_change_percent NUMERIC(8, 4) DEFAULT 0.0000;
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS has_availability_change BOOLEAN DEFAULT FALSE;
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS has_attribute_change BOOLEAN DEFAULT FALSE;
ALTER TABLE monitored_item_snapshots ADD COLUMN IF NOT EXISTS change_type VARCHAR(50) DEFAULT 'INITIAL';

-- Unique constraint for tenant idempotency
CREATE UNIQUE INDEX IF NOT EXISTS uq_snapshots_tenant_idempotency
    ON monitored_item_snapshots (tenant_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_snapshots_tenant_item_idempotency
    ON monitored_item_snapshots (tenant_id, item_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_snapshots_prev
    ON monitored_item_snapshots (tenant_id, previous_snapshot_id);
