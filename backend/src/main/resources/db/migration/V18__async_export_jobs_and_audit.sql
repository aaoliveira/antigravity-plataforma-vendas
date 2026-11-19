-- =============================================================================
-- Migration: V18__async_export_jobs_and_audit.sql
-- Description: Tabelas de exportação assíncrona com storage namespaced e auditoria (P021-BE)
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

CREATE TABLE IF NOT EXISTS export_jobs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    requested_by VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(120),
    dataset VARCHAR(50) NOT NULL,
    file_format VARCHAR(10) NOT NULL,
    filter_params_json TEXT NOT NULL DEFAULT '{}',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    max_rows INT NOT NULL DEFAULT 10000,
    total_rows INT DEFAULT 0,
    is_truncated BOOLEAN NOT NULL DEFAULT FALSE,
    file_name VARCHAR(255),
    storage_path VARCHAR(500),
    file_size_bytes BIGINT DEFAULT 0,
    checksum_sha256 VARCHAR(64),
    download_token VARCHAR(120),
    expires_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_export_job_tenant_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS export_audit_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    export_job_id UUID REFERENCES export_jobs(id) ON DELETE SET NULL,
    action VARCHAR(60) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(300),
    details_json TEXT DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_export_jobs_tenant_status ON export_jobs (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_export_jobs_expires_at ON export_jobs (expires_at);
CREATE INDEX IF NOT EXISTS idx_export_audit_tenant_created ON export_audit_logs (tenant_id, created_at);
