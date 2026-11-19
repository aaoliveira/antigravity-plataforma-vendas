package com.sellerintelligence.modules.aiassistant.tools;

import com.sellerintelligence.modules.catalog.MarketplaceListing;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.listinganalytics.model.MetricsSummaryDto;
import com.sellerintelligence.modules.listinganalytics.service.AccountMetricIngestionService;
import com.sellerintelligence.modules.pricing.model.PricingCalculationRequest;
import com.sellerintelligence.modules.pricing.model.PricingCalculationResult;
import com.sellerintelligence.modules.pricing.service.PricingEngineService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ferramentas internas declarativas expostas para Tool Calling pelo LLM.
 * Todas as operações validam estritamente o tenantId autenticado e operam exclusivamente em modo READ-ONLY.
 */
@Component
public class InternalAiTools {

    private final MarketplaceListingRepository listingRepository;
    private final PricingEngineService pricingEngineService;
    private final AccountMetricIngestionService metricIngestionService;

    public InternalAiTools(
            MarketplaceListingRepository listingRepository,
            PricingEngineService pricingEngineService,
            AccountMetricIngestionService metricIngestionService
    ) {
        this.listingRepository = listingRepository;
        this.pricingEngineService = pricingEngineService;
        this.metricIngestionService = metricIngestionService;
    }

    public List<AiTool> getAllTools() {
        return List.of(
                new SearchListingsTool(),
                new CalculateMarginTool(),
                new SummarizeMetricsTool()
        );
    }

    /**
     * Tool 1: Pesquisa de Anúncios Próprios do Tenant
     */
    private class SearchListingsTool implements AiTool {
        @Override
        public String getName() {
            return "search_listings";
        }

        @Override
        public String getDescription() {
            return "Pesquisa anúncios do catálogo próprio do vendedor por marketplace ou termo de busca. Retorna título, preço, SKU e status.";
        }

        @Override
        public Map<String, Object> getParametersSchema() {
            return Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "search", Map.of("type", "string", "description", "Termo de busca (título, SKU ou ID do anúncio)"),
                            "marketplace", Map.of("type", "string", "description", "Canal de venda (MERCADO_LIVRE ou SHOPEE)"),
                            "limit", Map.of("type", "integer", "description", "Quantidade máxima de registros (1 a 20)")
                    )
            );
        }

        @Override
        public ToolExecutionResult execute(UUID authenticatedTenantId, Map<String, Object> arguments) {
            String search = arguments.get("search") != null ? arguments.get("search").toString().trim() : null;
            String marketplace = arguments.get("marketplace") != null ? arguments.get("marketplace").toString().trim() : null;
            int limit = 10;
            if (arguments.get("limit") instanceof Number n) {
                limit = Math.min(Math.max(n.intValue(), 1), 20);
            }

            Page<MarketplaceListing> page = listingRepository.searchListings(
                    authenticatedTenantId,
                    marketplace,
                    null,
                    null,
                    search,
                    PageRequest.of(0, limit)
            );

            if (page.isEmpty()) {
                return ToolExecutionResult.unavailable(
                        "Nenhum anúncio encontrado no catálogo para o tenant informado com os filtros aplicados.",
                        List.of("catalog:marketplace_listings")
                );
            }

            List<Map<String, Object>> records = new ArrayList<>();
            for (MarketplaceListing l : page.getContent()) {
                Map<String, Object> item = new HashMap<>();
                item.put("listingId", l.getId().toString());
                item.put("externalListingId", l.getExternalListingId());
                item.put("marketplace", l.getMarketplace());
                item.put("title", l.getTitle());
                item.put("price", l.getPrice());
                item.put("status", l.getStatus());
                item.put("permalink", l.getPermalink());
                records.add(item);
            }

            return ToolExecutionResult.success(records, List.of("catalog:marketplace_listings"));
        }
    }

    /**
     * Tool 2: Cálculo Determinístico de Margem de Contribuição e Preço de Equilíbrio
     */
    private class CalculateMarginTool implements AiTool {
        @Override
        public String getName() {
            return "calculate_margin";
        }

        @Override
        public String getDescription() {
            return "Calcula matematicamente a margem de contribuição (R$ e %), deduções de taxas e preço de equilíbrio via motor determinístico em BigDecimal. Jamais rotula como lucro líquido.";
        }

        @Override
        public Map<String, Object> getParametersSchema() {
            return Map.of(
                    "type", "object",
                    "required", List.of("sellingPrice", "unitCost"),
                    "properties", Map.of(
                            "sellingPrice", Map.of("type", "number", "description", "Preço de venda pretendido (BRL)"),
                            "unitCost", Map.of("type", "number", "description", "Custo unitário do produto / CMV (BRL)"),
                            "commissionPercent", Map.of("type", "number", "description", "Percentual de comissão do marketplace (opcional)"),
                            "fixedFee", Map.of("type", "number", "description", "Taxa fixa por item (opcional)"),
                            "shippingCost", Map.of("type", "number", "description", "Frete subsidiado pelo vendedor (opcional)"),
                            "taxPercent", Map.of("type", "number", "description", "Alíquota de impostos sobre venda % (opcional)")
                    )
            );
        }

        @Override
        public ToolExecutionResult execute(UUID authenticatedTenantId, Map<String, Object> arguments) {
            Object priceObj = arguments.get("sellingPrice");
            Object costObj = arguments.get("unitCost");

            if (priceObj == null || costObj == null) {
                return ToolExecutionResult.error("Os campos 'sellingPrice' e 'unitCost' são obrigatórios.");
            }

            BigDecimal sellingPrice = new BigDecimal(priceObj.toString());
            BigDecimal unitCost = new BigDecimal(costObj.toString());

            BigDecimal commissionPercent = arguments.get("commissionPercent") != null
                    ? new BigDecimal(arguments.get("commissionPercent").toString())
                    : new BigDecimal("16.00"); // Padrão Meli Clássico se não informado

            BigDecimal fixedFee = arguments.get("fixedFee") != null
                    ? new BigDecimal(arguments.get("fixedFee").toString())
                    : BigDecimal.ZERO;

            BigDecimal shippingCost = arguments.get("shippingCost") != null
                    ? new BigDecimal(arguments.get("shippingCost").toString())
                    : BigDecimal.ZERO;

            BigDecimal taxPercent = arguments.get("taxPercent") != null
                    ? new BigDecimal(arguments.get("taxPercent").toString())
                    : new BigDecimal("6.00"); // Simples Nacional padrão se não informado

            PricingCalculationRequest req = new PricingCalculationRequest(
                    sellingPrice,
                    BigDecimal.ZERO,
                    unitCost,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    shippingCost,
                    commissionPercent,
                    fixedFee,
                    BigDecimal.ZERO,
                    taxPercent,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    null,
                    null,
                    "BRL"
            );

            PricingCalculationResult res = pricingEngineService.calculateMargins(req);

            Map<String, Object> data = new HashMap<>();
            data.put("grossRevenue", res.grossSellingPrice());
            data.put("netRevenue", res.netSellingPrice());
            data.put("totalVariableDeductions", res.totalVariableDeductions());
            data.put("contributionMarginAmount", res.contributionMarginAmount());
            data.put("contributionMarginPercent", res.contributionMarginPercent());
            data.put("breakevenPrice", res.breakEvenPrice());
            data.put("formulaVersion", res.formulaVersion());
            data.put("guardrailDisclaimer", "Margem de Contribuição apurada deterministicamente. Despesas fixas não estão contempladas, não constituindo Lucro Líquido.");

            return ToolExecutionResult.success(data, List.of("pricing:PricingEngineService"));
        }
    }

    /**
     * Tool 3: Resumo de Métricas Oficiais do Canal
     */
    private class SummarizeMetricsTool implements AiTool {
        @Override
        public String getName() {
            return "summarize_metrics";
        }

        @Override
        public String getDescription() {
            return "Retorna o resumo oficial consolidado de métricas (visitas, pedidos, unidades, receita, conversão e CTR) persistidas no tenant para a janela de tempo.";
        }

        @Override
        public Map<String, Object> getParametersSchema() {
            return Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "marketplace", Map.of("type", "string", "description", "Marketplace (MERCADO_LIVRE ou SHOPEE)"),
                            "externalListingId", Map.of("type", "string", "description", "Código externo do anúncio (opcional)"),
                            "windowDays", Map.of("type", "integer", "description", "Janela em dias retroativos (ex: 7, 14, 30; padrão 30)")
                    )
            );
        }

        @Override
        public ToolExecutionResult execute(UUID authenticatedTenantId, Map<String, Object> arguments) {
            String marketplace = arguments.get("marketplace") != null ? arguments.get("marketplace").toString().trim() : null;
            String externalListingId = arguments.get("externalListingId") != null ? arguments.get("externalListingId").toString().trim() : null;
            int days = 30;
            if (arguments.get("windowDays") instanceof Number n) {
                days = Math.min(Math.max(n.intValue(), 1), 365);
            }

            Instant end = Instant.now();
            Instant start = end.minus(days, ChronoUnit.DAYS);

            MetricsSummaryDto summary = metricIngestionService.getSummary(
                    authenticatedTenantId,
                    null,
                    marketplace,
                    externalListingId,
                    start,
                    end
            );

            if (summary.totalMetricsRecorded() == 0) {
                return ToolExecutionResult.unavailable(
                        "Nenhuma métrica oficial encontrada para o filtro informado nesta janela temporal.",
                        List.of("analytics:connected_account_metrics")
                );
            }

            Map<String, Object> data = new HashMap<>();
            data.put("totalVisits", summary.totalVisits());
            data.put("totalOrders", summary.totalOrders());
            data.put("totalUnitsSold", summary.totalUnitsSold());
            data.put("totalRevenue", summary.totalRevenue());
            data.put("overallConversionRateOrders", summary.overallConversionRateOrders());
            data.put("overallCtr", summary.overallCtr());
            data.put("availableMetricsCount", summary.availableMetricsCount());
            data.put("unsupportedByChannelCount", summary.unsupportedByChannelCount());
            data.put("antiHallucinationNotice", summary.antiHallucinationNotice());

            if ("MERCADO_LIVRE".equalsIgnoreCase(marketplace)) {
                data.put("ctrChannelLimitation", "No tráfego orgânico do Mercado Livre, impressões e cliques não são providos na API do vendedor (exclusivos de Mercado Ads contratado). CTR não suportado organicamente.");
            }

            return ToolExecutionResult.success(data, List.of("analytics:connected_account_metrics"));
        }
    }
}
