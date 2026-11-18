-- =============================================================================
-- Migration: V6_1__postgresql_rls_orders.sql
-- Description: Ativação mandatória de Row-Level Security (RLS) no PostgreSQL para Pedidos, Itens, Checkpoints, Outbox e DLQ
-- Engine: PostgreSQL 17 / 18
-- =============================================================================

-- 1. orders
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_orders ON orders;
CREATE POLICY tenant_isolation_orders ON orders
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 2. order_items
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_order_items ON order_items;
CREATE POLICY tenant_isolation_order_items ON order_items
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 3. order_sync_checkpoints
ALTER TABLE order_sync_checkpoints ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_sync_checkpoints FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_order_sync_checkpoints ON order_sync_checkpoints;
CREATE POLICY tenant_isolation_order_sync_checkpoints ON order_sync_checkpoints
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 4. order_outbox_events
ALTER TABLE order_outbox_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_outbox_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_order_outbox_events ON order_outbox_events;
CREATE POLICY tenant_isolation_order_outbox_events ON order_outbox_events
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 5. order_dlq_records
ALTER TABLE order_dlq_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_dlq_records FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation_order_dlq_records ON order_dlq_records;
CREATE POLICY tenant_isolation_order_dlq_records ON order_dlq_records
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
