-- V11: Advanced Product Discovery (PDF Supplier Catalogs, Structured Extraction, Human Review & Similarity Matching)

-- 1. Catálogos de Fornecedores Enviados
CREATE TABLE IF NOT EXISTS discovery_catalogs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    supplier_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    total_items INTEGER NOT NULL DEFAULT 0,
    reviewed_items INTEGER NOT NULL DEFAULT 0,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_discovery_catalogs_tenant ON discovery_catalogs(tenant_id, status);

-- 2. Itens Extraídos de Catálogos (com Confiança por Campo e Estado de Revisão)
CREATE TABLE IF NOT EXISTS discovery_catalog_items (
    id UUID PRIMARY KEY,
    catalog_id UUID NOT NULL REFERENCES discovery_catalogs(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(500) NOT NULL,
    supplier_sku VARCHAR(150),
    ean VARCHAR(50),
    description TEXT,
    cost_price NUMERIC(15, 2),
    currency VARCHAR(10) NOT NULL DEFAULT 'BRL',
    weight_kg NUMERIC(10, 3),
    dimensions_cm VARCHAR(100),
    extraction_method VARCHAR(50) NOT NULL DEFAULT 'PDF_TEXT',
    overall_confidence NUMERIC(5, 4) NOT NULL DEFAULT 1.0000,
    confidence_details TEXT,
    review_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_REVIEW',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_discovery_catalog_items_tenant ON discovery_catalog_items(tenant_id, review_status);
CREATE INDEX IF NOT EXISTS idx_discovery_catalog_items_catalog ON discovery_catalog_items(catalog_id);

-- 3. Candidatos de Correspondência por Similaridade (Sem Correspondência Automática Definitiva)
CREATE TABLE IF NOT EXISTS discovery_product_matches (
    id UUID PRIMARY KEY,
    catalog_item_id UUID NOT NULL REFERENCES discovery_catalog_items(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    matched_external_id VARCHAR(150) NOT NULL,
    matched_title VARCHAR(500) NOT NULL,
    matched_price NUMERIC(15, 2),
    matched_url TEXT,
    similarity_score NUMERIC(5, 4) NOT NULL,
    match_source VARCHAR(50) NOT NULL DEFAULT 'SEMANTIC_VECTOR',
    status VARCHAR(50) NOT NULL DEFAULT 'CANDIDATE',
    estimated_gross_margin NUMERIC(7, 4),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_discovery_matches_item ON discovery_product_matches(catalog_item_id);
CREATE INDEX IF NOT EXISTS idx_discovery_matches_tenant ON discovery_product_matches(tenant_id, status);

-- 4. Buscas Visuais por Imagem de Produto
CREATE TABLE IF NOT EXISTS discovery_visual_searches (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    stored_image_path VARCHAR(500) NOT NULL,
    image_mime_type VARCHAR(100) NOT NULL,
    search_query_description VARCHAR(500),
    top_matches_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_discovery_visual_searches_tenant ON discovery_visual_searches(tenant_id);
