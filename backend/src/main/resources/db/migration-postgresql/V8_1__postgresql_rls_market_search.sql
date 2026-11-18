-- =============================================================================
-- Migration: V8_1__postgresql_rls_market_search.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Pesquisa de Mercado
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- 1. market_search_queries
ALTER TABLE market_search_queries ENABLE ROW LEVEL SECURITY;
ALTER TABLE market_search_queries FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_market_search_queries ON market_search_queries;
CREATE POLICY tenant_isolation_market_search_queries ON market_search_queries
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 2. market_search_cache
ALTER TABLE market_search_cache ENABLE ROW LEVEL SECURITY;
ALTER TABLE market_search_cache FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_market_search_cache ON market_search_cache;
CREATE POLICY tenant_isolation_market_search_cache ON market_search_cache
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
