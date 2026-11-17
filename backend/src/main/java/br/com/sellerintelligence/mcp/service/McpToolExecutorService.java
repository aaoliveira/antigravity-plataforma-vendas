package br.com.sellerintelligence.mcp.service;

import br.com.sellerintelligence.discovery.security.UntrustedTextSanitizer;
import br.com.sellerintelligence.financial.domain.SkuCost;
import br.com.sellerintelligence.financial.domain.SkuCostRepository;
import br.com.sellerintelligence.market.provider.MarketSearchQuery;
import br.com.sellerintelligence.market.provider.MarketSearchResult;
import br.com.sellerintelligence.market.service.MarketIntelligenceService;
import br.com.sellerintelligence.mcp.domain.*;
import br.com.sellerintelligence.mcp.web.dto.McpProvenanceDto;
import br.com.sellerintelligence.mcp.web.dto.McpToolCallRequest;
import br.com.sellerintelligence.mcp.web.dto.McpToolCallResponse;
import br.com.sellerintelligence.reconciliation.domain.ReconciliationSettlement;
import br.com.sellerintelligence.reconciliation.domain.ReconciliationSettlementRepository;
import br.com.sellerintelligence.sync.domain.Listing;
import br.com.sellerintelligence.sync.domain.ListingRepository;
import br.com.sellerintelligence.sync.domain.Order;
import br.com.sellerintelligence.sync.domain.OrderRepository;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class McpToolExecutorService {

    private static final Logger log = LoggerFactory.getLogger(McpToolExecutorService.class);

    private final McpToolRegistry toolRegistry;
    private final McpToolExecutionLogRepository executionLogRepository;
    private final UntrustedTextSanitizer textSanitizer;

    // Domain services & repositories
    private final MarketIntelligenceService marketService;
    private final ListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final SkuCostRepository skuCostRepository;
    private final ReconciliationSettlementRepository settlementRepository;

    public McpToolExecutorService(McpToolRegistry toolRegistry,
                                  McpToolExecutionLogRepository executionLogRepository,
                                  UntrustedTextSanitizer textSanitizer,
                                  MarketIntelligenceService marketService,
                                  ListingRepository listingRepository,
                                  OrderRepository orderRepository,
                                  SkuCostRepository skuCostRepository,
                                  ReconciliationSettlementRepository settlementRepository) {
        this.toolRegistry = toolRegistry;
        this.executionLogRepository = executionLogRepository;
        this.textSanitizer = textSanitizer;
        this.marketService = marketService;
        this.listingRepository = listingRepository;
        this.orderRepository = orderRepository;
        this.skuCostRepository = skuCostRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public McpToolCallResponse executeTool(UUID tenantId, UUID userId, UUID apiKeyId,
                                          McpToolCallRequest request, String ipAddress) {
        long startTime = System.currentTimeMillis();
        String toolName = request.toolName() != null ? request.toolName().trim() : "";
        Map<String, Object> rawArgs = request.arguments() != null ? request.arguments() : Collections.emptyMap();

        log.info("Executing MCP tool: tenantId={}, user={}, toolName={}", tenantId, userId, toolName);

        // 1. Verificação de Operação Mutável Proibida
        if (toolRegistry.isForbiddenMutableOperation(toolName)) {
            long latency = System.currentTimeMillis() - startTime;
            String errorMsg = "Operação mutável '" + toolName + "' é estritamente proibida no servidor MCP read-only. Ações mutáveis exigem confirmação explícita na UI.";
            logSecurityBlock(tenantId, userId, apiKeyId, toolName, rawArgs, McpExecutionStatus.BLOCKED_MUTABLE_DENIED, latency, errorMsg, ipAddress);

            return new McpToolCallResponse(
                    false, toolName, null, errorMsg, McpExecutionStatus.BLOCKED_MUTABLE_DENIED,
                    new McpProvenanceDto("MCP_SERVER", "UNAVAILABLE", OffsetDateTime.now().toString(), "ORGANIZATION", "Operações mutáveis bloqueadas por governança"),
                    latency
            );
        }

        // 2. Verificação se a ferramenta existe no catálogo registrado
        Optional<McpToolDefinition> toolDefOpt = toolRegistry.getTool(toolName);
        if (toolDefOpt.isEmpty()) {
            long latency = System.currentTimeMillis() - startTime;
            String errorMsg = "Ferramenta '" + toolName + "' não encontrada no catálogo de ferramentas read-only do MCP.";
            logSecurityBlock(tenantId, userId, apiKeyId, toolName, rawArgs, McpExecutionStatus.ERROR, latency, errorMsg, ipAddress);

            return new McpToolCallResponse(
                    false, toolName, null, errorMsg, McpExecutionStatus.ERROR,
                    new McpProvenanceDto("MCP_SERVER", "UNAVAILABLE", OffsetDateTime.now().toString(), "ORGANIZATION", "Ferramenta desconhecida"),
                    latency
            );
        }

        // 3. Verificação Adversarial Anti-Prompt Injection (OWASP LLM01)
        for (Map.Entry<String, Object> entry : rawArgs.entrySet()) {
            if (entry.getValue() instanceof String strVal) {
                if (textSanitizer.containsSuspiciousInstructions(strVal)) {
                    long latency = System.currentTimeMillis() - startTime;
                    String errorMsg = "Tentativa de Prompt Injection ou manipulação adversarial detectada no argumento '" + entry.getKey() + "'. Execução bloqueada.";
                    logSecurityBlock(tenantId, userId, apiKeyId, toolName, rawArgs, McpExecutionStatus.BLOCKED_INJECTION, latency, errorMsg, ipAddress);

                    return new McpToolCallResponse(
                            false, toolName, null, errorMsg, McpExecutionStatus.BLOCKED_INJECTION,
                            new McpProvenanceDto("MCP_SECURITY_FILTER", "UNAVAILABLE", OffsetDateTime.now().toString(), "ORGANIZATION", "Bloqueado por filtro de segurança anti-injection"),
                            latency
                    );
                }
            }
        }

        // 4. Verificação Estrita de Isolamento Cross-Tenant (Anti-IDOR)
        if (rawArgs.containsKey("tenant_id") || rawArgs.containsKey("tenantId")) {
            Object foreignTenantObj = rawArgs.getOrDefault("tenant_id", rawArgs.get("tenantId"));
            if (foreignTenantObj != null) {
                String foreignTenantStr = foreignTenantObj.toString().trim();
                if (!foreignTenantStr.equalsIgnoreCase(tenantId.toString())) {
                    long latency = System.currentTimeMillis() - startTime;
                    String errorMsg = "Tentativa de acesso cross-tenant detectada: tenant autenticado [" + tenantId + "] difere do solicitado [" + foreignTenantStr + "]. Execução bloqueada.";
                    logSecurityBlock(tenantId, userId, apiKeyId, toolName, rawArgs, McpExecutionStatus.BLOCKED_CROSS_TENANT, latency, errorMsg, ipAddress);

                    return new McpToolCallResponse(
                            false, toolName, null, errorMsg, McpExecutionStatus.BLOCKED_CROSS_TENANT,
                            new McpProvenanceDto("MCP_SECURITY_FILTER", "UNAVAILABLE", OffsetDateTime.now().toString(), "ORGANIZATION", "Isolamento multi-tenant violado"),
                            latency
                    );
                }
            }
        }

        // 5. Execução Determinística da Ferramenta Read-Only
        try {
            McpToolCallResponse response = switch (toolName) {
                case "search_market" -> executeSearchMarket(tenantId, rawArgs, startTime);
                case "query_tenant_products" -> executeQueryTenantProducts(tenantId, rawArgs, startTime);
                case "query_sales_indicators" -> executeQuerySalesIndicators(tenantId, rawArgs, startTime);
                case "calculate_sku_margin" -> executeCalculateSkuMargin(tenantId, rawArgs, startTime);
                case "query_reconciliation_summary" -> executeQueryReconciliationSummary(tenantId, rawArgs, startTime);
                default -> throw new IllegalArgumentException("Unhandled tool: " + toolName);
            };

            // Registra log de sucesso na auditoria
            McpToolExecutionLog executionLog = new McpToolExecutionLog(
                    tenantId, userId, apiKeyId, toolName,
                    sanitizeArgsToJson(rawArgs), McpExecutionStatus.SUCCESS,
                    response.latencyMs(), "mcp-server-v1", "1.0",
                    "Execução bem-sucedida: " + toolName,
                    response.provenance() != null ? response.provenance().source() : "SIP_BACKEND",
                    response.provenance() != null ? response.provenance().classification() : "OBSERVED",
                    ipAddress
            );
            executionLogRepository.save(executionLog);

            return response;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Error executing MCP tool: toolName={}, error={}", toolName, e.getMessage(), e);
            logSecurityBlock(tenantId, userId, apiKeyId, toolName, rawArgs, McpExecutionStatus.ERROR, latency, e.getMessage(), ipAddress);

            return new McpToolCallResponse(
                    false, toolName, null, "Falha na execução da ferramenta: " + e.getMessage(),
                    McpExecutionStatus.ERROR,
                    new McpProvenanceDto("MCP_SERVER", "UNAVAILABLE", OffsetDateTime.now().toString(), "ORGANIZATION", "Falha interna controlada"),
                    latency
            );
        }
    }

    // --- FERRAMENTA 1: search_market ---
    private McpToolCallResponse executeSearchMarket(UUID tenantId, Map<String, Object> args, long startTime) {
        String query = (String) args.get("query");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Parâmetro 'query' é obrigatório para search_market.");
        }

        String mktStr = (String) args.getOrDefault("marketplace", "MERCADO_LIVRE");
        Marketplace marketplace = Marketplace.MERCADO_LIVRE;
        if ("SHOPEE".equalsIgnoreCase(mktStr)) {
            marketplace = Marketplace.SHOPEE;
        }

        int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 10;
        MarketSearchQuery searchQuery = new MarketSearchQuery(query, null, null, null, null, null, null, 0, limit);

        MarketSearchResult result = marketService.executeSearch(tenantId, marketplace, searchQuery, true);
        long latency = System.currentTimeMillis() - startTime;

        McpProvenanceDto provenance = new McpProvenanceDto(
                "API Oficial " + marketplace.name(),
                "OBSERVED",
                OffsetDateTime.now().toString(),
                "PUBLIC_MARKETPLACE",
                "Dados observados via busca pública; não inclui dados privados ou faturamento oculto de concorrentes."
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("marketplace", marketplace.name());
        data.put("query", query);
        data.put("totalResults", result.total());
        data.put("items", result.items());

        return new McpToolCallResponse(true, "search_market", data, null, McpExecutionStatus.SUCCESS, provenance, latency);
    }

    // --- FERRAMENTA 2: query_tenant_products ---
    private McpToolCallResponse executeQueryTenantProducts(UUID tenantId, Map<String, Object> args, long startTime) {
        List<Listing> listings = listingRepository.findByTenantId(tenantId);
        long latency = System.currentTimeMillis() - startTime;

        int limit = args.containsKey("limit") ? ((Number) args.get("limit")).intValue() : 20;

        List<Map<String, Object>> items = listings.stream()
                .limit(limit)
                .map(l -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", l.getId().toString());
                    map.put("marketplace", l.getMarketplace() != null ? l.getMarketplace().name() : "DESCONHECIDO");
                    map.put("externalListingId", l.getExternalListingId());
                    map.put("title", l.getTitle());
                    map.put("price", l.getPrice());
                    map.put("status", l.getStatus() != null ? l.getStatus() : "ACTIVE");
                    map.put("availableQuantity", l.getAvailableQuantity());
                    return map;
                })
                .toList();

        McpProvenanceDto provenance = new McpProvenanceDto(
                "Catálogo de Anúncios do Tenant (Sincronizado)",
                "IMPORTED",
                OffsetDateTime.now().toString(),
                "TENANT_ORGANIZATION",
                "Anúncios importados das lojas conectadas do tenant via conector oficial."
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalFound", listings.size());
        data.put("returnedCount", items.size());
        data.put("products", items);

        return new McpToolCallResponse(true, "query_tenant_products", data, null, McpExecutionStatus.SUCCESS, provenance, latency);
    }

    // --- FERRAMENTA 3: query_sales_indicators ---
    private McpToolCallResponse executeQuerySalesIndicators(UUID tenantId, Map<String, Object> args, long startTime) {
        Instant now = Instant.now();
        Instant past = now.minus(30, ChronoUnit.DAYS);

        if (args.containsKey("startDate") && args.get("startDate") != null) {
            past = parseInstant(args.get("startDate").toString(), past);
        }
        if (args.containsKey("endDate") && args.get("endDate") != null) {
            now = parseInstant(args.get("endDate").toString(), now);
        }

        List<Order> orders = orderRepository.findByTenantIdAndOrderDateBetween(tenantId, past, now);
        long latency = System.currentTimeMillis() - startTime;

        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        BigDecimal totalShipping = BigDecimal.ZERO;
        int orderCount = orders.size();

        for (Order o : orders) {
            if (!"CANCELLED".equalsIgnoreCase(o.getStatus())) {
                if (o.getTotalAmount() != null) grossRevenue = grossRevenue.add(o.getTotalAmount());
                if (o.getMarketplaceFee() != null) totalFees = totalFees.add(o.getMarketplaceFee());
                if (o.getShippingCostSeller() != null) totalShipping = totalShipping.add(o.getShippingCostSeller());
            }
        }

        BigDecimal averageTicket = orderCount > 0
                ? grossRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO;

        McpProvenanceDto provenance = new McpProvenanceDto(
                "Base Sincronizada de Pedidos do Tenant",
                "IMPORTED",
                OffsetDateTime.now().toString(),
                "TENANT_ORDERS",
                "Vendas apuradas por regime de competência com base na data de criação do pedido."
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("periodStart", past.toString());
        data.put("periodEnd", now.toString());
        data.put("totalOrders", orderCount);
        data.put("grossRevenue", grossRevenue.setScale(2, RoundingMode.HALF_EVEN));
        data.put("averageTicket", averageTicket);
        data.put("estimatedMarketplaceFees", totalFees.setScale(2, RoundingMode.HALF_EVEN));
        data.put("estimatedShippingCosts", totalShipping.setScale(2, RoundingMode.HALF_EVEN));

        return new McpToolCallResponse(true, "query_sales_indicators", data, null, McpExecutionStatus.SUCCESS, provenance, latency);
    }

    // --- FERRAMENTA 4: calculate_sku_margin ---
    private McpToolCallResponse executeCalculateSkuMargin(UUID tenantId, Map<String, Object> args, long startTime) {
        String sku = (String) args.get("sku");
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("Parâmetro 'sku' é obrigatório.");
        }

        Number salePriceNum = (Number) args.get("salePrice");
        if (salePriceNum == null) {
            throw new IllegalArgumentException("Parâmetro 'salePrice' é obrigatório.");
        }
        BigDecimal salePrice = new BigDecimal(salePriceNum.toString()).setScale(2, RoundingMode.HALF_EVEN);

        // Busca custo efetivo cadastrado no banco para este tenant e SKU
        Optional<SkuCost> costOpt = skuCostRepository.findEffectiveCost(tenantId, sku, Instant.now());
        BigDecimal costPrice = costOpt.map(SkuCost::getCostPrice).orElse(BigDecimal.ZERO);
        boolean costRegistered = costOpt.isPresent();

        // Cálculo determinístico em BigDecimal com comissões e impostos padrão
        BigDecimal commissionRate = new BigDecimal("0.1600"); // 16% comissão padrão
        BigDecimal taxRate = new BigDecimal("0.0600");        // 6% Simples Nacional padrão
        BigDecimal shippingCost = salePrice.compareTo(new BigDecimal("79.00")) >= 0
                ? new BigDecimal("18.50")
                : BigDecimal.ZERO;

        BigDecimal commissionAmount = salePrice.multiply(commissionRate).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal taxAmount = salePrice.multiply(taxRate).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal netRevenue = salePrice.subtract(commissionAmount).subtract(taxAmount).subtract(shippingCost);
        BigDecimal contributionMargin = netRevenue.subtract(costPrice);

        BigDecimal marginPercentage = salePrice.compareTo(BigDecimal.ZERO) > 0
                ? contributionMargin.divide(salePrice, 4, RoundingMode.HALF_EVEN).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO;

        // Preço mínimo para margem de contribuição zero
        BigDecimal totalFixedDeductionsRate = commissionRate.add(taxRate); // 0.22
        BigDecimal divisor = BigDecimal.ONE.subtract(totalFixedDeductionsRate);
        BigDecimal minPrice = (costPrice.add(shippingCost)).divide(divisor, 2, RoundingMode.HALF_EVEN);

        long latency = System.currentTimeMillis() - startTime;

        McpProvenanceDto provenance = new McpProvenanceDto(
                "Motor Financeiro Determinístico (FinancialEngine)",
                costRegistered ? "CALCULATED" : "ESTIMATED",
                OffsetDateTime.now().toString(),
                "SKU_FINANCIAL_RULE",
                costRegistered
                        ? "Cálculo exato em BigDecimal baseado no custo cadastrado de R$ " + costPrice
                        : "Custo do SKU não cadastrado no banco; margem calculada assumindo custo R$ 0,00."
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sku", sku);
        data.put("salePrice", salePrice);
        data.put("unitCost", costPrice);
        data.put("costRegisteredInSystem", costRegistered);
        data.put("commissionAmount", commissionAmount);
        data.put("taxAmount", taxAmount);
        data.put("shippingCost", shippingCost);
        data.put("contributionMargin", contributionMargin);
        data.put("marginPercentage", marginPercentage + "%");
        data.put("minimumPriceTarget", minPrice);

        return new McpToolCallResponse(true, "calculate_sku_margin", data, null, McpExecutionStatus.SUCCESS, provenance, latency);
    }

    // --- FERRAMENTA 5: query_reconciliation_summary ---
    private McpToolCallResponse executeQueryReconciliationSummary(UUID tenantId, Map<String, Object> args, long startTime) {
        List<ReconciliationSettlement> settlements = settlementRepository.findByTenantIdOrderBySettlementDateDesc(tenantId);
        long latency = System.currentTimeMillis() - startTime;

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;
        int batchesCount = settlements.size();
        int conciledTotal = 0;
        int discrepantTotal = 0;

        for (ReconciliationSettlement s : settlements) {
            if (s.getGrossAmount() != null) totalGross = totalGross.add(s.getGrossAmount());
            if (s.getNetAmount() != null) totalNet = totalNet.add(s.getNetAmount());
            if (s.getTotalFees() != null) totalFees = totalFees.add(s.getTotalFees());
            conciledTotal += s.getReconciledItemsCount();
            discrepantTotal += s.getDiscrepantItemsCount();
        }

        McpProvenanceDto provenance = new McpProvenanceDto(
                "Módulo de Conciliação Financeira de Repasses",
                "IMPORTED",
                OffsetDateTime.now().toString(),
                "TENANT_SETTLEMENTS",
                "Demonstrativo gerencial de liquidações bancárias e retenções de marketplace."
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalSettlementBatches", batchesCount);
        data.put("totalGrossLiquidated", totalGross.setScale(2, RoundingMode.HALF_EVEN));
        data.put("totalNetReceived", totalNet.setScale(2, RoundingMode.HALF_EVEN));
        data.put("totalRetainedFees", totalFees.setScale(2, RoundingMode.HALF_EVEN));
        data.put("conciliatedItemsCount", conciledTotal);
        data.put("discrepantItemsCount", discrepantTotal);

        return new McpToolCallResponse(true, "query_reconciliation_summary", data, null, McpExecutionStatus.SUCCESS, provenance, latency);
    }

    private void logSecurityBlock(UUID tenantId, UUID userId, UUID apiKeyId, String toolName,
                                  Map<String, Object> rawArgs, McpExecutionStatus status,
                                  long latency, String errorMsg, String ipAddress) {
        McpToolExecutionLog log = new McpToolExecutionLog(
                tenantId, userId, apiKeyId, toolName,
                sanitizeArgsToJson(rawArgs), status, latency,
                "mcp-security-gate", "1.0", errorMsg,
                "MCP_SECURITY_GATE", "UNAVAILABLE", ipAddress
        );
        executionLogRepository.save(log);
    }

    private String sanitizeArgsToJson(Map<String, Object> args) {
        if (args == null || args.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> e : args.entrySet()) {
            if (sb.length() > 1) sb.append(", ");
            // Mascarar chaves secretas ou senhas se presentes
            String k = e.getKey();
            Object v = e.getValue();
            if (k.toLowerCase().contains("secret") || k.toLowerCase().contains("password") || k.toLowerCase().contains("token")) {
                v = "[PROTECTED_SECRET]";
            }
            sb.append("\"").append(k).append("\":\"").append(v).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private Instant parseInstant(String dateStr, Instant fallback) {
        try {
            if (dateStr.length() == 10) {
                return Instant.parse(dateStr + "T00:00:00Z");
            }
            return Instant.parse(dateStr);
        } catch (Exception e) {
            return fallback;
        }
    }
}
