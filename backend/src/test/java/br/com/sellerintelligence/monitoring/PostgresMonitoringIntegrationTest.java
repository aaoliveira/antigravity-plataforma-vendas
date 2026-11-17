package br.com.sellerintelligence.monitoring;

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
class PostgresMonitoringIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("seller_test_monitoring")
            .withUsername("test_user")
            .withPassword("test_password");

    @Test
    @DisplayName("Should run Flyway V9 migration and enforce monitoring tables, indices and constraints on real PostgreSQL")
    void shouldExecuteV9MigrationAndEnforceConstraints() throws Exception {
        assertThat(postgres.isRunning()).isTrue();

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        var migrateResult = flyway.migrate();
        assertThat(migrateResult.success).isTrue();
        assertThat(migrateResult.migrationsExecuted).isGreaterThanOrEqualTo(9);

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            UUID tenantId = UUID.randomUUID();
            UUID watchlistId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID snapshotId = UUID.randomUUID();
            UUID eventId = UUID.randomUUID();
            UUID ruleId = UUID.randomUUID();
            UUID alertId = UUID.randomUUID();

            // 1. Insert organization (tenant)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO organizations (id, name, slug, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, tenantId);
                stmt.setString(2, "Monitoring Org");
                stmt.setString(3, "mon-org-" + System.currentTimeMillis());
                stmt.executeUpdate();
            }

            // 2. Insert watchlist
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO monitoring_watchlists (id, tenant_id, name, description, type, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, watchlistId);
                stmt.setObject(2, tenantId);
                stmt.setString(3, "Lista de Concorrentes Principais");
                stmt.setString(4, "Top sellers de periféricos");
                stmt.setString(5, "COMPETITOR_LISTING");
                stmt.setString(6, "ACTIVE");
                stmt.executeUpdate();
            }

            // 3. Insert watchlist item
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO monitoring_watchlist_items (id, watchlist_id, tenant_id, marketplace, item_type, external_id, target_name, status, check_frequency_minutes, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, itemId);
                stmt.setObject(2, watchlistId);
                stmt.setObject(3, tenantId);
                stmt.setString(4, "MERCADO_LIVRE");
                stmt.setString(5, "LISTING");
                stmt.setString(6, "MLB998877");
                stmt.setString(7, "Mouse Gamer 16000 DPI");
                stmt.setString(8, "ACTIVE");
                stmt.setInt(9, 360);
                stmt.executeUpdate();
            }

            // 4. Insert snapshot (preserves timestamp and source)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO monitoring_snapshots (id, tenant_id, item_id, marketplace, external_id, price, listing_status, title, collection_status, source, provenance, collected_at, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, snapshotId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, itemId);
                stmt.setString(4, "MERCADO_LIVRE");
                stmt.setString(5, "MLB998877");
                stmt.setBigDecimal(6, new BigDecimal("149.90"));
                stmt.setString(7, "ACTIVE");
                stmt.setString(8, "Mouse Gamer 16000 DPI");
                stmt.setString(9, "SUCCESS");
                stmt.setString(10, "MERCADO_LIVRE_PUBLIC_API");
                stmt.setString(11, "OBSERVED");
                stmt.executeUpdate();
            }

            // 5. Insert change event with deterministic hash
            String eventHash = "a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef";
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO monitoring_change_events (id, tenant_id, item_id, snapshot_id, event_type, previous_value, current_value, price_delta_amount, price_delta_percentage, event_hash, detected_at, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, eventId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, itemId);
                stmt.setObject(4, snapshotId);
                stmt.setString(5, "PRICE_CHANGE");
                stmt.setString(6, "169.90");
                stmt.setString(7, "149.90");
                stmt.setBigDecimal(8, new BigDecimal("-20.00"));
                stmt.setBigDecimal(9, new BigDecimal("-11.7716"));
                stmt.setString(10, eventHash);
                stmt.executeUpdate();
            }

            // Enforce duplicate event hash violation
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO monitoring_change_events (id, tenant_id, item_id, snapshot_id, event_type, previous_value, current_value, event_hash, detected_at, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, tenantId);
                stmt.setObject(3, itemId);
                stmt.setObject(4, snapshotId);
                stmt.setString(5, "PRICE_CHANGE");
                stmt.setString(6, "169.90");
                stmt.setString(7, "149.90");
                stmt.setString(8, eventHash); // Duplicate hash!
                assertThatThrownBy(stmt::executeUpdate)
                        .hasMessageContaining("uk_monitoring_change_event_hash");
            }

            // 6. Insert alert rule
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO monitoring_alert_rules (id, tenant_id, watchlist_id, name, event_type, min_price_delta_percentage, direction, cooldown_minutes, channel, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, ruleId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, watchlistId);
                stmt.setString(4, "Alerta Queda > 10%");
                stmt.setString(5, "PRICE_CHANGE");
                stmt.setBigDecimal(6, new BigDecimal("10.00"));
                stmt.setString(7, "DECREASE");
                stmt.setInt(8, 1440);
                stmt.setString(9, "IN_APP");
                stmt.setString(10, "ACTIVE");
                stmt.executeUpdate();
            }

            // 7. Insert alert with dedup_key
            String dedupKey = ruleId + ":" + itemId + ":PRICE_CHANGE:48500";
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO monitoring_alerts (id, tenant_id, rule_id, item_id, event_id, title, message, severity, dedup_key, is_read, triggered_at, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false, NOW(), NOW())")) {
                stmt.setObject(1, alertId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, ruleId);
                stmt.setObject(4, itemId);
                stmt.setObject(5, eventId);
                stmt.setString(6, "Queda Detectada: Mouse Gamer");
                stmt.setString(7, "Preço caiu de 169.90 para 149.90");
                stmt.setString(8, "WARNING");
                stmt.setString(9, dedupKey);
                stmt.executeUpdate();
            }

            // Verify query counts
            try (PreparedStatement stmt = conn.prepareStatement("SELECT count(*) FROM monitoring_watchlists WHERE tenant_id = ?");
                 PreparedStatement stmtItems = conn.prepareStatement("SELECT count(*) FROM monitoring_watchlist_items WHERE tenant_id = ?");
                 PreparedStatement stmtSnaps = conn.prepareStatement("SELECT count(*) FROM monitoring_snapshots WHERE tenant_id = ?")) {
                stmt.setObject(1, tenantId);
                ResultSet rs = stmt.executeQuery();
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);

                stmtItems.setObject(1, tenantId);
                ResultSet rsItems = stmtItems.executeQuery();
                rsItems.next();
                assertThat(rsItems.getInt(1)).isEqualTo(1);

                stmtSnaps.setObject(1, tenantId);
                ResultSet rsSnaps = stmtSnaps.executeQuery();
                rsSnaps.next();
                assertThat(rsSnaps.getInt(1)).isEqualTo(1);
            }
        }
    }
}
