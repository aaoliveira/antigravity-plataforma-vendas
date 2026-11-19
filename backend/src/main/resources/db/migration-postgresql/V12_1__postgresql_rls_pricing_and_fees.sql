-- =============================================================================
-- Migration: V12_1__postgresql_rls_pricing_and_fees.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Custos e Taxas (P014-BE)
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- 1. product_costs
ALTER TABLE product_costs ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_costs FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_product_costs ON product_costs;
CREATE POLICY tenant_isolation_product_costs ON product_costs
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 2. marketplace_fee_rules
ALTER TABLE marketplace_fee_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_fee_rules FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_marketplace_fee_rules ON marketplace_fee_rules;
CREATE POLICY tenant_isolation_marketplace_fee_rules ON marketplace_fee_rules
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
