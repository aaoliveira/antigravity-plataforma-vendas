-- =============================================================================
-- Migration: V15_1__postgresql_rls_connected_account_metrics.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Métricas de Contas Conectadas (P018-BE)
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

ALTER TABLE connected_account_metrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE connected_account_metrics FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_connected_account_metrics ON connected_account_metrics;
CREATE POLICY tenant_isolation_connected_account_metrics ON connected_account_metrics
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
