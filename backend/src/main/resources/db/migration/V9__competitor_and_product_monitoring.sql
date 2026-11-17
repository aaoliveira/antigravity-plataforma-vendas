-- V9: Competitor Intelligence & Product Monitoring (Watchlists, Snapshots, Diff Engine, Gaps, Alert Rules & Deduplication)

-- 1. Watchlists de Produtos e Concorrentes
CREATE TABLE IF NOT EXISTS monitoring_watchlists (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL DEFAULT 'MIXED',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_monitoring_watchlists_tenant ON monitoring_watchlists(tenant_id);

-- 2. Itens das Watchlists (Anúncios ou Sellers)
CREATE TABLE IF NOT EXISTS monitoring_watchlist_items (
    id UUID PRIMARY KEY,
    watchlist_id UUID NOT NULL REFERENCES monitoring_watchlists(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    external_id VARCHAR(150) NOT NULL,
    target_name VARCHAR(500) NOT NULL,
    target_url TEXT,
    seller_nickname VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    check_frequency_minutes INTEGER NOT NULL DEFAULT 360,
    last_collected_at TIMESTAMPTZ,
    last_status VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_watchlist_items_unique UNIQUE (watchlist_id, marketplace, item_type, external_id)
);

CREATE INDEX IF NOT EXISTS idx_monitoring_items_tenant ON monitoring_watchlist_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_monitoring_items_lookup ON monitoring_watchlist_items(marketplace, external_id);

-- 3. Snapshots Periódicos de Monitoramento
CREATE TABLE IF NOT EXISTS monitoring_snapshots (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES monitoring_watchlist_items(id) ON DELETE RESTRICT,
    marketplace VARCHAR(50) NOT NULL,
    external_id VARCHAR(150) NOT NULL,
    price NUMERIC(15, 2),
    original_price NUMERIC(15, 2),
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    listing_status VARCHAR(50) NOT NULL,
    title VARCHAR(500),
    seller_nickname VARCHAR(255),
    seller_reputation VARCHAR(100),
    free_shipping BOOLEAN,
    thumbnail_url TEXT,
    collection_status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    failure_reason TEXT,
    source VARCHAR(100) NOT NULL,
    provenance VARCHAR(50) NOT NULL DEFAULT 'OBSERVED',
    collected_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_monitoring_snapshots_tenant ON monitoring_snapshots(tenant_id);
CREATE INDEX IF NOT EXISTS idx_monitoring_snapshots_timeline ON monitoring_snapshots(tenant_id, item_id, collected_at);

-- 4. Eventos de Alteração / Diffs Detectados
CREATE TABLE IF NOT EXISTS monitoring_change_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES monitoring_watchlist_items(id) ON DELETE CASCADE,
    snapshot_id UUID REFERENCES monitoring_snapshots(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    previous_value TEXT,
    current_value TEXT,
    price_delta_amount NUMERIC(15, 2),
    price_delta_percentage NUMERIC(7, 4),
    event_hash VARCHAR(64) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_monitoring_change_event_hash UNIQUE (tenant_id, event_hash)
);

CREATE INDEX IF NOT EXISTS idx_monitoring_events_item ON monitoring_change_events(item_id, detected_at);

-- 5. Regras de Alerta de Monitoramento
CREATE TABLE IF NOT EXISTS monitoring_alert_rules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    watchlist_id UUID REFERENCES monitoring_watchlists(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    min_price_delta_percentage NUMERIC(7, 4),
    direction VARCHAR(20) NOT NULL DEFAULT 'ANY',
    cooldown_minutes INTEGER NOT NULL DEFAULT 1440,
    channel VARCHAR(50) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_monitoring_rules_tenant ON monitoring_alert_rules(tenant_id);

-- 6. Alertas Disparados e Deduplicados
CREATE TABLE IF NOT EXISTS monitoring_alerts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    rule_id UUID REFERENCES monitoring_alert_rules(id) ON DELETE CASCADE,
    item_id UUID REFERENCES monitoring_watchlist_items(id) ON DELETE CASCADE,
    event_id UUID REFERENCES monitoring_change_events(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(50) NOT NULL DEFAULT 'INFO',
    dedup_key VARCHAR(128) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    triggered_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_monitoring_alerts_dedup UNIQUE (tenant_id, dedup_key)
);

CREATE INDEX IF NOT EXISTS idx_monitoring_alerts_tenant ON monitoring_alerts(tenant_id, is_read);
