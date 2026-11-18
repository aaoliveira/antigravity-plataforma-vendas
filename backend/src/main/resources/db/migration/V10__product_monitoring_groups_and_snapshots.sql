-- =============================================================================
-- Migration: V10__product_monitoring_groups_and_snapshots.sql
-- Description: Tabelas de Grupos de Monitoramento, Itens Monitorados e Snapshots Históricos
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

-- 1. Grupos de Monitoramento por Tenant
CREATE TABLE IF NOT EXISTS monitoring_groups (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(30) DEFAULT '#3B82F6',
    min_interval_minutes INT NOT NULL DEFAULT 60,
    retention_days INT NOT NULL DEFAULT 90,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_monitoring_groups_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX IF NOT EXISTS idx_monitoring_groups_tenant_status 
    ON monitoring_groups (tenant_id, status);

-- 2. Itens Monitorados Vinculados a Grupos e Fontes
CREATE TABLE IF NOT EXISTS monitored_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES monitoring_groups(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    source_policy_id VARCHAR(100) NOT NULL,
    external_listing_id VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    initial_price NUMERIC(15, 4),
    current_price NUMERIC(15, 4),
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    source_url VARCHAR(1000),
    seller_name VARCHAR(150),
    item_condition VARCHAR(30) DEFAULT 'NEW',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    monitoring_interval_minutes INT NOT NULL DEFAULT 60,
    last_snapshot_at TIMESTAMP WITH TIME ZONE,
    next_run_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_snapshot_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_monitored_item_group_listing UNIQUE (tenant_id, group_id, marketplace, external_listing_id)
);

CREATE INDEX IF NOT EXISTS idx_monitored_items_tenant_group 
    ON monitored_items (tenant_id, group_id, status);

CREATE INDEX IF NOT EXISTS idx_monitored_items_next_run 
    ON monitored_items (tenant_id, status, next_run_at);

-- 3. Snapshots Históricos Auditáveis (Histórico começa no primeiro snapshot após inclusão)
CREATE TABLE IF NOT EXISTS monitored_item_snapshots (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES monitored_items(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES monitoring_groups(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    external_listing_id VARCHAR(100) NOT NULL,
    price NUMERIC(15, 4) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    shipping_cost NUMERIC(15, 4),
    shipping_status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    available_quantity INT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    provenance_type VARCHAR(50) NOT NULL DEFAULT 'OBSERVED',
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_effective_snapshot BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_snapshots_tenant_item_observed 
    ON monitored_item_snapshots (tenant_id, item_id, observed_at);

CREATE INDEX IF NOT EXISTS idx_snapshots_tenant_group_observed 
    ON monitored_item_snapshots (tenant_id, group_id, observed_at);

-- 4. Registro e Auditoria de Execuções de Jobs de Monitoramento
CREATE TABLE IF NOT EXISTS monitoring_job_executions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    item_id UUID REFERENCES monitored_items(id) ON DELETE SET NULL,
    group_id UUID REFERENCES monitoring_groups(id) ON DELETE SET NULL,
    job_type VARCHAR(50) NOT NULL,
    marketplace VARCHAR(50) NOT NULL,
    source_policy_id VARCHAR(100) NOT NULL,
    execution_status VARCHAR(50) NOT NULL,
    skip_reason VARCHAR(255),
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_ms BIGINT
);

CREATE INDEX IF NOT EXISTS idx_job_exec_tenant_status 
    ON monitoring_job_executions (tenant_id, execution_status, executed_at);
