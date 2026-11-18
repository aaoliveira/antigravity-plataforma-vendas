-- =============================================================================
-- Migration: V5__catalog_products_and_listings.sql
-- Description: Tabelas de Catálogo Canônico, Anúncios Próprios, Mapeamentos e Checkpoints
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

-- 1. Contas de Vendedor por Marketplace (Seller Accounts)
CREATE TABLE IF NOT EXISTS seller_accounts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    external_account_id VARCHAR(100) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_seller_acc_tenant_mkt_ext UNIQUE (tenant_id, marketplace, external_account_id)
);

CREATE INDEX IF NOT EXISTS idx_seller_acc_tenant_status ON seller_accounts (tenant_id, status);

-- 2. Produtos Canônicos Internos (Master Products)
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    canonical_sku VARCHAR(100),
    brand VARCHAR(100),
    category VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_tenant_sku UNIQUE (tenant_id, canonical_sku)
);

CREATE INDEX IF NOT EXISTS idx_product_tenant_status ON products (tenant_id, status);

-- 3. Variantes de Produto Canônico (Product Variants)
CREATE TABLE IF NOT EXISTS product_variants (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_sku VARCHAR(100),
    gtin_barcode VARCHAR(50),
    attributes_json TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_variant_tenant_sku UNIQUE (tenant_id, variant_sku)
);

CREATE INDEX IF NOT EXISTS idx_variant_tenant_product ON product_variants (tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_variant_tenant_gtin ON product_variants (tenant_id, gtin_barcode);

-- 4. Anúncios em Marketplaces (Marketplace Listings)
CREATE TABLE IF NOT EXISTS marketplace_listings (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    seller_account_id UUID NOT NULL REFERENCES seller_accounts(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    product_variant_id UUID REFERENCES product_variants(id) ON DELETE SET NULL,
    marketplace VARCHAR(50) NOT NULL,
    external_listing_id VARCHAR(100) NOT NULL,
    external_sku VARCHAR(100),
    external_variation_id VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    price NUMERIC(15, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    available_quantity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    permalink VARCHAR(500),
    raw_payload_json TEXT,
    schema_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    origin VARCHAR(100) NOT NULL,
    synced_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_listing_tenant_mkt_extid UNIQUE (tenant_id, marketplace, external_listing_id)
);

CREATE INDEX IF NOT EXISTS idx_listing_tenant_product ON marketplace_listings (tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_listing_tenant_account ON marketplace_listings (tenant_id, seller_account_id);
CREATE INDEX IF NOT EXISTS idx_listing_tenant_status ON marketplace_listings (tenant_id, status);

-- 5. Mapeamentos de Identificadores Externos
CREATE TABLE IF NOT EXISTS external_identifier_mappings (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    external_id_type VARCHAR(50) NOT NULL,
    external_id_value VARCHAR(100) NOT NULL,
    internal_entity_type VARCHAR(50) NOT NULL,
    internal_entity_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ext_map_tenant_mkt_type_val UNIQUE (tenant_id, marketplace, external_id_type, external_id_value)
);

CREATE INDEX IF NOT EXISTS idx_ext_map_internal ON external_identifier_mappings (tenant_id, internal_entity_type, internal_entity_id);

-- 6. Checkpoints de Sincronização Paginada (Listing Sync Checkpoints)
CREATE TABLE IF NOT EXISTS listing_sync_checkpoints (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    seller_account_id UUID NOT NULL REFERENCES seller_accounts(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    sync_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    last_offset INTEGER NOT NULL DEFAULT 0,
    page_size INTEGER NOT NULL DEFAULT 50,
    total_items_synced INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_checkpoint_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    CONSTRAINT uk_chkpt_tenant_acc_mkt UNIQUE (tenant_id, seller_account_id, marketplace)
);

CREATE INDEX IF NOT EXISTS idx_chkpt_tenant_status ON listing_sync_checkpoints (tenant_id, sync_status);
