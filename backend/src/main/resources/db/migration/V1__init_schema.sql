-- =============================================================================
-- Migration: V1__init_schema.sql
-- Description: Estrutura inicial de governança, organizações e metadados
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_metadata (
    metadata_key VARCHAR(100) PRIMARY KEY,
    metadata_value VARCHAR(255) NOT NULL,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Inserção de metadados da baseline de engenharia
INSERT INTO app_metadata (metadata_key, metadata_value, description, updated_at)
VALUES 
    ('schema_version', '1.0.0', 'Versão inicial do schema de governança modular', CURRENT_TIMESTAMP),
    ('architecture_pattern', 'MODULAR_MONOLITH', 'Monólito modular com Spring Modulith e ArchUnit', CURRENT_TIMESTAMP),
    ('multi_tenancy_mode', 'ROW_LEVEL_SECURITY', 'Isolamento por tenant_id e RLS', CURRENT_TIMESTAMP);
