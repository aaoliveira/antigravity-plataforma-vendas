-- =============================================================================
-- Migration: V17__draft_listing_content.sql
-- Description: Tabelas de rascunhos de anúncios gerados por IA e versionamento (P020-BE)
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

CREATE TABLE IF NOT EXISTS draft_listings (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    product_variant_id UUID REFERENCES product_variants(id) ON DELETE SET NULL,
    target_marketplace VARCHAR(50) NOT NULL,
    current_version_number INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_draft_listings_tenant_status 
    ON draft_listings(tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_draft_listings_tenant_marketplace 
    ON draft_listings(tenant_id, target_marketplace);

CREATE INDEX IF NOT EXISTS idx_draft_listings_tenant_product 
    ON draft_listings(tenant_id, product_id);


CREATE TABLE IF NOT EXISTS draft_listing_versions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    draft_listing_id UUID NOT NULL REFERENCES draft_listings(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    prompt_template_id VARCHAR(100) NOT NULL,
    prompt_template_version VARCHAR(50) NOT NULL,
    ai_model_name VARCHAR(100) NOT NULL,
    ai_provider VARCHAR(50) NOT NULL,
    tone VARCHAR(50) NOT NULL,
    target_marketplace VARCHAR(50) NOT NULL,
    input_attributes_json TEXT NOT NULL,
    generated_title VARCHAR(255) NOT NULL,
    generated_description TEXT NOT NULL,
    reviewed_title VARCHAR(255),
    reviewed_description TEXT,
    traceable_attributes_json TEXT NOT NULL,
    unproven_claims_json TEXT,
    title_char_count INT NOT NULL,
    description_char_count INT NOT NULL,
    character_limit_valid BOOLEAN NOT NULL,
    user_edited BOOLEAN NOT NULL DEFAULT FALSE,
    review_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_draft_versions_ver UNIQUE (tenant_id, draft_listing_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_draft_versions_tenant_draft 
    ON draft_listing_versions(tenant_id, draft_listing_id);

CREATE INDEX IF NOT EXISTS idx_draft_versions_tenant_created 
    ON draft_listing_versions(tenant_id, created_at);
