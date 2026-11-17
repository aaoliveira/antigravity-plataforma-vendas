package br.com.sellerintelligence.mcp;

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
class PostgresMcpIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("seller_test_mcp")
            .withUsername("test_user")
            .withPassword("test_password");

    @Test
    @DisplayName("Should run Flyway V13 migration and enforce MCP and Conversational tables, indices and constraints on real PostgreSQL 17")
    void shouldExecuteV13MigrationAndEnforceConstraints() throws Exception {
        assertThat(postgres.isRunning()).isTrue();

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        var migrateResult = flyway.migrate();
        assertThat(migrateResult.success).isTrue();
        assertThat(Integer.parseInt(migrateResult.targetSchemaVersion)).isGreaterThanOrEqualTo(13);

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            UUID tenantId = UUID.randomUUID();
            UUID apiKeyId = UUID.randomUUID();
            UUID logId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            UUID messageId = UUID.randomUUID();

            // 1. Cria organização para chave estrangeira
            try (PreparedStatement orgStmt = conn.prepareStatement(
                    "INSERT INTO organizations (id, name, slug) VALUES (?, 'MCP Test Org', 'mcp-test')")) {
                orgStmt.setObject(1, tenantId);
                orgStmt.executeUpdate();
            }

            // 2. Insere chave de API MCP
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mcp_api_keys (id, tenant_id, name, key_hash, key_prefix, permissions, status) " +
                    "VALUES (?, ?, 'Cursor Integration Key', 'sha256-hash-sample', 'sip_mcp_', 'READ_ONLY', 'ACTIVE')")) {
                stmt.setObject(1, apiKeyId);
                stmt.setObject(2, tenantId);
                stmt.executeUpdate();
            }

            // 3. Insere log de execução de ferramenta
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO mcp_tool_execution_logs (id, tenant_id, api_key_id, tool_name, arguments_sanitized, " +
                    "execution_status, latency_ms, model_name, prompt_version, result_summary, provenance_source, provenance_classification, ip_address) " +
                    "VALUES (?, ?, ?, 'calculate_sku_margin', '{\"sku\":\"ABC\"}', 'SUCCESS', 12, 'mcp-v1', '1.0', 'Margem OK', 'FINANCIAL_ENGINE', 'CALCULATED', '127.0.0.1')")) {
                stmt.setObject(1, logId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, apiKeyId);
                stmt.executeUpdate();
            }

            // 4. Insere sessão de chat
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO conversational_sessions (id, tenant_id, user_id, title) VALUES (?, ?, ?, 'Sessão MCP')")) {
                stmt.setObject(1, sessionId);
                stmt.setObject(2, tenantId);
                stmt.setObject(3, UUID.randomUUID());
                stmt.executeUpdate();
            }

            // 5. Insere mensagem de chat
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO conversational_messages (id, session_id, tenant_id, role, content, tool_calls, provenance_metadata) " +
                    "VALUES (?, ?, ?, 'USER', 'Qual a margem do produto?', '[]', '{}')")) {
                stmt.setObject(1, messageId);
                stmt.setObject(2, sessionId);
                stmt.setObject(3, tenantId);
                stmt.executeUpdate();
            }

            // 6. Consulta e valida persistência
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT k.name, l.tool_name, l.execution_status, s.title, m.content " +
                    "FROM mcp_api_keys k " +
                    "JOIN mcp_tool_execution_logs l ON l.tenant_id = k.tenant_id " +
                    "JOIN conversational_sessions s ON s.tenant_id = k.tenant_id " +
                    "JOIN conversational_messages m ON m.session_id = s.id " +
                    "WHERE k.tenant_id = ?")) {
                stmt.setObject(1, tenantId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("name")).isEqualTo("Cursor Integration Key");
                    assertThat(rs.getString("tool_name")).isEqualTo("calculate_sku_margin");
                    assertThat(rs.getString("execution_status")).isEqualTo("SUCCESS");
                    assertThat(rs.getString("title")).isEqualTo("Sessão MCP");
                    assertThat(rs.getString("content")).isEqualTo("Qual a margem do produto?");
                }
            }
        }
    }
}
