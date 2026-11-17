package br.com.sellerintelligence.mcp.service;

import br.com.sellerintelligence.mcp.domain.McpToolDefinition;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class McpToolRegistry {

    private final Map<String, McpToolDefinition> tools = new LinkedHashMap<>();

    private static final Set<String> FORBIDDEN_MUTABLE_OPERATIONS = Set.of(
            "publish_listing", "update_price", "delete_order", "modify_stock",
            "create_campaign", "update_campaign", "delete_listing", "change_setting",
            "execute_sql", "run_query", "update_database", "alter_table"
    );

    public McpToolRegistry() {
        registerTools();
    }

    private void registerTools() {
        // 1. search_market
        Map<String, Object> searchMarketSchema = new LinkedHashMap<>();
        searchMarketSchema.put("type", "object");
        Map<String, Object> searchMarketProps = new LinkedHashMap<>();
        searchMarketProps.put("query", Map.of("type", "string", "description", "Termo de busca de produto no marketplace"));
        searchMarketProps.put("marketplace", Map.of("type", "string", "enum", List.of("MERCADO_LIVRE", "SHOPEE", "AMAZON"), "description", "Marketplace alvo"));
        searchMarketProps.put("limit", Map.of("type", "integer", "description", "Quantidade máxima de resultados (1 a 50)"));
        searchMarketSchema.put("properties", searchMarketProps);
        searchMarketSchema.put("required", List.of("query"));

        tools.put("search_market", new McpToolDefinition(
                "search_market",
                "Pesquisa produtos, tendências e anúncios concorrentes no marketplace utilizando dados públicos observáveis.",
                true,
                searchMarketSchema
        ));

        // 2. query_tenant_products
        Map<String, Object> tenantProductsSchema = new LinkedHashMap<>();
        tenantProductsSchema.put("type", "object");
        Map<String, Object> tenantProductsProps = new LinkedHashMap<>();
        tenantProductsProps.put("marketplace", Map.of("type", "string", "description", "Filtro por marketplace (opcional)"));
        tenantProductsProps.put("status", Map.of("type", "string", "description", "Filtro por status do anúncio (ex: ACTIVE, PAUSED)"));
        tenantProductsProps.put("limit", Map.of("type", "integer", "description", "Limite de itens"));
        tenantProductsSchema.put("properties", tenantProductsProps);

        tools.put("query_tenant_products", new McpToolDefinition(
                "query_tenant_products",
                "Consulta o catálogo de produtos e anúncios próprios cadastrados no tenant atual com estoque e preços.",
                true,
                tenantProductsSchema
        ));

        // 3. query_sales_indicators
        Map<String, Object> salesSchema = new LinkedHashMap<>();
        salesSchema.put("type", "object");
        Map<String, Object> salesProps = new LinkedHashMap<>();
        salesProps.put("startDate", Map.of("type", "string", "format", "date", "description", "Data inicial do período (YYYY-MM-DD)"));
        salesProps.put("endDate", Map.of("type", "string", "format", "date", "description", "Data final do período (YYYY-MM-DD)"));
        salesProps.put("marketplace", Map.of("type", "string", "description", "Canal de venda específico ou TODOS"));
        salesSchema.put("properties", salesProps);

        tools.put("query_sales_indicators", new McpToolDefinition(
                "query_sales_indicators",
                "Consulta indicadores agregados de vendas do tenant (faturamento, quantidade de pedidos e ticket médio).",
                true,
                salesSchema
        ));

        // 4. calculate_sku_margin
        Map<String, Object> marginSchema = new LinkedHashMap<>();
        marginSchema.put("type", "object");
        Map<String, Object> marginProps = new LinkedHashMap<>();
        marginProps.put("sku", Map.of("type", "string", "description", "Código SKU do produto"));
        marginProps.put("salePrice", Map.of("type", "number", "description", "Preço de venda simulado em R$"));
        marginProps.put("marketplace", Map.of("type", "string", "description", "Marketplace para apuração das comissões"));
        marginSchema.put("properties", marginProps);
        marginSchema.put("required", List.of("sku", "salePrice"));

        tools.put("calculate_sku_margin", new McpToolDefinition(
                "calculate_sku_margin",
                "Calcula a margem de contribuição determinística, impostos e lucro líquido para um SKU com base no motor financeiro.",
                true,
                marginSchema
        ));

        // 5. query_reconciliation_summary
        Map<String, Object> reconcilSchema = new LinkedHashMap<>();
        reconcilSchema.put("type", "object");
        Map<String, Object> reconcilProps = new LinkedHashMap<>();
        reconcilProps.put("marketplace", Map.of("type", "string", "description", "Marketplace do extrato de repasse"));
        reconcilProps.put("startDate", Map.of("type", "string", "description", "Data inicial"));
        reconcilProps.put("endDate", Map.of("type", "string", "description", "Data final"));
        reconcilSchema.put("properties", reconcilProps);

        tools.put("query_reconciliation_summary", new McpToolDefinition(
                "query_reconciliation_summary",
                "Consulta extratos de repasse, liquidações e lançamentos divergentes da conciliação financeira.",
                true,
                reconcilSchema
        ));
    }

    public List<McpToolDefinition> listTools() {
        return new ArrayList<>(tools.values());
    }

    public Optional<McpToolDefinition> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public boolean isForbiddenMutableOperation(String toolName) {
        if (toolName == null) return false;
        String lower = toolName.trim().toLowerCase();
        return FORBIDDEN_MUTABLE_OPERATIONS.contains(lower) ||
                lower.startsWith("update_") ||
                lower.startsWith("delete_") ||
                lower.startsWith("create_") ||
                lower.startsWith("publish_") ||
                lower.startsWith("modify_") ||
                lower.contains("sql") ||
                lower.contains("exec");
    }
}
