package br.com.sellerintelligence.financial;

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
class PostgresFinancialEngineIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("seller_test_financial")
            .withUsername("test_user")
            .withPassword("test_password");

    @Test
    @DisplayName("Should run Flyway V7 migration and enforce financial constraints and precision on real PostgreSQL")
    void shouldExecuteV7MigrationAndEnforceConstraints() throws Exception {
        assertThat(postgres.isRunning()).isTrue();

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        var migrateResult = flyway.migrate();
        assertThat(migrateResult.success).isTrue();

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            UUID orgId = UUID.randomUUID();
            UUID skuCostId = UUID.randomUUID();
            UUID ruleSetId = UUID.randomUUID();
            UUID compId = UUID.randomUUID();

            // 1. Insert organization
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO organizations (id, name, slug, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, orgId);
                stmt.setString(2, "Postgres Financial Org");
                stmt.setString(3, "postgres-fin-" + System.currentTimeMillis());
                stmt.executeUpdate();
            }

            // 2. Insert SKU cost with 4-decimal precision
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO sku_costs (id, tenant_id, sku, cost_price, currency, unit, origin, effective_from, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, 'BRL', 'UN', 'MANUAL', NOW(), NOW(), NOW())")) {
                stmt.setObject(1, skuCostId);
                stmt.setObject(2, orgId);
                stmt.setString(3, "SKU-PG-01");
                stmt.setBigDecimal(4, new BigDecimal("123.4567"));
                stmt.executeUpdate();
            }

            // Verify SKU cost inserted with exact precision
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT cost_price FROM sku_costs WHERE id = ?")) {
                stmt.setObject(1, skuCostId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getBigDecimal("cost_price")).isEqualByComparingTo("123.4567");
                }
            }

            // 3. Insert financial rule set
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO financial_rule_sets (id, tenant_id, marketplace, name, version, effective_from, is_active, created_at, updated_at) " +
                    "VALUES (?, ?, 'MERCADO_LIVRE', 'Regra Geral ML', 1, NOW(), true, NOW(), NOW())")) {
                stmt.setObject(1, ruleSetId);
                stmt.setObject(2, orgId);
                stmt.executeUpdate();
            }

            // 4. Insert component
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO financial_rule_components (id, rule_set_id, component_type, component_value, threshold_amount, applies_to, created_at) " +
                    "VALUES (?, ?, 'MARKETPLACE_COMMISSION_PERCENTAGE', ?, 79.00, 'ITEM', NOW())")) {
                stmt.setObject(1, compId);
                stmt.setObject(2, ruleSetId);
                stmt.setBigDecimal(3, new BigDecimal("0.1600"));
                stmt.executeUpdate();
            }

            // 5. Verify unique constraint on financial_rule_sets: (tenant_id, name, version)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO financial_rule_sets (id, tenant_id, marketplace, name, version, effective_from, is_active, created_at, updated_at) " +
                    "VALUES (?, ?, 'MERCADO_LIVRE', 'Regra Geral ML', 1, NOW(), true, NOW(), NOW())")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, orgId);
                org.junit.jupiter.api.Assertions.assertThrows(Exception.class, stmt::executeUpdate);
            }
        }
    }
}
