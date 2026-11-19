-- =============================================================================
-- Migration: V14__financial_facts_and_reconciliation.sql
-- Description: Tabelas de fatos financeiros por pedido e por item, reconciliação e trilha de cálculo (P017-BE)
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

-- 1. Tabela de Fatos Financeiros por Pedido (order_financial_facts)
CREATE TABLE IF NOT EXISTS order_financial_facts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id),
    seller_account_id UUID NOT NULL REFERENCES seller_accounts(id),
    company_id UUID REFERENCES companies(id),
    marketplace VARCHAR(50) NOT NULL,
    external_order_id VARCHAR(100) NOT NULL,
    order_status VARCHAR(50) NOT NULL,
    order_date TIMESTAMP WITH TIME ZONE NOT NULL,
    gross_revenue NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    discount_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    net_revenue NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    cancellation_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    return_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    marketplace_fee NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    fixed_fee NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    shipping_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    product_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    packaging_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    additional_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    tax_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    other_expenses_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_deductions NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    contribution_margin_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    contribution_margin_ratio NUMERIC(8, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    reconciliation_status VARCHAR(30) NOT NULL DEFAULT 'ESTIMATED',
    fee_provenance VARCHAR(30) NOT NULL DEFAULT 'ESTIMATED',
    shipping_provenance VARCHAR(30) NOT NULL DEFAULT 'ESTIMATED',
    cost_provenance VARCHAR(30) NOT NULL DEFAULT 'USER_INFORMED',
    tax_provenance VARCHAR(30) NOT NULL DEFAULT 'USER_INFORMED',
    has_missing_data BOOLEAN NOT NULL DEFAULT FALSE,
    missing_data_reasons TEXT,
    calculation_version VARCHAR(20) NOT NULL DEFAULT 'v1.0.0',
    calculation_trace_json TEXT,
    settlement_external_id VARCHAR(100),
    settlement_date TIMESTAMP WITH TIME ZONE,
    settlement_notes TEXT,
    reconciled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_order_financial_facts_tenant_order UNIQUE (tenant_id, order_id)
);

CREATE INDEX IF NOT EXISTS idx_order_financial_facts_lookup 
    ON order_financial_facts (tenant_id, order_date, marketplace);

CREATE INDEX IF NOT EXISTS idx_order_financial_facts_reconciliation 
    ON order_financial_facts (tenant_id, reconciliation_status);

CREATE INDEX IF NOT EXISTS idx_order_financial_facts_company 
    ON order_financial_facts (tenant_id, company_id);

-- 2. Tabela de Fatos Financeiros por Item de Pedido (order_item_financial_facts)
CREATE TABLE IF NOT EXISTS order_item_financial_facts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    order_fact_id UUID NOT NULL REFERENCES order_financial_facts(id) ON DELETE CASCADE,
    order_id UUID NOT NULL,
    order_item_id UUID NOT NULL REFERENCES order_items(id),
    product_id UUID,
    variant_id UUID,
    sku VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    gross_revenue NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    discount_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    net_revenue NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    cancellation_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    return_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    marketplace_fee NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    fixed_fee NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    shipping_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    unit_product_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    product_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    packaging_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    additional_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    tax_rate_percent NUMERIC(8, 4) NOT NULL DEFAULT 0.0000,
    tax_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    other_expenses_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_deductions NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    contribution_margin_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    contribution_margin_ratio NUMERIC(8, 4) NOT NULL DEFAULT 0.0000,
    cost_provenance VARCHAR(30) NOT NULL DEFAULT 'USER_INFORMED',
    has_missing_data BOOLEAN NOT NULL DEFAULT FALSE,
    missing_data_reasons TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_order_item_financial_facts_tenant_item UNIQUE (tenant_id, order_item_id)
);

CREATE INDEX IF NOT EXISTS idx_order_item_financial_facts_fact 
    ON order_item_financial_facts (tenant_id, order_fact_id);

CREATE INDEX IF NOT EXISTS idx_order_item_financial_facts_order 
    ON order_item_financial_facts (tenant_id, order_id);
