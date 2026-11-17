-- V6: Market Intelligence — Pesquisa de Mercado, Snapshots, Itens Observados, Buscas Salvas e Favoritos com Isolamento Multi-Tenant

-- 1. Snapshots de Pesquisas de Mercado
CREATE TABLE IF NOT EXISTS market_search_snapshots (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    query VARCHAR(255) NOT NULL,
    category_filter VARCHAR(100),
    min_price NUMERIC(15, 2),
    max_price NUMERIC(15, 2),
    condition_filter VARCHAR(50),
    brand_filter VARCHAR(100),
    sort_order VARCHAR(50),
    total_results INTEGER NOT NULL DEFAULT 0,
    collected_at TIMESTAMPTZ NOT NULL,
    provenance VARCHAR(50) NOT NULL DEFAULT 'OBSERVED',
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_market_snapshots_tenant ON market_search_snapshots(tenant_id);
CREATE INDEX IF NOT EXISTS idx_market_snapshots_mkt_query ON market_search_snapshots(marketplace, query);

-- 2. Itens Observados do Mercado nos Snapshots
CREATE TABLE IF NOT EXISTS market_product_items (
    id UUID PRIMARY KEY,
    snapshot_id UUID REFERENCES market_search_snapshots(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    external_item_id VARCHAR(150) NOT NULL,
    title VARCHAR(500) NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    condition VARCHAR(50),
    seller_nickname VARCHAR(255),
    seller_reputation VARCHAR(100),
    permalink TEXT,
    thumbnail_url TEXT,
    category_id VARCHAR(100),
    category_name VARCHAR(255),
    brand VARCHAR(255),
    free_shipping BOOLEAN,
    review_score NUMERIC(3, 2),
    review_count INTEGER,
    estimated_sales_range VARCHAR(100),
    estimation_method TEXT,
    provenance VARCHAR(50) NOT NULL DEFAULT 'OBSERVED',
    collected_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_market_product_items_snapshot ON market_product_items(snapshot_id);
CREATE INDEX IF NOT EXISTS idx_market_product_items_ext_id ON market_product_items(marketplace, external_item_id);

-- 3. Buscas de Mercado Salvas pelo Usuário (Listas de Oportunidades)
CREATE TABLE IF NOT EXISTS saved_searches (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    marketplace VARCHAR(50) NOT NULL,
    query VARCHAR(255) NOT NULL,
    category_filter VARCHAR(100),
    min_price NUMERIC(15, 2),
    max_price NUMERIC(15, 2),
    condition_filter VARCHAR(50),
    brand_filter VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_saved_searches_tenant ON saved_searches(tenant_id);

-- 4. Produtos de Mercado Favoritados/Salvos com Notas de Análise
CREATE TABLE IF NOT EXISTS saved_market_products (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    external_item_id VARCHAR(150) NOT NULL,
    title VARCHAR(500) NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    seller_nickname VARCHAR(255),
    permalink TEXT,
    thumbnail_url TEXT,
    notes TEXT,
    provenance VARCHAR(50) NOT NULL DEFAULT 'OBSERVED',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_saved_market_products_tenant_item UNIQUE (tenant_id, marketplace, external_item_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_market_products_tenant ON saved_market_products(tenant_id);
