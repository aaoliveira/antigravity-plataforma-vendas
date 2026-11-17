-- V7: Motor Financeiro Determinístico, Custos de SKU, Regras Versionadas e Cálculos Fechados com Isolamento Multi-Tenant

-- 1. Tabela de Custos de SKU com Vigência e Histórico Temporal
CREATE TABLE IF NOT EXISTS sku_costs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    sku VARCHAR(120) NOT NULL,
    cost_price NUMERIC(15, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    unit VARCHAR(20) NOT NULL DEFAULT 'UN',
    origin VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sku_costs_tenant ON sku_costs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sku_costs_tenant_sku_dates ON sku_costs(tenant_id, sku, effective_from DESC);

-- 2. Tabela de Conjuntos de Regras Financeiras Versionadas
CREATE TABLE IF NOT EXISTS financial_rule_sets (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    company_id UUID REFERENCES companies(id) ON DELETE SET NULL,
    store_id UUID REFERENCES stores(id) ON DELETE SET NULL,
    marketplace VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_financial_rules_tenant_name_version UNIQUE (tenant_id, name, version)
);

CREATE INDEX IF NOT EXISTS idx_financial_rule_sets_tenant ON financial_rule_sets(tenant_id);
CREATE INDEX IF NOT EXISTS idx_financial_rule_sets_tenant_active ON financial_rule_sets(tenant_id, is_active);
CREATE INDEX IF NOT EXISTS idx_financial_rule_sets_tenant_marketplace ON financial_rule_sets(tenant_id, marketplace);

-- 3. Componentes das Regras Financeiras
CREATE TABLE IF NOT EXISTS financial_rule_components (
    id UUID PRIMARY KEY,
    rule_set_id UUID NOT NULL REFERENCES financial_rule_sets(id) ON DELETE CASCADE,
    component_type VARCHAR(60) NOT NULL,
    component_value NUMERIC(15, 4) NOT NULL,
    threshold_amount NUMERIC(15, 2),
    applies_to VARCHAR(30) NOT NULL DEFAULT 'ITEM',
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_financial_rule_components_rule ON financial_rule_components(rule_set_id);

-- 4. Cálculos Financeiros Consolidados de Pedidos
CREATE TABLE IF NOT EXISTS order_financial_calculations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    rule_set_id UUID REFERENCES financial_rule_sets(id) ON DELETE SET NULL,
    rule_version INTEGER NOT NULL,
    calculation_status VARCHAR(50) NOT NULL,
    calculation_quality VARCHAR(50) NOT NULL,
    gross_revenue NUMERIC(15, 2) NOT NULL,
    net_revenue NUMERIC(15, 2) NOT NULL,
    total_cogs NUMERIC(15, 2) NOT NULL,
    total_marketplace_fees NUMERIC(15, 2) NOT NULL,
    total_shipping_seller NUMERIC(15, 2) NOT NULL,
    total_taxes NUMERIC(15, 2) NOT NULL,
    total_packaging NUMERIC(15, 2) NOT NULL,
    total_other_costs NUMERIC(15, 2) NOT NULL,
    contribution_margin_value NUMERIC(15, 2) NOT NULL,
    contribution_margin_percentage NUMERIC(7, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    missing_fields TEXT,
    assumptions TEXT,
    calculated_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ,
    recalculated_at TIMESTAMPTZ,
    recalculation_reason TEXT,
    CONSTRAINT uk_order_calc_tenant_order UNIQUE (tenant_id, order_id)
);

CREATE INDEX IF NOT EXISTS idx_order_financial_calc_tenant ON order_financial_calculations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_order_financial_calc_order ON order_financial_calculations(order_id);
CREATE INDEX IF NOT EXISTS idx_order_financial_calc_status ON order_financial_calculations(tenant_id, calculation_status);

-- 5. Cálculos Financeiros Detalhados por Item do Pedido (com Memória)
CREATE TABLE IF NOT EXISTS order_item_financial_calculations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    order_calculation_id UUID NOT NULL REFERENCES order_financial_calculations(id) ON DELETE CASCADE,
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    sku VARCHAR(120),
    unit_price NUMERIC(15, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    total_price NUMERIC(15, 2) NOT NULL,
    unit_cogs NUMERIC(15, 4),
    total_cogs NUMERIC(15, 2) NOT NULL,
    marketplace_fee NUMERIC(15, 2) NOT NULL,
    shipping_seller NUMERIC(15, 2) NOT NULL,
    tax_value NUMERIC(15, 2) NOT NULL,
    packaging_cost NUMERIC(15, 2) NOT NULL,
    other_costs NUMERIC(15, 2) NOT NULL,
    contribution_margin_value NUMERIC(15, 2) NOT NULL,
    contribution_margin_percentage NUMERIC(7, 4) NOT NULL,
    calculation_quality VARCHAR(50) NOT NULL,
    memory_breakdown TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_item_calc_tenant ON order_item_financial_calculations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_order_item_calc_parent ON order_item_financial_calculations(order_calculation_id);
CREATE INDEX IF NOT EXISTS idx_order_item_calc_item ON order_item_financial_calculations(order_item_id);
