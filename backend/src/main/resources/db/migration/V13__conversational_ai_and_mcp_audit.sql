-- =============================================================================
-- Migration V13: Conversational AI, MCP Read-Only Server and Tool Execution Audit
-- Context: Prompt 18 (FR-AI-001 to FR-AI-007, FR-MCP, ADR DEC-023)
-- =============================================================================

-- Tabela de Chaves de Acesso MCP por Tenant
CREATE TABLE IF NOT EXISTS mcp_api_keys (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(128) NOT NULL UNIQUE,
    key_prefix VARCHAR(16) NOT NULL,
    permissions VARCHAR(255) NOT NULL DEFAULT 'READ_ONLY',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID,
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mcp_key_tenant FOREIGN KEY (tenant_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mcp_key_tenant ON mcp_api_keys(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mcp_key_hash ON mcp_api_keys(key_hash);

-- Tabela de Auditoria Imutável de Execução de Ferramentas MCP
CREATE TABLE IF NOT EXISTS mcp_tool_execution_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID,
    api_key_id UUID,
    tool_name VARCHAR(100) NOT NULL,
    arguments_sanitized TEXT NOT NULL,
    execution_status VARCHAR(50) NOT NULL,
    latency_ms BIGINT NOT NULL,
    model_name VARCHAR(100),
    prompt_version VARCHAR(50),
    result_summary TEXT,
    provenance_source VARCHAR(100),
    provenance_classification VARCHAR(50),
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mcp_log_tenant FOREIGN KEY (tenant_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_mcp_log_api_key FOREIGN KEY (api_key_id) REFERENCES mcp_api_keys(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_mcp_log_tenant ON mcp_tool_execution_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mcp_log_tool ON mcp_tool_execution_logs(tool_name);
CREATE INDEX IF NOT EXISTS idx_mcp_log_created_at ON mcp_tool_execution_logs(created_at DESC);

-- Tabela de Sessões de Chat Conversacional
CREATE TABLE IF NOT EXISTS conversational_sessions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conv_session_tenant FOREIGN KEY (tenant_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_conv_session_tenant ON conversational_sessions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_conv_session_user ON conversational_sessions(user_id);

-- Tabela de Mensagens do Chat Conversacional com Metadados e Ferramentas
CREATE TABLE IF NOT EXISTS conversational_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    tool_calls TEXT,
    provenance_metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_conv_message_session FOREIGN KEY (session_id) REFERENCES conversational_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_conv_message_tenant FOREIGN KEY (tenant_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_conv_message_session ON conversational_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_conv_message_tenant ON conversational_messages(tenant_id);
