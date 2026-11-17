package br.com.sellerintelligence.discovery;

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
class PostgresDiscoveryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("seller_test_discovery")
            .withUsername("test_user")
            .withPassword("test_password");

    @Test
    @DisplayName("Should run Flyway V11 migration and enforce Product Discovery tables, indices and constraints on real PostgreSQL")
    void shouldExecuteV11MigrationAndEnforceConstraints() throws Exception {
        assertThat(postgres.isRunning()).isTrue();

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        var migrateResult = flyway.migrate();
        assertThat(migrateResult.success).isTrue();
        assertThat(Integer.parseInt(migrateResult.targetSchemaVersion)).isGreaterThanOrEqualTo(11);

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            UUID tenantId = UUID.randomUUID();
            UUID catalogId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID matchId = UUID.randomUUID();
            UUID visualSearchId = UUID.randomUUID();

            // 1. Insert organization (tenant)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO organizations (id, name, slug, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())")) {
                stmt.setObject(1, tenantId);
                stmt.setString(2, "Discovery Org");
                stmt.setString(3, "disc-org-" + System.currentTimeMillis());
                stmt.executeUpdate();
            }

            // 2. Insert catalog into discovery_catalogs
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO discovery_catalogs (id, tenant_id, filename, stored_filename, content_type, file_size_bytes, supplier_name, status, total_items, reviewed_items, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'PARSED', 1, 0, NOW(), NOW())")) {
                stmt.setObject(1, catalogId);
                stmt.setObject(2, tenantId);
                stmt.setString(3, "fornecedor_tech.pdf");
                stmt.setString(4, "stored_" + catalogId + ".pdf");
                stmt.setString(5, "application/pdf");
                stmt.setLong(6, 1048576L);
                stmt.setString(7, "Distribuidora Tech Ltda");
                stmt.executeUpdate();
            }

            // 3. Insert catalog item into discovery_catalog_items
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO discovery_catalog_items (id, catalog_id, tenant_id, name, supplier_sku, ean, cost_price, currency, extraction_method, overall_confidence, review_status, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'BRL', 'PDF_TEXT', ?, 'PENDING_REVIEW', NOW(), NOW())")) {
                stmt.setObject(1, itemId);
                stmt.setObject(2, catalogId);
                stmt.setObject(3, tenantId);
                stmt.setString(4, "Headset Gamer 7.1 USB");
                stmt.setString(5, "HS-71");
                stmt.setString(6, "7891234567895");
                stmt.setBigDecimal(7, new BigDecimal("89.90"));
                stmt.setBigDecimal(8, new BigDecimal("0.9500"));
                stmt.executeUpdate();
            }

            // 4. Insert candidate match into discovery_product_matches
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO discovery_product_matches (id, catalog_item_id, tenant_id, marketplace, matched_external_id, matched_title, matched_price, similarity_score, match_source, status, estimated_gross_margin, created_at) " +
                    "VALUES (?, ?, ?, 'MERCADO_LIVRE', 'MLB882211', 'Headset Gamer 7.1 Surround', ?, ?, 'EAN_EXACT', 'CANDIDATE', ?, NOW())")) {
                stmt.setObject(1, matchId);
                stmt.setObject(2, itemId);
                stmt.setObject(3, tenantId);
                stmt.setBigDecimal(4, new BigDecimal("179.90"));
                stmt.setBigDecimal(5, new BigDecimal("0.9800"));
                stmt.setBigDecimal(6, new BigDecimal("0.5003"));
                stmt.executeUpdate();
            }

            // 5. Insert visual search into discovery_visual_searches
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO discovery_visual_searches (id, tenant_id, stored_image_path, image_mime_type, search_query_description, top_matches_count, created_at) " +
                    "VALUES (?, ?, ?, 'image/png', 'Busca visual fone de ouvido', 1, NOW())")) {
                stmt.setObject(1, visualSearchId);
                stmt.setObject(2, tenantId);
                stmt.setString(3, "visual/image_123.png");
                stmt.executeUpdate();
            }

            // 6. Query and assert
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT c.filename, i.name, m.matched_title, m.similarity_score, m.status " +
                    "FROM discovery_catalogs c " +
                    "JOIN discovery_catalog_items i ON i.catalog_id = c.id " +
                    "JOIN discovery_product_matches m ON m.catalog_item_id = i.id " +
                    "WHERE c.tenant_id = ?")) {
                stmt.setObject(1, tenantId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("filename")).isEqualTo("fornecedor_tech.pdf");
                    assertThat(rs.getString("name")).isEqualTo("Headset Gamer 7.1 USB");
                    assertThat(rs.getString("matched_title")).isEqualTo("Headset Gamer 7.1 Surround");
                    assertThat(rs.getBigDecimal("similarity_score")).isEqualByComparingTo("0.9800");
                    assertThat(rs.getString("status")).isEqualTo("CANDIDATE");
                }
            }

            // 7. Verify Cascade Deletion (deleting catalog deletes items and matches)
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM discovery_catalogs WHERE id = ?")) {
                stmt.setObject(1, catalogId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM discovery_catalog_items WHERE id = ?")) {
                stmt.setObject(1, itemId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }
        }
    }
}
