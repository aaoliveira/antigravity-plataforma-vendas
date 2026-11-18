-- =============================================================================
-- Migration: V2__identity_and_tenancy.sql
-- Description: Mínimo seguro de usuários, memberships de tenant e eventos de auditoria
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

CREATE TABLE IF NOT EXISTS app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS memberships (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_memberships_user_tenant UNIQUE (user_id, tenant_id)
);

CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    principal_id UUID,
    principal_email VARCHAR(255),
    tenant_id UUID,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500),
    correlation_id VARCHAR(100),
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_tenant_id ON memberships(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_tenant_id ON audit_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events(created_at);

-- Inserção de Organizações de Teste / Desenvolvimento Inicial
INSERT INTO organizations (id, name, slug, status, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'Alpha Seller Org', 'alpha-seller', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'Beta Store Org', 'beta-store', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Inserção de Usuários de Teste / Desenvolvimento Inicial
-- Senha padrão para os usuários de seed: "Password@123"
-- Hash gerado com BCrypt (10 rounds): $2a$10$oZOrqxhfQZRZWgIpxjx32ecDk/F1e4PWveuUiet3RNhLD.Bha7mV2
INSERT INTO app_users (id, email, password_hash, full_name, status, created_at, updated_at)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'seller@example.com', '$2a$10$oZOrqxhfQZRZWgIpxjx32ecDk/F1e4PWveuUiet3RNhLD.Bha7mV2', 'Alpha Seller Admin', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'competitor@example.com', '$2a$10$oZOrqxhfQZRZWgIpxjx32ecDk/F1e4PWveuUiet3RNhLD.Bha7mV2', 'Beta Seller Admin', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'orphan@example.com', '$2a$10$oZOrqxhfQZRZWgIpxjx32ecDk/F1e4PWveuUiet3RNhLD.Bha7mV2', 'Orphan User Without Tenant', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Inserção de Memberships
INSERT INTO memberships (id, user_id, tenant_id, role, status, created_at, updated_at)
VALUES
    ('aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'OWNER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', 'OWNER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
