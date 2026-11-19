-- =============================================================================
-- Migration: V15__connected_account_metrics.sql
-- Description: Tabela de métricas oficiais de contas conectadas e anúncios (P018-BE)
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

CREATE TABLE IF NOT EXISTS connected_account_metrics (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    seller_account_id UUID NOT NULL REFERENCES seller_accounts(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    listing_id UUID REFERENCES marketplace_listings(id) ON DELETE SET NULL,
    external_listing_id VARCHAR(100),
    target_identifier VARCHAR(100) NOT NULL DEFAULT '__ACCOUNT__',
    metric_scope VARCHAR(30) NOT NULL,
    granularity VARCHAR(30) NOT NULL,
    window_start TIMESTAMP WITH TIME ZONE NOT NULL,
    window_end TIMESTAMP WITH TIME ZONE NOT NULL,
    metric_key VARCHAR(100) NOT NULL,
    metric_value NUMERIC(15, 4),
    metric_unit VARCHAR(30) NOT NULL,
    provenance VARCHAR(30) NOT NULL,
    formula_definition VARCHAR(500),
    denominator_metric VARCHAR(100),
    denominator_value NUMERIC(15, 4),
    numerator_metric VARCHAR(100),
    numerator_value NUMERIC(15, 4),
    availability_status VARCHAR(30) NOT NULL,
    unavailability_reason VARCHAR(500),
    source_endpoint VARCHAR(255) NOT NULL,
    ingested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_connected_account_metrics UNIQUE (tenant_id, seller_account_id, marketplace, target_identifier, metric_key, granularity, window_start, window_end)
);

CREATE INDEX IF NOT EXISTS idx_connected_account_metrics_tenant_acc 
    ON connected_account_metrics(tenant_id, seller_account_id);

CREATE INDEX IF NOT EXISTS idx_connected_account_metrics_tenant_window 
    ON connected_account_metrics(tenant_id, window_start, window_end);

CREATE INDEX IF NOT EXISTS idx_connected_account_metrics_tenant_key 
    ON connected_account_metrics(tenant_id, metric_key);

CREATE INDEX IF NOT EXISTS idx_connected_account_metrics_tenant_status 
    ON connected_account_metrics(tenant_id, availability_status);
