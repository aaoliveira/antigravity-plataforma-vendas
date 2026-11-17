-- V12__marketplace_reconciliation_and_managerial_dre.sql
-- Tabela de Repasses Consolidados do Marketplace (Extratos / Settlement Batches)
CREATE TABLE IF NOT EXISTS reconciliation_settlements (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    store_id UUID,
    marketplace VARCHAR(50) NOT NULL,
    external_settlement_id VARCHAR(100) NOT NULL,
    source_filename VARCHAR(255),
    settlement_date TIMESTAMPTZ NOT NULL,
    gross_amount NUMERIC(15, 2) NOT NULL,
    net_amount NUMERIC(15, 2) NOT NULL,
    total_fees NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_shipping NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_adjustments NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    status VARCHAR(50) NOT NULL DEFAULT 'IMPORTED',
    hash_fingerprint VARCHAR(64) NOT NULL,
    total_items_count INT NOT NULL DEFAULT 0,
    reconciled_items_count INT NOT NULL DEFAULT 0,
    discrepant_items_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_settlement_idempotency UNIQUE (tenant_id, marketplace, external_settlement_id),
    CONSTRAINT fk_settlement_tenant FOREIGN KEY (tenant_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_settlement_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE SET NULL
);

-- Tabela de Lançamentos / Itens Individuais do Repasse
CREATE TABLE IF NOT EXISTS reconciliation_settlement_items (
    id UUID PRIMARY KEY,
    settlement_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    external_order_id VARCHAR(100),
    external_transaction_id VARCHAR(100),
    order_id UUID,
    entry_type VARCHAR(50) NOT NULL,
    expected_amount NUMERIC(15, 2),
    settled_amount NUMERIC(15, 2) NOT NULL,
    discrepancy_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    reconciliation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_MATCH',
    confidence_score NUMERIC(5, 4) NOT NULL DEFAULT 0.0000,
    matching_method VARCHAR(50) NOT NULL DEFAULT 'UNMATCHED',
    discrepancy_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_settlement_item_settlement FOREIGN KEY (settlement_id) REFERENCES reconciliation_settlements(id) ON DELETE CASCADE,
    CONSTRAINT fk_settlement_item_tenant FOREIGN KEY (tenant_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_settlement_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
);

-- Tabela de Gestão de Contestações / Protocolos de Atendimento
CREATE TABLE IF NOT EXISTS reconciliation_disputes (
    id UUID PRIMARY KEY,
    settlement_item_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    protocol_number VARCHAR(100) NOT NULL,
    marketplace VARCHAR(50) NOT NULL,
    disputed_amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    channel VARCHAR(50) NOT NULL DEFAULT 'HELP_CENTER',
    notes TEXT,
    disclaimer TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_dispute_settlement_item FOREIGN KEY (settlement_item_id) REFERENCES reconciliation_settlement_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_dispute_tenant FOREIGN KEY (tenant_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- Tabela de Trilha de Auditoria Imutável de Ajustes Manuais
CREATE TABLE IF NOT EXISTS reconciliation_audit_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    settlement_item_id UUID NOT NULL,
    user_id UUID,
    action VARCHAR(50) NOT NULL,
    previous_state TEXT,
    new_state TEXT,
    justification TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_settlement_item FOREIGN KEY (settlement_item_id) REFERENCES reconciliation_settlement_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Índices de Performance e Isolamento Multi-Tenant
CREATE INDEX IF NOT EXISTS idx_settlements_tenant_marketplace ON reconciliation_settlements(tenant_id, marketplace, settlement_date);
CREATE INDEX IF NOT EXISTS idx_settlement_items_settlement ON reconciliation_settlement_items(settlement_id);
CREATE INDEX IF NOT EXISTS idx_settlement_items_tenant_status ON reconciliation_settlement_items(tenant_id, reconciliation_status);
CREATE INDEX IF NOT EXISTS idx_settlement_items_order ON reconciliation_settlement_items(tenant_id, external_order_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_disputes_tenant ON reconciliation_disputes(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_reconciliation_audit_tenant ON reconciliation_audit_logs(tenant_id, created_at);
