-- =============================================================================
-- Migration: V4__oauth_connection_fields.sql
-- Description: Campos adicionais de expiração e escopos para conexões OAuth
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

ALTER TABLE marketplace_connections ADD COLUMN IF NOT EXISTS token_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE marketplace_connections ADD COLUMN IF NOT EXISTS authorized_scopes VARCHAR(255);
ALTER TABLE marketplace_connections ADD COLUMN IF NOT EXISTS last_refreshed_at TIMESTAMP WITH TIME ZONE;
