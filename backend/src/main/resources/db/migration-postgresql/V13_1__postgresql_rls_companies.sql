-- =============================================================================
-- Migration: V13_1__postgresql_rls_companies.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Empresas e Vínculos (P016-BE)
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- 1. companies
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE companies FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_companies ON companies;
CREATE POLICY tenant_isolation_companies ON companies
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 2. company_seller_account_links
ALTER TABLE company_seller_account_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_seller_account_links FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_company_seller_account_links ON company_seller_account_links;
CREATE POLICY tenant_isolation_company_seller_account_links ON company_seller_account_links
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
