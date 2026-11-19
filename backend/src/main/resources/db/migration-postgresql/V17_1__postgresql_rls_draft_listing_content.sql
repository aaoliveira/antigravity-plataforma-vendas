-- =============================================================================
-- Migration: V17_1__postgresql_rls_draft_listing_content.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Rascunhos de Anúncios (P020-BE)
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

ALTER TABLE draft_listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE draft_listings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_draft_listings ON draft_listings;
CREATE POLICY tenant_isolation_draft_listings ON draft_listings
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);


ALTER TABLE draft_listing_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE draft_listing_versions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_draft_listing_versions ON draft_listing_versions;
CREATE POLICY tenant_isolation_draft_listing_versions ON draft_listing_versions
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
