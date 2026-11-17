-- V5: Sincronização de Catálogo, Anúncios, Pedidos, Jobs e Webhooks com Isolamento Multi-Tenant

-- 1. Tabela de Produtos / SKUs próprios
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    sku VARCHAR(120) NOT NULL,
    title VARCHAR(500) NOT NULL,
    brand VARCHAR(255),
    category VARCHAR(255),
    barcode VARCHAR(100),
    cost_price NUMERIC(15, 2),
    provenance VARCHAR(50) NOT NULL DEFAULT 'IMPORTED',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_products_tenant_store_sku UNIQUE (tenant_id, store_id, sku)
);

CREATE INDEX IF NOT EXISTS idx_products_tenant ON products(tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_store ON products(store_id);
CREATE INDEX IF NOT EXISTS idx_products_tenant_sku ON products(tenant_id, sku);

-- 2. Tabela de Anúncios / Listings próprios sincronizados do marketplace
CREATE TABLE IF NOT EXISTS listings (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    marketplace VARCHAR(50) NOT NULL,
    external_listing_id VARCHAR(150) NOT NULL,
    sku VARCHAR(120),
    title VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    available_quantity INTEGER,
    sold_quantity INTEGER,
    listing_type VARCHAR(100),
    permalink TEXT,
    thumbnail_url TEXT,
    provenance VARCHAR(50) NOT NULL DEFAULT 'IMPORTED',
    raw_payload TEXT,
    last_synced_at TIMESTAMPTZ,
    origin_updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_listings_tenant_marketplace_ext_id UNIQUE (tenant_id, marketplace, external_listing_id)
);

CREATE INDEX IF NOT EXISTS idx_listings_tenant ON listings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_listings_store ON listings(store_id);
CREATE INDEX IF NOT EXISTS idx_listings_ext_id ON listings(marketplace, external_listing_id);

-- 3. Tabela de Pedidos / Orders próprios sincronizados do marketplace
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    external_order_id VARCHAR(150) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    order_date TIMESTAMPTZ NOT NULL,
    buyer_nickname VARCHAR(255),
    shipping_id VARCHAR(150),
    shipping_cost_seller NUMERIC(15, 2),
    marketplace_fee NUMERIC(15, 2),
    provenance VARCHAR(50) NOT NULL DEFAULT 'IMPORTED',
    raw_payload TEXT,
    origin_updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_orders_tenant_marketplace_ext_id UNIQUE (tenant_id, marketplace, external_order_id)
);

CREATE INDEX IF NOT EXISTS idx_orders_tenant ON orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_store ON orders(store_id);
CREATE INDEX IF NOT EXISTS idx_orders_tenant_date ON orders(tenant_id, order_date DESC);

-- 4. Itens do Pedido / Order Items
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    external_item_id VARCHAR(150),
    sku VARCHAR(120),
    title VARCHAR(500) NOT NULL,
    unit_price NUMERIC(15, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    sale_fee NUMERIC(15, 2),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_tenant ON order_items(tenant_id);

-- 5. Jobs de Sincronização Assíncronos
CREATE TABLE IF NOT EXISTS sync_jobs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    items_processed INTEGER NOT NULL DEFAULT 0,
    items_succeeded INTEGER NOT NULL DEFAULT 0,
    items_failed INTEGER NOT NULL DEFAULT 0,
    cursor TEXT,
    error_summary TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sync_jobs_tenant ON sync_jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_sync_jobs_store ON sync_jobs(store_id);
CREATE INDEX IF NOT EXISTS idx_sync_jobs_status ON sync_jobs(status);

-- 6. Tabela de Webhooks com Deduplicação e Proteção contra Replay
CREATE TABLE IF NOT EXISTS webhook_events (
    id UUID PRIMARY KEY,
    tenant_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    marketplace VARCHAR(50) NOT NULL,
    external_event_id VARCHAR(255) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    CONSTRAINT uk_webhook_events_marketplace_ext_id UNIQUE (marketplace, external_event_id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_events_mkt_ext ON webhook_events(marketplace, external_event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_events_status ON webhook_events(status);
