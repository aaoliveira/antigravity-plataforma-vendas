-- V10: Smart Alerts & Action Center (Rules Catalog, Inbox, Operational Tasks, Deduplication & Opt-In Governance)

-- 1. Catálogo de Regras de Alerta
CREATE TABLE IF NOT EXISTS action_alert_rules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(64) NOT NULL,
    scope_type VARCHAR(32) NOT NULL DEFAULT 'ORGANIZATION',
    scope_id VARCHAR(128),
    threshold_value NUMERIC(15, 2) NOT NULL,
    threshold_operator VARCHAR(32) NOT NULL DEFAULT 'LESS_THAN',
    time_window_minutes INTEGER NOT NULL DEFAULT 60,
    cooldown_minutes INTEGER NOT NULL DEFAULT 120,
    severity VARCHAR(32) NOT NULL DEFAULT 'WARNING',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_action_alert_rules_tenant ON action_alert_rules(tenant_id, is_active);

-- 2. Preferências e Opt-in de Notificações
CREATE TABLE IF NOT EXISTS action_notification_preferences (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    channel VARCHAR(32) NOT NULL,
    is_opted_in BOOLEAN NOT NULL DEFAULT false,
    target_address VARCHAR(255),
    frequency_limit_per_hour INTEGER NOT NULL DEFAULT 10,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_action_notif_pref UNIQUE (tenant_id, user_id, channel)
);

CREATE INDEX IF NOT EXISTS idx_action_notif_pref_tenant_user ON action_notification_preferences(tenant_id, user_id);

-- 3. Inbox Unificado de Alertas Operacionais
CREATE TABLE IF NOT EXISTS action_alerts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    rule_id UUID REFERENCES action_alert_rules(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    domain_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    scope_type VARCHAR(32) NOT NULL DEFAULT 'ORGANIZATION',
    scope_id VARCHAR(128),
    provenance VARCHAR(32) NOT NULL DEFAULT 'CALCULATED',
    evidence_context TEXT NOT NULL,
    dedup_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_action_alert_dedup UNIQUE (tenant_id, dedup_key)
);

CREATE INDEX IF NOT EXISTS idx_action_alerts_tenant_status ON action_alerts(tenant_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_action_alerts_domain ON action_alerts(tenant_id, domain_type);

-- 4. Action Center — Tarefas Operacionais
CREATE TABLE IF NOT EXISTS action_tasks (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    alert_id UUID REFERENCES action_alerts(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'TODO',
    assigned_to_user_id UUID,
    due_date TIMESTAMPTZ,
    evidence_summary TEXT,
    completion_notes TEXT,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_action_tasks_tenant_status ON action_tasks(tenant_id, status, due_date);
CREATE INDEX IF NOT EXISTS idx_action_tasks_assigned ON action_tasks(tenant_id, assigned_to_user_id);
CREATE INDEX IF NOT EXISTS idx_action_tasks_alert ON action_tasks(alert_id);

-- 5. Trilha de Auditoria Imutável de Tarefas
CREATE TABLE IF NOT EXISTS action_task_audits (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES action_tasks(id) ON DELETE CASCADE,
    actor_user_id UUID NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    previous_state VARCHAR(64),
    new_state VARCHAR(64) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_action_task_audits_task ON action_task_audits(task_id, created_at ASC);

-- 6. Registro de Disparos e Supressões de Notificação
CREATE TABLE IF NOT EXISTS action_notification_deliveries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    alert_id UUID NOT NULL REFERENCES action_alerts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    channel VARCHAR(32) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    delivery_status VARCHAR(64) NOT NULL,
    error_message TEXT,
    sent_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notif_deliveries_tenant ON action_notification_deliveries(tenant_id, sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_deliveries_alert ON action_notification_deliveries(alert_id);
