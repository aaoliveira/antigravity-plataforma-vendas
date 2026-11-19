-- =============================================================================
-- Migration: V16_1__postgresql_rls_ai_gateway.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Auditoria de IA (P019-BE)
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

ALTER TABLE ai_usage_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_usage_audits FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_ai_usage_audits ON ai_usage_audits;
CREATE POLICY tenant_isolation_ai_usage_audits ON ai_usage_audits
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
