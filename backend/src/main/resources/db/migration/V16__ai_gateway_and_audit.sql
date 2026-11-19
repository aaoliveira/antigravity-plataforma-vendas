-- =============================================================================
-- Migration: V16__ai_gateway_and_audit.sql
-- Description: Tabela de auditoria e governança de IA, tokens, latência e custos (P019-BE)
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

CREATE TABLE IF NOT EXISTS ai_usage_audits (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID,
    provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_key VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(20) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    estimated_cost_brl NUMERIC(10, 6) NOT NULL DEFAULT 0.000000,
    tools_invoked VARCHAR(500),
    injection_detected BOOLEAN NOT NULL DEFAULT FALSE,
    injection_threat_type VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    error_code VARCHAR(100),
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_audits_tenant_created 
    ON ai_usage_audits(tenant_id, created_at);

CREATE INDEX IF NOT EXISTS idx_ai_audits_tenant_prompt 
    ON ai_usage_audits(tenant_id, prompt_key);

CREATE INDEX IF NOT EXISTS idx_ai_audits_tenant_status 
    ON ai_usage_audits(tenant_id, status);
