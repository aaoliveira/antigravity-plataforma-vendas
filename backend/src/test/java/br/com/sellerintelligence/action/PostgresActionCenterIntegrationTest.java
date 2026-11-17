package br.com.sellerintelligence.action;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class PostgresActionCenterIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("seller_test_action")
            .withUsername("test_user")
            .withPassword("test_password");

    @Test
    @DisplayName("Should run Flyway V10 migration and enforce Smart Alerts and Action Center schema, indices and constraints on real PostgreSQL")
    void shouldExecuteV10MigrationAndEnforceConstraints() throws Exception {
        assertThat(postgres.isRunning()).isTrue();

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        var migrateResult = flyway.migrate();
        assertThat(migrateResult.success).isTrue();
        assertThat(migrateResult.targetSchemaVersion).isEqualTo("10");

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            UUID tenantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID ruleId = UUID.randomUUID();
            UUID alertId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID auditId = UUID.randomUUID();
            UUID prefId = UUID.randomUUID();
            UUID deliveryId = UUID.randomUUID();

            // 1. Insert organization (tenant)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO organizations (id, name, slug, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, tenantId);
                stmt.setString(2, "Action Center Org");
                stmt.setString(3, "act-org-" + System.currentTimeMillis());
                stmt.executeUpdate();
            }

            // 2. Insert alert rule in action_alert_rules
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO action_alert_rules (id, tenant_id, name, rule_type, scope_type, threshold_value, threshold_operator, time_window_minutes, cooldown_minutes, severity, is_active, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, NOW(), NOW())")) {
                stmt.setObject(1, ruleId);
                stmt.setObject(2, tenantId);
                stmt.setString(3, "Margem Mínima 10%");
                stmt.setString(4, "MARGIN_THRESHOLD");
                stmt.setString(5, "ORGANIZATION");
                stmt.setBigDecimal(6, new BigDecimal("10.00"));
                stmt.setString(7, "LESS_THAN");
                stmt.setInt(8, 60);
                stmt.setInt(9, 120);
                stmt.setString(10, "CRITICAL");
                stmt.executeUpdate();
            }

            // 3. Insert notification preference with opt-in
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO action_notification_preferences (id, tenant_id, user_id, channel, is_opted_in, target_address, frequency_limit_per_hour, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, true, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, prefId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, userId);
                stmt.setString(4, "EMAIL");
                stmt.setString(5, "alerts@seller.com");
                stmt.setInt(6, 5);
                stmt.executeUpdate();
            }

            // 4. Test UNIQUE constraint on action_notification_preferences (tenant_id, user_id, channel)
            assertThatThrownBy(() -> {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO action_notification_preferences (id, tenant_id, user_id, channel, is_opted_in, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, true, NOW(), NOW())")) {
                    stmt.setObject(1, UUID.randomUUID());
                    stmt.setObject(2, tenantId);
                    stmt.setObject(3, userId);
                    stmt.setString(4, "EMAIL"); // duplicate channel for same tenant & user
                    stmt.executeUpdate();
                }
            }).hasMessageContaining("uk_action_notif_pref");

            // 5. Insert alert into action_alerts
            String dedupKey = "dedup:rule:" + ruleId + ":bucket:100";
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO action_alerts (id, tenant_id, rule_id, title, description, severity, domain_type, status, scope_type, provenance, evidence_context, dedup_key, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'NEW', 'ORGANIZATION', 'CALCULATED', ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, alertId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, ruleId);
                stmt.setString(4, "Margem Crítica Detectada");
                stmt.setString(5, "Margem do pedido é de 2.1%");
                stmt.setString(6, "CRITICAL");
                stmt.setString(7, "FINANCIAL");
                stmt.setString(8, "{\"margin\":2.1}");
                stmt.setString(9, dedupKey);
                stmt.executeUpdate();
            }

            // 6. Test UNIQUE constraint on action_alerts (tenant_id, dedup_key)
            assertThatThrownBy(() -> {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO action_alerts (id, tenant_id, title, description, severity, domain_type, evidence_context, dedup_key, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 'INFO', 'FINANCIAL', '{}', ?, NOW(), NOW())")) {
                    stmt.setObject(1, UUID.randomUUID());
                    stmt.setObject(2, tenantId);
                    stmt.setString(3, "Alerta Duplicado");
                    stmt.setString(4, "Desc");
                    stmt.setString(5, dedupKey); // duplicate dedup key
                    stmt.executeUpdate();
                }
            }).hasMessageContaining("uk_action_alert_dedup");

            // 7. Insert operational task into action_tasks
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO action_tasks (id, tenant_id, alert_id, title, description, priority, status, assigned_to_user_id, evidence_summary, created_by_user_id, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'TODO', ?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, taskId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, alertId);
                stmt.setString(4, "Revisar custo fornecedor");
                stmt.setString(5, "Alinhamento com compras");
                stmt.setString(6, "HIGH");
                stmt.setObject(7, userId);
                stmt.setString(8, "Origem: Alerta " + alertId);
                stmt.setObject(9, userId);
                stmt.executeUpdate();
            }

            // 8. Insert audit record in action_task_audits
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO action_task_audits (id, task_id, actor_user_id, action_type, new_state, notes, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NOW())")) {
                stmt.setObject(1, auditId);
                stmt.setObject(2, taskId);
                stmt.setObject(3, userId);
                stmt.setString(4, "CREATED");
                stmt.setString(5, "TODO");
                stmt.setString(6, "Tarefa criada a partir do alerta");
                stmt.executeUpdate();
            }

            // 9. Insert notification delivery in action_notification_deliveries
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO action_notification_deliveries (id, tenant_id, alert_id, user_id, channel, recipient, delivery_status, sent_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'SENT', NOW())")) {
                stmt.setObject(1, deliveryId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, alertId);
                stmt.setObject(4, userId);
                stmt.setString(5, "EMAIL");
                stmt.setString(6, "alerts@seller.com");
                stmt.executeUpdate();
            }

            // 10. Query and assert
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT t.title, a.title AS alert_title, d.delivery_status " +
                    "FROM action_tasks t " +
                    "JOIN action_alerts a ON t.alert_id = a.id " +
                    "JOIN action_notification_deliveries d ON d.alert_id = a.id " +
                    "WHERE t.tenant_id = ?")) {
                stmt.setObject(1, tenantId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("title")).isEqualTo("Revisar custo fornecedor");
                    assertThat(rs.getString("alert_title")).isEqualTo("Margem Crítica Detectada");
                    assertThat(rs.getString("delivery_status")).isEqualTo("SENT");
                }
            }
        }
    }
}
