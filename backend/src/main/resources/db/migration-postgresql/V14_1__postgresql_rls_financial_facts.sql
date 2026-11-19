-- =============================================================================
-- Migration: V14_1__postgresql_rls_financial_facts.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Fatos Financeiros (P017-BE)
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- 1. order_financial_facts
ALTER TABLE order_financial_facts ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_financial_facts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_order_financial_facts ON order_financial_facts;
CREATE POLICY tenant_isolation_order_financial_facts ON order_financial_facts
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 2. order_item_financial_facts
ALTER TABLE order_item_financial_facts ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_item_financial_facts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_order_item_financial_facts ON order_item_financial_facts;
CREATE POLICY tenant_isolation_order_item_financial_facts ON order_item_financial_facts
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
