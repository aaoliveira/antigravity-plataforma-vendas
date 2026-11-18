-- =============================================================================
-- Migration: V10_1__postgresql_rls_product_monitoring.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Monitoramento de Produtos
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- 1. monitoring_groups
ALTER TABLE monitoring_groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE monitoring_groups FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_monitoring_groups ON monitoring_groups;
CREATE POLICY tenant_isolation_monitoring_groups ON monitoring_groups
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 2. monitored_items
ALTER TABLE monitored_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE monitored_items FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_monitored_items ON monitored_items;
CREATE POLICY tenant_isolation_monitored_items ON monitored_items
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 3. monitored_item_snapshots
ALTER TABLE monitored_item_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE monitored_item_snapshots FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_monitored_item_snapshots ON monitored_item_snapshots;
CREATE POLICY tenant_isolation_monitored_item_snapshots ON monitored_item_snapshots
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 4. monitoring_job_executions
ALTER TABLE monitoring_job_executions ENABLE ROW LEVEL SECURITY;
ALTER TABLE monitoring_job_executions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_monitoring_job_executions ON monitoring_job_executions;
CREATE POLICY tenant_isolation_monitoring_job_executions ON monitoring_job_executions
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
