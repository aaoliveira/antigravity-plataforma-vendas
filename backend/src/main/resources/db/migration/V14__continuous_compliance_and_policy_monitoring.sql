-- Flyway Migration: V14__continuous_compliance_and_policy_monitoring.sql
-- Module: Continuous Marketplace Compliance & Policy Monitoring Engine
-- Creates tables for policy registry, changes, incidents, API lifecycles, and safe reactivation.

CREATE TABLE IF NOT EXISTS marketplace_policies (
    id UUID PRIMARY KEY,
    marketplace VARCHAR(50) NOT NULL,
    policy_type VARCHAR(50) NOT NULL,
    policy_name VARCHAR(255) NOT NULL,
    official_url VARCHAR(1000) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    policy_version VARCHAR(50),
    effective_date TIMESTAMPTZ,
    last_checked_at TIMESTAMPTZ NOT NULL,
    last_changed_at TIMESTAMPTZ NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    raw_content TEXT,
    status VARCHAR(50) NOT NULL,
    risk_level VARCHAR(50) NOT NULL,
    affected_feature VARCHAR(100),
    evidence TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_policies_mkt_status ON marketplace_policies(marketplace, status);
CREATE INDEX idx_policies_type ON marketplace_policies(policy_type);
CREATE INDEX idx_policies_hash ON marketplace_policies(content_hash);

CREATE TABLE IF NOT EXISTS marketplace_policy_changes (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL REFERENCES marketplace_policies(id) ON DELETE CASCADE,
    marketplace VARCHAR(50) NOT NULL,
    source_url VARCHAR(1000) NOT NULL,
    change_type VARCHAR(50) NOT NULL,
    previous_content TEXT,
    current_content TEXT,
    previous_hash VARCHAR(64),
    current_hash VARCHAR(64),
    detected_at TIMESTAMPTZ NOT NULL,
    effective_date TIMESTAMPTZ,
    evidence TEXT,
    preliminary_impact TEXT,
    risk_level VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_policy_changes_mkt_date ON marketplace_policy_changes(marketplace, detected_at DESC);
CREATE INDEX idx_policy_changes_policy ON marketplace_policy_changes(policy_id);

CREATE TABLE IF NOT EXISTS compliance_incidents (
    id UUID PRIMARY KEY,
    incident_code VARCHAR(50) NOT NULL UNIQUE,
    marketplace VARCHAR(50) NOT NULL,
    policy_id UUID REFERENCES marketplace_policies(id) ON DELETE SET NULL,
    policy_change_id UUID REFERENCES marketplace_policy_changes(id) ON DELETE SET NULL,
    severity VARCHAR(50) NOT NULL,
    affected_features VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    opened_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    responsible VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    evidence TEXT,
    remediation_plan TEXT,
    approved_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incidents_status_sev ON compliance_incidents(status, severity);
CREATE INDEX idx_incidents_mkt ON compliance_incidents(marketplace);

CREATE TABLE IF NOT EXISTS marketplace_api_lifecycles (
    id UUID PRIMARY KEY,
    marketplace VARCHAR(50) NOT NULL,
    api_name VARCHAR(100) NOT NULL,
    version VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    sunset_date TIMESTAMPTZ,
    migration_guide_url VARCHAR(1000),
    last_audited_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_lifecycles_mkt ON marketplace_api_lifecycles(marketplace, status);

CREATE TABLE IF NOT EXISTS feature_enforcement_records (
    id UUID PRIMARY KEY,
    marketplace VARCHAR(50) NOT NULL,
    feature_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    incident_id UUID REFERENCES compliance_incidents(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_feature_enforcement UNIQUE (marketplace, feature_name)
);

CREATE TABLE IF NOT EXISTS safe_reactivation_logs (
    id UUID PRIMARY KEY,
    marketplace VARCHAR(50) NOT NULL,
    feature_name VARCHAR(100) NOT NULL,
    approved_by VARCHAR(255) NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL,
    evidence TEXT NOT NULL,
    policy_version VARCHAR(50),
    tests_executed TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reactivation_logs_mkt ON safe_reactivation_logs(marketplace, approved_at DESC);
