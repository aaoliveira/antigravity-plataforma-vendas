-- =============================================================================
-- Migration: V7__market_observations_and_matching.sql
-- Description: Tabelas de Observações de Mercado, Proveniência e Matching Explicável
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

-- 1. Observações de Mercado (Market Observations)
CREATE TABLE IF NOT EXISTS market_observations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    observed_listing_id VARCHAR(100) NOT NULL,
    marketplace VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    price NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    item_condition VARCHAR(50) NOT NULL DEFAULT 'NEW',
    seller_identifier VARCHAR(255),
    source_type VARCHAR(50) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    method VARCHAR(100) NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL DEFAULT 1.0000,
    methodology_description TEXT,
    model_version VARCHAR(50),
    time_window VARCHAR(50),
    gtin VARCHAR(50),
    brand VARCHAR(100),
    sales_volume NUMERIC(15, 4),
    visits_count BIGINT,
    ctr NUMERIC(8, 4),
    revenue NUMERIC(15, 4),
    minimized_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_obs_tenant_mkt_listing UNIQUE (tenant_id, marketplace, observed_listing_id)
);

CREATE INDEX IF NOT EXISTS idx_obs_tenant_marketplace ON market_observations (tenant_id, marketplace);
CREATE INDEX IF NOT EXISTS idx_obs_tenant_source_type ON market_observations (tenant_id, source_type);
CREATE INDEX IF NOT EXISTS idx_obs_tenant_gtin ON market_observations (tenant_id, gtin);
CREATE INDEX IF NOT EXISTS idx_obs_tenant_collected ON market_observations (tenant_id, collected_at);

-- 2. Correspondência de Produtos e Observações com Score Explicável (Product Matches)
CREATE TABLE IF NOT EXISTS product_matches (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    canonical_product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    observation_id UUID NOT NULL REFERENCES market_observations(id) ON DELETE CASCADE,
    overall_score NUMERIC(5, 4) NOT NULL,
    gtin_score NUMERIC(5, 4),
    title_score NUMERIC(5, 4) NOT NULL,
    brand_score NUMERIC(5, 4),
    price_score NUMERIC(5, 4),
    match_status VARCHAR(50) NOT NULL,
    explanation TEXT NOT NULL,
    threshold_applied NUMERIC(5, 4) NOT NULL DEFAULT 0.7500,
    matched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_match_tenant_prod_obs UNIQUE (tenant_id, canonical_product_id, observation_id)
);

CREATE INDEX IF NOT EXISTS idx_matches_tenant_product ON product_matches (tenant_id, canonical_product_id);
CREATE INDEX IF NOT EXISTS idx_matches_tenant_observation ON product_matches (tenant_id, observation_id);
CREATE INDEX IF NOT EXISTS idx_matches_tenant_status ON product_matches (tenant_id, match_status);
