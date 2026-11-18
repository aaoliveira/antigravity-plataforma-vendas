-- =============================================================================
-- Migration: V3_1__postgresql_rls_defense.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- Habilita e Força RLS em marketplace_connections
ALTER TABLE marketplace_connections ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_connections FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_connections ON marketplace_connections;
CREATE POLICY tenant_isolation_connections ON marketplace_connections
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- Habilita e Força RLS em tenant_documents
ALTER TABLE tenant_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_documents FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_documents ON tenant_documents;
CREATE POLICY tenant_isolation_documents ON tenant_documents
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
