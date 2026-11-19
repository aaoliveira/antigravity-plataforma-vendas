-- =============================================================================
-- Migration: V12__product_costs_and_fee_rules.sql
-- Description: Tabelas de cadastro de custos por produto/variante/empresa e regras de taxas/comissões
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

-- 1. Tabela de Custos de Produtos e Variantes (product_costs)
CREATE TABLE IF NOT EXISTS product_costs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    variant_id UUID,
    company_id UUID,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    unit VARCHAR(20) NOT NULL DEFAULT 'UN',
    acquisition_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    packaging_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    additional_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    subsidized_shipping_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    tax_rate_percent NUMERIC(8, 4) NOT NULL DEFAULT 0.0000,
    other_expenses_percent NUMERIC(8, 4) NOT NULL DEFAULT 0.0000,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_product_costs_tenant_product 
    ON product_costs (tenant_id, product_id);

CREATE INDEX IF NOT EXISTS idx_product_costs_lookup 
    ON product_costs (tenant_id, product_id, variant_id, company_id, effective_from, effective_to);

-- 2. Tabela de Regras de Tarifas e Comissões de Marketplaces (marketplace_fee_rules)
CREATE TABLE IF NOT EXISTS marketplace_fee_rules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    marketplace VARCHAR(50) NOT NULL,
    listing_type VARCHAR(50) NOT NULL,
    category_id VARCHAR(100),
    fee_type VARCHAR(50) NOT NULL,
    fee_value_type VARCHAR(50) NOT NULL,
    percentage_rate NUMERIC(8, 4),
    fixed_amount NUMERIC(15, 4),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    min_price_threshold NUMERIC(15, 4),
    max_price_threshold NUMERIC(15, 4),
    source_type VARCHAR(50) NOT NULL,
    source_reference TEXT NOT NULL,
    legal_disclaimer TEXT,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_fee_rules_tenant_marketplace 
    ON marketplace_fee_rules (tenant_id, marketplace);

CREATE INDEX IF NOT EXISTS idx_fee_rules_lookup 
    ON marketplace_fee_rules (tenant_id, marketplace, listing_type, fee_type, effective_from, effective_to);
