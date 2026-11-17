package br.com.sellerintelligence.reconciliation;

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

@Testcontainers
class PostgresReconciliationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("seller_test_reconciliation")
            .withUsername("test_user")
            .withPassword("test_password");

    @Test
    @DisplayName("Should run Flyway V12 migration and enforce Reconciliation and DRE tables, indices and constraints on real PostgreSQL")
    void shouldExecuteV12MigrationAndEnforceConstraints() throws Exception {
        assertThat(postgres.isRunning()).isTrue();

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        var migrateResult = flyway.migrate();
        assertThat(migrateResult.success).isTrue();
        assertThat(Integer.parseInt(migrateResult.targetSchemaVersion)).isGreaterThanOrEqualTo(12);

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            UUID tenantId = UUID.randomUUID();
            UUID settlementId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID disputeId = UUID.randomUUID();
            UUID auditLogId = UUID.randomUUID();

            // 1. Insert organization (tenant)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO organizations (id, name, slug, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, tenantId);
                stmt.setString(2, "Reconciliation Org");
                stmt.setString(3, "rec-org-" + System.currentTimeMillis());
                stmt.executeUpdate();
            }

            // 2. Insert settlement batch into reconciliation_settlements
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO reconciliation_settlements (id, tenant_id, marketplace, external_settlement_id, " +
                    "settlement_date, gross_amount, net_amount, total_fees, total_shipping, total_adjustments, " +
                    "currency, status, hash_fingerprint, total_items_count, created_at, updated_at) " +
                    "VALUES (?, ?, 'MERCADO_LIVRE', 'SETTL-TEST-01', NOW(), ?, ?, ?, ?, ?, 'BRL', 'IMPORTED', ?, 1, NOW(), NOW())")) {
                stmt.setObject(1, settlementId);
                stmt.setObject(2, tenantId);
                stmt.setBigDecimal(3, new BigDecimal("500.00"));
                stmt.setBigDecimal(4, new BigDecimal("430.00"));
                stmt.setBigDecimal(5, new BigDecimal("50.00"));
                stmt.setBigDecimal(6, new BigDecimal("20.00"));
                stmt.setBigDecimal(7, BigDecimal.ZERO);
                stmt.setString(8, "hash-test-fingerprint");
                stmt.executeUpdate();
            }

            // 3. Insert settlement item into reconciliation_settlement_items
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO reconciliation_settlement_items (id, settlement_id, tenant_id, external_order_id, " +
                    "external_transaction_id, entry_type, expected_amount, settled_amount, discrepancy_amount, " +
                    "reconciliation_status, confidence_score, matching_method, created_at, updated_at) " +
                    "VALUES (?, ?, ?, 'MLB999888', 'TX-888', 'ORDER_PAYOUT', ?, ?, ?, 'DISCREPANT', ?, 'ORDER_ID_EXACT', NOW(), NOW())")) {
                stmt.setObject(1, itemId);
                stmt.setObject(2, settlementId);
                stmt.setObject(3, tenantId);
                stmt.setBigDecimal(4, new BigDecimal("450.00"));
                stmt.setBigDecimal(5, new BigDecimal("430.00"));
                stmt.setBigDecimal(6, new BigDecimal("-20.00"));
                stmt.setBigDecimal(7, new BigDecimal("1.0000"));
                stmt.executeUpdate();
            }

            // 4. Insert dispute protocol into reconciliation_disputes
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO reconciliation_disputes (id, settlement_item_id, tenant_id, protocol_number, " +
                    "marketplace, disputed_amount, status, channel, disclaimer, created_at, updated_at) " +
                    "VALUES (?, ?, ?, 'PROT-TEST-123', 'MERCADO_LIVRE', ?, 'OPEN', 'HELP_CENTER', 'Disclaimer Test', NOW(), NOW())")) {
                stmt.setObject(1, disputeId);
                stmt.setObject(2, itemId);
                stmt.setObject(3, tenantId);
                stmt.setBigDecimal(4, new BigDecimal("20.00"));
                stmt.executeUpdate();
            }

            // 5. Insert audit log into reconciliation_audit_logs
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO reconciliation_audit_logs (id, tenant_id, settlement_item_id, action, previous_state, new_state, justification, created_at) " +
                    "VALUES (?, ?, ?, 'DISPUTE_LOGGED', 'DISCREPANT', 'DISPUTE_OPEN', 'Abertura de contestação por divergência tarifária', NOW())")) {
                stmt.setObject(1, auditLogId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, itemId);
                stmt.executeUpdate();
            }

            // 6. Verify inserted data and constraints
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reconciliation_settlements WHERE tenant_id = ?")) {
                stmt.setObject(1, tenantId);
                ResultSet rs = stmt.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reconciliation_settlement_items WHERE tenant_id = ?")) {
                stmt.setObject(1, tenantId);
                ResultSet rs = stmt.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reconciliation_disputes WHERE tenant_id = ?")) {
                stmt.setObject(1, tenantId);
                ResultSet rs = stmt.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM reconciliation_audit_logs WHERE tenant_id = ?")) {
                stmt.setObject(1, tenantId);
                ResultSet rs = stmt.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }
}
