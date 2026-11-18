-- =============================================================================
-- Migration: V3__tenant_scoped_entities.sql
-- Description: Tabelas tenant-scoped com FKs estritas e índices compostos
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

CREATE TABLE IF NOT EXISTS marketplace_connections (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    seller_account_id VARCHAR(100) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    encrypted_access_token TEXT,
    encrypted_refresh_token TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_conn_tenant_marketplace_account UNIQUE (tenant_id, marketplace, seller_account_id)
);

CREATE TABLE IF NOT EXISTS tenant_documents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conn_tenant_status ON marketplace_connections (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_conn_tenant_marketplace ON marketplace_connections (tenant_id, marketplace);
CREATE INDEX IF NOT EXISTS idx_docs_tenant_created ON tenant_documents (tenant_id, created_at);
