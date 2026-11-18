-- =============================================================================
-- Migration: V9__listing_equivalence_and_comparison.sql
-- Description: Tabelas de Histórico Real de Preços e Comparações de Listings Equivalentes
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

-- 1. Histórico Real de Preços por Anúncio (Listing Price History)
-- Regra estrita: armazena exclusivamente observações legítimas sem pontos retroativos inventados
CREATE TABLE IF NOT EXISTS listing_price_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    external_listing_id VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    price NUMERIC(15, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    shipping_cost NUMERIC(15, 4),
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source_type VARCHAR(50) NOT NULL DEFAULT 'OFFICIAL',
    source_url VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_price_hist_tenant_mkt_listing 
    ON listing_price_history (tenant_id, marketplace, external_listing_id, observed_at);

CREATE INDEX IF NOT EXISTS idx_price_hist_tenant_observed 
    ON listing_price_history (tenant_id, observed_at);

-- 2. Registro e Auditoria de Comparações de Listings (Listing Comparisons)
CREATE TABLE IF NOT EXISTS listing_comparisons (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    base_listing_id VARCHAR(100),
    base_title VARCHAR(500) NOT NULL,
    base_brand VARCHAR(255),
    base_model VARCHAR(255),
    base_gtin VARCHAR(64),
    base_price NUMERIC(15, 4) NOT NULL,
    base_shipping_cost NUMERIC(15, 4),
    base_observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    base_marketplace VARCHAR(50) NOT NULL,
    target_listing_id VARCHAR(100),
    target_title VARCHAR(500) NOT NULL,
    target_brand VARCHAR(255),
    target_model VARCHAR(255),
    target_gtin VARCHAR(64),
    target_price NUMERIC(15, 4) NOT NULL,
    target_shipping_cost NUMERIC(15, 4),
    target_observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    target_marketplace VARCHAR(50) NOT NULL,
    target_seller_name VARCHAR(255),
    target_source_url VARCHAR(1000),
    similarity_score NUMERIC(5, 4) NOT NULL,
    gtin_score NUMERIC(5, 4) NOT NULL,
    brand_score NUMERIC(5, 4) NOT NULL,
    model_score NUMERIC(5, 4) NOT NULL,
    title_score NUMERIC(5, 4) NOT NULL,
    attributes_score NUMERIC(5, 4) NOT NULL,
    is_equivalent BOOLEAN NOT NULL,
    is_compatible BOOLEAN NOT NULL,
    compatibility_status VARCHAR(32) NOT NULL,
    incompatibility_reasons_json TEXT,
    price_variation_comparable BOOLEAN NOT NULL,
    price_difference NUMERIC(15, 4),
    price_variation_percentage NUMERIC(7, 4),
    time_delta_days BIGINT,
    explanation TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_listing_comp_tenant_created 
    ON listing_comparisons (tenant_id, created_at);

CREATE INDEX IF NOT EXISTS idx_listing_comp_base_listing 
    ON listing_comparisons (tenant_id, base_listing_id);
