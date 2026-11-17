package br.com.sellerintelligence.mcp.service;

import br.com.sellerintelligence.mcp.domain.McpToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpToolRegistryTest {

    private McpToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new McpToolRegistry();
    }

    @Test
    @DisplayName("Should list all 5 approved read-only tools with strictly readOnly = true")
    void shouldListRegisteredReadOnlyTools() {
        List<McpToolDefinition> tools = registry.listTools();
        assertThat(tools).hasSize(5);

        List<String> toolNames = tools.stream().map(McpToolDefinition::name).toList();
        assertThat(toolNames).containsExactlyInAnyOrder(
                "search_market",
                "query_tenant_products",
                "query_sales_indicators",
                "calculate_sku_margin",
                "query_reconciliation_summary"
        );

        for (McpToolDefinition def : tools) {
            assertThat(def.readOnly()).isTrue();
            assertThat(def.inputSchema()).isNotEmpty();
            assertThat(def.inputSchema().get("type")).isEqualTo("object");
        }
    }

    @Test
    @DisplayName("Should retrieve registered tool by name with complete schema")
    void shouldGetToolByName() {
        Optional<McpToolDefinition> tool = registry.getTool("calculate_sku_margin");
        assertThat(tool).isPresent();
        assertThat(tool.get().name()).isEqualTo("calculate_sku_margin");
        assertThat(tool.get().readOnly()).isTrue();
        assertThat(tool.get().description()).contains("Calcula a margem de contribuição determinística");
    }

    @Test
    @DisplayName("Should detect and classify forbidden mutable operations")
    void shouldDetectForbiddenMutableOperations() {
        assertThat(registry.isForbiddenMutableOperation("publish_listing")).isTrue();
        assertThat(registry.isForbiddenMutableOperation("update_price")).isTrue();
        assertThat(registry.isForbiddenMutableOperation("delete_order")).isTrue();
        assertThat(registry.isForbiddenMutableOperation("modify_stock")).isTrue();
        assertThat(registry.isForbiddenMutableOperation("create_campaign")).isTrue();
        assertThat(registry.isForbiddenMutableOperation("execute_sql")).isTrue();
        assertThat(registry.isForbiddenMutableOperation("alter_table")).isTrue();

        // Safe read-only operations
        assertThat(registry.isForbiddenMutableOperation("search_market")).isFalse();
        assertThat(registry.isForbiddenMutableOperation("query_sales_indicators")).isFalse();
        assertThat(registry.isForbiddenMutableOperation("calculate_sku_margin")).isFalse();
    }

    @Test
    @DisplayName("Should reject instantiation of non-readOnly McpToolDefinition")
    void shouldRejectMutableToolDefinition() {
        assertThatThrownBy(() -> new McpToolDefinition("mutate_something", "Mutates data", false, java.util.Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MCP tools in this server must be strictly readOnly");
    }
}
