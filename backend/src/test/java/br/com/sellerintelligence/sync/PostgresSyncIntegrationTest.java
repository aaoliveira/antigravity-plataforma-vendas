package br.com.sellerintelligence.sync;

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
class PostgresSyncIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("seller_test_sync")
            .withUsername("test_user")
            .withPassword("test_password");

    @Test
    @DisplayName("Should run Flyway V5 migration and enforce constraints on real PostgreSQL")
    void shouldExecuteV5MigrationAndEnforceConstraints() throws Exception {
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
            UUID companyId = UUID.randomUUID();
            UUID storeId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID listingId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            // 1. Insert organization
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO organizations (id, name, slug, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, orgId);
                stmt.setString(2, "Postgres Sync Test Org");
                stmt.setString(3, "postgres-sync-" + System.currentTimeMillis());
                stmt.executeUpdate();
            }

            // 2. Insert company
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO companies (id, tenant_id, cnpj, legal_name, trade_name, tax_regime, status, created_at, updated_at) " +
                            "VALUES (?, ?, '12345678000195', 'Razao PG', 'Fantasia PG', 'SIMPLES_NACIONAL', 'ACTIVE', NOW(), NOW())")) {
                stmt.setObject(1, companyId);
                stmt.setObject(2, orgId);
                stmt.executeUpdate();
            }

            // 3. Insert store
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO stores (id, tenant_id, company_id, marketplace, internal_name, external_account_id, connection_status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 'MERCADO_LIVRE', 'Loja PG', 'EXT-PG-1', 'CONNECTED', NOW(), NOW())")) {
                stmt.setObject(1, storeId);
                stmt.setObject(2, orgId);
                stmt.setObject(3, companyId);
                stmt.executeUpdate();
            }

            // 4. Insert product
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO products (id, tenant_id, store_id, sku, title, brand, category, barcode, cost_price, provenance, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 'SKU-PG-001', 'Produto Teste PG', 'Marca PG', 'Cat PG', '123456', 50.00, 'IMPORTED', NOW(), NOW())")) {
                stmt.setObject(1, productId);
                stmt.setObject(2, orgId);
                stmt.setObject(3, storeId);
                stmt.executeUpdate();
            }

            // 5. Insert listing
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO listings (id, tenant_id, store_id, product_id, marketplace, external_listing_id, sku, title, status, price, currency, available_quantity, sold_quantity, provenance, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, 'MERCADO_LIVRE', 'MLB-PG-999', 'SKU-PG-001', 'Titulo Anuncio PG', 'ACTIVE', 99.90, 'BRL', 10, 2, 'IMPORTED', NOW(), NOW())")) {
                stmt.setObject(1, listingId);
                stmt.setObject(2, orgId);
                stmt.setObject(3, storeId);
                stmt.setObject(4, productId);
                stmt.executeUpdate();
            }

            // 6. Insert order
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO orders (id, tenant_id, store_id, marketplace, external_order_id, status, total_amount, currency, order_date, provenance, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 'MERCADO_LIVRE', 'ORDER-PG-123', 'PAID', 99.90, 'BRL', NOW(), 'IMPORTED', NOW(), NOW())")) {
                stmt.setObject(1, orderId);
                stmt.setObject(2, orgId);
                stmt.setObject(3, storeId);
                stmt.executeUpdate();
            }

            // 7. Insert order item
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO order_items (id, tenant_id, order_id, external_item_id, sku, title, unit_price, quantity, sale_fee, created_at) " +
                            "VALUES (?, ?, ?, 'MLB-PG-999', 'SKU-PG-001', 'Titulo Anuncio PG', 99.90, 1, 12.00, NOW())")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, orgId);
                stmt.setObject(3, orderId);
                stmt.executeUpdate();
            }

            // 8. Insert sync_job
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO sync_jobs (id, tenant_id, store_id, marketplace, resource_type, trigger_type, status, items_processed, items_succeeded, items_failed, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 'MERCADO_LIVRE', 'CATALOG', 'MANUAL', 'COMPLETED', 1, 1, 0, NOW(), NOW())")) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, orgId);
                stmt.setObject(3, storeId);
                stmt.executeUpdate();
            }

            // Verify counts
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE tenant_id = ?")) {
                stmt.setObject(1, orgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM listings WHERE tenant_id = ?")) {
                stmt.setObject(1, orgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE tenant_id = ?")) {
                stmt.setObject(1, orgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1);
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM order_items WHERE tenant_id = ?")) {
                stmt.setObject(1, orgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(1);
                }
            }
        }
    }
}
