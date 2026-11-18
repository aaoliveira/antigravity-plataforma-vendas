-- =============================================================================
-- Migration: V5_1__postgresql_rls_catalog.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Catálogo
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- 1. seller_accounts
ALTER TABLE seller_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE seller_accounts FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_seller_accounts ON seller_accounts;
CREATE POLICY tenant_isolation_seller_accounts ON seller_accounts
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 2. products
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE products FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_products ON products;
CREATE POLICY tenant_isolation_products ON products
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 3. product_variants
ALTER TABLE product_variants ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_variants FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_product_variants ON product_variants;
CREATE POLICY tenant_isolation_product_variants ON product_variants
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 4. marketplace_listings
ALTER TABLE marketplace_listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_listings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_marketplace_listings ON marketplace_listings;
CREATE POLICY tenant_isolation_marketplace_listings ON marketplace_listings
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 5. external_identifier_mappings
ALTER TABLE external_identifier_mappings ENABLE ROW LEVEL SECURITY;
ALTER TABLE external_identifier_mappings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_ext_mappings ON external_identifier_mappings;
CREATE POLICY tenant_isolation_ext_mappings ON external_identifier_mappings
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 6. listing_sync_checkpoints
ALTER TABLE listing_sync_checkpoints ENABLE ROW LEVEL SECURITY;
ALTER TABLE listing_sync_checkpoints FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_sync_checkpoints ON listing_sync_checkpoints;
CREATE POLICY tenant_isolation_sync_checkpoints ON listing_sync_checkpoints
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- Garante privilégios explícitos para a role seller_app
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'seller_app') THEN
        GRANT ALL ON TABLE seller_accounts TO seller_app;
        GRANT ALL ON TABLE products TO seller_app;
        GRANT ALL ON TABLE product_variants TO seller_app;
        GRANT ALL ON TABLE marketplace_listings TO seller_app;
        GRANT ALL ON TABLE external_identifier_mappings TO seller_app;
        GRANT ALL ON TABLE listing_sync_checkpoints TO seller_app;
    END IF;
END
$$;
