-- =============================================================================
-- Migration: V6__orders_and_items.sql
-- Description: Tabelas de Ingestão de Pedidos, Itens, Checkpoints, Outbox e DLQ
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

-- 1. Pedidos de Venda Próprios (Orders)
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    seller_account_id UUID NOT NULL REFERENCES seller_accounts(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    external_order_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    subtotal NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    shipping_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    buyer_name VARCHAR(255),
    buyer_document VARCHAR(50),
    tracking_number VARCHAR(100),
    cancellation_reason VARCHAR(255),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    returned_at TIMESTAMP WITH TIME ZONE,
    raw_payload_json TEXT,
    schema_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    origin VARCHAR(100) NOT NULL,
    order_created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    synced_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_tenant_mkt_extid UNIQUE (tenant_id, marketplace, external_order_id)
);

CREATE INDEX IF NOT EXISTS idx_orders_tenant_status ON orders (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_account ON orders (tenant_id, seller_account_id);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_created ON orders (tenant_id, order_created_at);

-- 2. Itens do Pedido (Order Items)
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    product_variant_id UUID REFERENCES product_variants(id) ON DELETE SET NULL,
    external_listing_id VARCHAR(100),
    external_item_id VARCHAR(100),
    sku VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    unit_price NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    quantity INTEGER NOT NULL DEFAULT 1,
    total_item_price NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_items_tenant_order ON order_items (tenant_id, order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_tenant_product ON order_items (tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_order_items_tenant_sku ON order_items (tenant_id, sku);

-- 3. Checkpoints de Sincronização de Pedidos (Order Sync Checkpoints)
CREATE TABLE IF NOT EXISTS order_sync_checkpoints (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    seller_account_id UUID NOT NULL REFERENCES seller_accounts(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    sync_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    cursor_token VARCHAR(255),
    last_offset INTEGER NOT NULL DEFAULT 0,
    page_size INTEGER NOT NULL DEFAULT 50,
    total_orders_synced INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_checkpoint_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    CONSTRAINT uk_order_chkpt_tenant_acc_mkt UNIQUE (tenant_id, seller_account_id, marketplace)
);

CREATE INDEX IF NOT EXISTS idx_order_chkpt_tenant_status ON order_sync_checkpoints (tenant_id, sync_status);

-- 4. Transactional Outbox para Eventos de Pedidos (Order Outbox Events)
CREATE TABLE IF NOT EXISTS order_outbox_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL DEFAULT 'ORDER',
    aggregate_id UUID NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_outbox_tenant_status ON order_outbox_events (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON order_outbox_events (tenant_id, aggregate_type, aggregate_id);

-- 5. Dead-Letter Queue para Falhas de Ingestão de Pedidos (Order DLQ Records)
CREATE TABLE IF NOT EXISTS order_dlq_records (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    seller_account_id UUID NOT NULL REFERENCES seller_accounts(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    external_order_id VARCHAR(100),
    raw_payload_json TEXT,
    error_reason TEXT NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'UNRESOLVED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dlq_tenant_status ON order_dlq_records (tenant_id, status);
