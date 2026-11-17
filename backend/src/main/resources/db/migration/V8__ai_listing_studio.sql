-- V8: AI Listing Studio — Análise Explicável de Anúncios, Histórico de Versões e Auditoria de IA
-- Suporte aos requisitos FR-LST-001, FR-LST-002, FR-LST-003, FR-LST-008 e FR-LST-011

-- 1. Tabela de Versões e Rascunhos de Anúncios (Human Review & Version Control)
CREATE TABLE IF NOT EXISTS listing_versions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    listing_id UUID REFERENCES listings(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    version_number INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    attributes_json TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    source VARCHAR(50) NOT NULL DEFAULT 'AI_GENERATED',
    prompt_version VARCHAR(50),
    model_name VARCHAR(100),
    created_by VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_listing_versions_tenant_listing_ver UNIQUE (tenant_id, listing_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_listing_versions_tenant ON listing_versions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_listing_versions_listing ON listing_versions(listing_id);
CREATE INDEX IF NOT EXISTS idx_listing_versions_tenant_listing ON listing_versions(tenant_id, listing_id);

-- 2. Tabela de Auditorias de Qualidade de Anúncio (Explainable Quality Audit)
CREATE TABLE IF NOT EXISTS listing_quality_audits (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    overall_score INTEGER NOT NULL,
    title_score INTEGER NOT NULL,
    description_score INTEGER NOT NULL,
    attributes_score INTEGER NOT NULL,
    images_score INTEGER NOT NULL,
    criteria_breakdown_json TEXT NOT NULL,
    actionable_recommendations_json TEXT NOT NULL,
    missing_required_attributes_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_listing_audits_tenant ON listing_quality_audits(tenant_id);
CREATE INDEX IF NOT EXISTS idx_listing_audits_listing ON listing_quality_audits(listing_id);
CREATE INDEX IF NOT EXISTS idx_listing_audits_tenant_listing ON listing_quality_audits(tenant_id, listing_id);

-- 3. Tabela de Logs de Geração de IA e Rastreabilidade
CREATE TABLE IF NOT EXISTS ai_generation_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id VARCHAR(150),
    listing_id UUID REFERENCES listings(id) ON DELETE SET NULL,
    prompt_type VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    latency_ms BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_code VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_generation_logs_tenant ON ai_generation_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_generation_logs_created_at ON ai_generation_logs(tenant_id, created_at);
