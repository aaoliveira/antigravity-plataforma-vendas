-- =============================================================================
-- Migration: V18_1__postgresql_rls_async_export.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Exportações e Auditoria (P021-BE)
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

ALTER TABLE export_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE export_jobs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_export_jobs ON export_jobs;
CREATE POLICY tenant_isolation_export_jobs ON export_jobs
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);


ALTER TABLE export_audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE export_audit_logs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_export_audit_logs ON export_audit_logs;
CREATE POLICY tenant_isolation_export_audit_logs ON export_audit_logs
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
