-- =============================================================================
-- Migration: V9_1__postgresql_rls_listing_comparison.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Comparações e Histórico de Preços
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- 1. listing_price_history
ALTER TABLE listing_price_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE listing_price_history FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_listing_price_history ON listing_price_history;
CREATE POLICY tenant_isolation_listing_price_history ON listing_price_history
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 2. listing_comparisons
ALTER TABLE listing_comparisons ENABLE ROW LEVEL SECURITY;
ALTER TABLE listing_comparisons FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_listing_comparisons ON listing_comparisons;
CREATE POLICY tenant_isolation_listing_comparisons ON listing_comparisons
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
