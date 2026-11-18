-- =============================================================================
-- Migration: V7_1__postgresql_rls_market_intelligence.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Observações de Mercado e Matches
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- 1. market_observations
ALTER TABLE market_observations ENABLE ROW LEVEL SECURITY;
ALTER TABLE market_observations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_market_observations ON market_observations;
CREATE POLICY tenant_isolation_market_observations ON market_observations
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 2. product_matches
ALTER TABLE product_matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_matches FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_product_matches ON product_matches;
CREATE POLICY tenant_isolation_product_matches ON product_matches
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
