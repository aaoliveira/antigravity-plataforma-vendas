package br.com.sellerintelligence.market;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PostgresMarketIntelligenceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("seller_test_market")
            .withUsername("test_user")
            .withPassword("test_password");

    @Test
    @DisplayName("Should run Flyway V6 migration and enforce constraints on real PostgreSQL")
    void shouldExecuteV6MigrationAndEnforceConstraints() throws Exception {
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
            UUID snapshotId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID savedSearchId = UUID.randomUUID();
            UUID savedProductId = UUID.randomUUID();

            // 1. Insert organization
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO organizations (id, name, slug, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, orgId);
                stmt.setString(2, "Postgres Market Org");
                stmt.setString(3, "postgres-market-" + System.currentTimeMillis());
                stmt.executeUpdate();
            }

            // 2. Insert market search snapshot
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO market_search_snapshots (id, tenant_id, marketplace, query, total_results, collected_at, provenance, created_at) " +
                            "VALUES (?, ?, 'MERCADO_LIVRE', 'smartphone 5g', 50, NOW(), 'OBSERVED', NOW())")) {
                stmt.setObject(1, snapshotId);
                stmt.setObject(2, orgId);
                stmt.executeUpdate();
            }

            // 3. Insert market product item
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO market_product_items (id, snapshot_id, marketplace, external_item_id, title, price, currency, provenance, collected_at) " +
                            "VALUES (?, ?, 'MERCADO_LIVRE', 'MLB-5G-100', 'Smartphone 5G 128GB', 1499.00, 'BRL', 'OBSERVED', NOW())")) {
                stmt.setObject(1, itemId);
                stmt.setObject(2, snapshotId);
                stmt.executeUpdate();
            }

            // 4. Insert saved search
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO saved_searches (id, tenant_id, name, marketplace, query, created_at, updated_at) " +
                            "VALUES (?, ?, 'Pesquisa Celulares 5G', 'MERCADO_LIVRE', 'smartphone 5g', NOW(), NOW())")) {
                stmt.setObject(1, savedSearchId);
                stmt.setObject(2, orgId);
                stmt.executeUpdate();
            }

            // 5. Insert saved market product
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO saved_market_products (id, tenant_id, marketplace, external_item_id, title, price, currency, notes, provenance, created_at, updated_at) " +
                            "VALUES (?, ?, 'MERCADO_LIVRE', 'MLB-5G-100', 'Smartphone 5G 128GB', 1499.00, 'BRL', 'Monitorar preco', 'OBSERVED', NOW(), NOW())")) {
                stmt.setObject(1, savedProductId);
                stmt.setObject(2, orgId);
                stmt.executeUpdate();
            }

            // Verify counts
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM market_search_snapshots WHERE tenant_id = ?")) {
                stmt.setObject(1, orgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM market_product_items WHERE snapshot_id = ?")) {
                stmt.setObject(1, snapshotId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM saved_searches WHERE tenant_id = ?")) {
                stmt.setObject(1, orgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM saved_market_products WHERE tenant_id = ?")) {
                stmt.setObject(1, orgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1);
                }
            }
        }
    }
}
