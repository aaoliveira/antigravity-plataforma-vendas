-- =============================================================================
-- Migration: V8__market_search_and_policies.sql
-- Description: Políticas de Fontes Autorizadas, Histórico de Pesquisa e Cache com TTL
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

-- 1. Políticas de Fontes de Pesquisa e Conformidade (Global Registry)
CREATE TABLE IF NOT EXISTS market_search_source_policies (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    marketplace VARCHAR(50) NOT NULL,
    capability VARCHAR(50) NOT NULL,
    capability_status VARCHAR(50) NOT NULL,
    authorization_requirement VARCHAR(100) NOT NULL,
    endpoint_pattern VARCHAR(500) NOT NULL,
    documentation_url VARCHAR(1000) NOT NULL,
    rate_limit_per_minute INT NOT NULL DEFAULT 60,
    allows_competitor_search BOOLEAN NOT NULL DEFAULT FALSE,
    policy_rules TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_search_policy_marketplace ON market_search_source_policies (marketplace);
CREATE INDEX IF NOT EXISTS idx_mkt_search_policy_status ON market_search_source_policies (capability_status);

-- Seed de Políticas de Fontes Homologadas
INSERT INTO market_search_source_policies (
    id, name, marketplace, capability, capability_status, authorization_requirement,
    endpoint_pattern, documentation_url, rate_limit_per_minute, allows_competitor_search,
    policy_rules, is_active
) VALUES 
(
    'POL-MLB-PUBLIC-01',
    'Mercado Livre Pesquisa Pública e Catálogo',
    'MERCADO_LIVRE',
    'MARKET_SEARCH',
    'SUPPORTED',
    'PUBLIC_RATE_LIMITED',
    'https://api.mercadolibre.com/sites/MLB/search',
    'https://developers.mercadolibre.com.br/pt_br/pesquisa-de-produtos',
    60,
    TRUE,
    'Uso estrito de endpoints públicos permitidos. Scraping hostil e extração automatizada de vendas unitárias ou dados privados de concorrentes são estritamente vedados pelos ToS.',
    TRUE
),
(
    'POL-SHOPEE-RESTRICTED-01',
    'Shopee Open Platform Product Search',
    'SHOPEE',
    'MARKET_SEARCH',
    'RESTRICTED',
    'HMAC_PARTNER_APP',
    'https://partner.shopeemobile.com/api/v2/product/search_item',
    'https://open.shopee.com/documents/v2/v2.product.search_item',
    10,
    FALSE,
    'Pesquisa aberta e scraping de concorrentes são expressamente vedados pelas diretrizes da Shopee Open Platform. Somente itens autorizados e catálogo do seller são consultáveis.',
    TRUE
),
(
    'POL-AMAZON-UNVERIFIED-01',
    'Amazon SP-API Pricing & Catalog',
    'AMAZON',
    'MARKET_SEARCH',
    'UNVERIFIED',
    'LWA_IAM_CREDENTIALS',
    'https://sellingpartnerapi-na.amazon.com/catalog/2022-04-01/items',
    'https://developer-docs.amazon.com/sp-api',
    0,
    FALSE,
    'Conector e escopos da Amazon SP-API em fase de homologação (UNVERIFIED). Todas as chamadas de produção permanecem bloqueadas via política Fail-Closed.',
    TRUE
),
(
    'POL-OWN-CATALOG-01',
    'Catálogo Próprio Canônico e Anúncios do Tenant',
    'INTERNAL',
    'READ_OWN_CATALOG',
    'SUPPORTED',
    'TENANT_INTERNAL_SESSION',
    'internal://catalog-service',
    'internal://docs/catalog-and-listings',
    300,
    FALSE,
    'Consulta restrita e isolada ao inventário e anúncios cadastrados e sincronizados pelo próprio tenant autenticado.',
    TRUE
),
(
    'POL-LICENSED-PROVIDER-01',
    'Provedor Homologado de Dados e Benchmarking de Mercado',
    'LICENSED_PROVIDER',
    'MARKET_SEARCH',
    'SUPPORTED',
    'LICENSED_DATA_KEY',
    'https://api.licensed-provider.internal/v1/search',
    'https://sellerintelligence.com/compliance/licensed-data',
    120,
    TRUE,
    'Base de dados agregada e licenciada comercialmente para inteligência de mercado, em conformidade com as leis de proteção de dados e direitos autorais.',
    TRUE
);

-- 2. Histórico e Trilha de Auditoria de Pesquisas de Mercado (Tenant-Scoped)
CREATE TABLE IF NOT EXISTS market_search_queries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    query_text VARCHAR(255) NOT NULL,
    marketplace VARCHAR(50),
    category VARCHAR(100),
    min_price NUMERIC(15, 4),
    max_price NUMERIC(15, 4),
    only_own_catalog BOOLEAN NOT NULL DEFAULT FALSE,
    source_policy_id VARCHAR(100) REFERENCES market_search_source_policies(id),
    capability_status VARCHAR(50) NOT NULL,
    result_count INT NOT NULL DEFAULT 0,
    execution_time_ms BIGINT NOT NULL DEFAULT 0,
    cached BOOLEAN NOT NULL DEFAULT FALSE,
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mkt_search_queries_tenant_date ON market_search_queries (tenant_id, executed_at DESC);
CREATE INDEX IF NOT EXISTS idx_mkt_search_queries_tenant_query ON market_search_queries (tenant_id, query_text);

-- 3. Cache de Resultados de Pesquisa com TTL (Tenant-Scoped)
CREATE TABLE IF NOT EXISTS market_search_cache (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    cache_key VARCHAR(255) NOT NULL,
    source_policy_id VARCHAR(100) REFERENCES market_search_source_policies(id),
    results_payload_json TEXT NOT NULL,
    result_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mkt_search_cache_tenant_key UNIQUE (tenant_id, cache_key)
);

CREATE INDEX IF NOT EXISTS idx_mkt_search_cache_tenant_key ON market_search_cache (tenant_id, cache_key);
CREATE INDEX IF NOT EXISTS idx_mkt_search_cache_tenant_expires ON market_search_cache (tenant_id, expires_at);
