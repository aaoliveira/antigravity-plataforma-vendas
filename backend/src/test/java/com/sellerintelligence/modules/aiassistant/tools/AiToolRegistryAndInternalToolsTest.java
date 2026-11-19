package com.sellerintelligence.modules.aiassistant.tools;

import com.sellerintelligence.modules.catalog.MarketplaceListing;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.listinganalytics.model.MetricsSummaryDto;
import com.sellerintelligence.modules.listinganalytics.service.AccountMetricIngestionService;
import com.sellerintelligence.modules.pricing.model.PricingCalculationRequest;
import com.sellerintelligence.modules.pricing.model.PricingCalculationResult;
import com.sellerintelligence.modules.pricing.service.PricingEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AiToolRegistryAndInternalToolsTest {

    private MarketplaceListingRepository listingRepository;
    private PricingEngineService pricingEngineService;
    private AccountMetricIngestionService metricIngestionService;

    private InternalAiTools internalAiTools;
    private AiToolRegistry toolRegistry;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID attackerTenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listingRepository = Mockito.mock(MarketplaceListingRepository.class);
        pricingEngineService = Mockito.mock(PricingEngineService.class);
        metricIngestionService = Mockito.mock(AccountMetricIngestionService.class);

        internalAiTools = new InternalAiTools(listingRepository, pricingEngineService, metricIngestionService);
        toolRegistry = new AiToolRegistry(internalAiTools);
    }

    @Test
    @DisplayName("Tool Registry bloqueia execução caso authenticatedTenantId seja nulo (Fail-Closed)")
    void shouldDenyAccessWhenTenantIdIsNull() {
        ToolExecutionResult result = toolRegistry.execute(
                "search_listings",
                null,
                List.of("search_listings"),
                Map.of("search", "Galaxy")
        );

        assertThat(result.status()).isEqualTo("ACCESS_DENIED");
        assertThat(result.unavailabilityReason()).contains("sem tenantId");
    }

    @Test
    @DisplayName("Tool Registry bloqueia execução caso a tool não conste na lista de tools autorizadas do template")
    void shouldDenyAccessWhenToolNotAllowedByPromptTemplate() {
        ToolExecutionResult result = toolRegistry.execute(
                "calculate_margin",
                tenantId,
                List.of("search_listings"), // Apenas search_listings autorizada
                Map.of("sellingPrice", new BigDecimal("100.00"))
        );

        assertThat(result.status()).isEqualTo("ACCESS_DENIED");
        assertThat(result.unavailabilityReason()).contains("não autorizada para o prompt atual");
    }

    @Test
    @DisplayName("Tool Registry dispara SecurityException caso argumentos fornecidos pelo LLM tentem injetar tenantId divergente")
    void shouldThrowSecurityExceptionOnInjectedTenantId() {
        Map<String, Object> maliciousArgs = Map.of(
                "search", "Galaxy",
                "tenantId", attackerTenantId.toString()
        );

        assertThatThrownBy(() -> toolRegistry.execute(
                "search_listings",
                tenantId,
                List.of("search_listings"),
                maliciousArgs
        ))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Tentativa de acesso cross-tenant bloqueada");
    }

    @Test
    @DisplayName("Tool search_listings executa consulta própria e retorna sucesso com fontes quando há registros")
    void shouldExecuteSearchListingsSuccessfully() {
        MarketplaceListing listing = new MarketplaceListing(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                null,
                null,
                "MERCADO_LIVRE",
                "MLB123456",
                "SKU-S24",
                null,
                "Smartphone Galaxy S24",
                new BigDecimal("4999.00"),
                "BRL",
                10,
                "ACTIVE",
                "https://mercadolivre.com.br/item",
                "{}",
                "v1.0",
                "OFFICIAL_API"
        );

        when(listingRepository.searchListings(eq(tenantId), any(), any(), any(), eq("Galaxy"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(listing)));

        ToolExecutionResult result = toolRegistry.execute(
                "search_listings",
                tenantId,
                List.of("search_listings"),
                Map.of("search", "Galaxy")
        );

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.sources()).contains("catalog:marketplace_listings");
        assertThat(result.data()).isNotNull();
    }

    @Test
    @DisplayName("Tool search_listings retorna UNAVAILABLE com nota explícita quando nenhum registro é localizado")
    void shouldReturnUnavailableWhenNoListingsFound() {
        when(listingRepository.searchListings(eq(tenantId), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ToolExecutionResult result = toolRegistry.execute(
                "search_listings",
                tenantId,
                List.of("search_listings"),
                Map.of("search", "Inexistente")
        );

        assertThat(result.status()).isEqualTo("UNAVAILABLE");
        assertThat(result.unavailabilityReason()).contains("Nenhum anúncio encontrado no catálogo");
        assertThat(result.sources()).contains("catalog:marketplace_listings");
    }

    @Test
    @DisplayName("Tool calculate_margin executa cálculo determinístico e emite disclaimer de Margem de Contribuição")
    void shouldExecuteCalculateMarginDeterministically() {
        PricingCalculationResult calcResult = new PricingCalculationResult(
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                new BigDecimal("16.00"),
                new BigDecimal("5.00"),
                new BigDecimal("71.00"),
                new BigDecimal("29.00"),
                new BigDecimal("29.00"),
                new BigDecimal("80.00"),
                true,
                new BigDecimal("85.00"),
                true,
                new BigDecimal("110.00"),
                true,
                "BRL",
                "1.0.0",
                Instant.now(),
                List.of(),
                List.of(),
                List.of()
        );

        when(pricingEngineService.calculateMargins(any(PricingCalculationRequest.class)))
                .thenReturn(calcResult);

        ToolExecutionResult result = toolRegistry.execute(
                "calculate_margin",
                tenantId,
                List.of("calculate_margin"),
                Map.of("sellingPrice", new BigDecimal("100.00"), "unitCost", new BigDecimal("50.00"))
        );

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.sources()).contains("pricing:PricingEngineService");
        Map<?, ?> data = (Map<?, ?>) result.data();
        assertThat(data.get("contributionMarginAmount")).isEqualTo(new BigDecimal("29.00"));
        assertThat(data.get("guardrailDisclaimer").toString()).contains("não constituindo Lucro Líquido");
    }

    @Test
    @DisplayName("Tool summarize_metrics retorna métricas oficiais e nota de ausência de CTR orgânico no ML")
    void shouldExecuteSummarizeMetricsWithOfficialDisclaimers() {
        MetricsSummaryDto summary = new MetricsSummaryDto(
                5,
                4,
                0,
                0,
                1,
                new BigDecimal("1500.00"),
                new BigDecimal("50.00"),
                new BigDecimal("45.00"),
                new BigDecimal("9500.00"),
                null,
                null,
                new BigDecimal("3.33"),
                null,
                "Avisos de anti-alucinação vigentes."
        );

        when(metricIngestionService.getSummary(eq(tenantId), any(), eq("MERCADO_LIVRE"), any(), any(), any()))
                .thenReturn(summary);

        ToolExecutionResult result = toolRegistry.execute(
                "summarize_metrics",
                tenantId,
                List.of("summarize_metrics"),
                Map.of("marketplace", "MERCADO_LIVRE", "windowDays", 30)
        );

        assertThat(result.status()).isEqualTo("SUCCESS");
        Map<?, ?> data = (Map<?, ?>) result.data();
        assertThat(data.get("totalVisits")).isEqualTo(new BigDecimal("1500.00"));
        assertThat(data.get("totalRevenue")).isEqualTo(new BigDecimal("9500.00"));
        assertThat(data.get("ctrChannelLimitation").toString()).contains("No tráfego orgânico do Mercado Livre");
    }
}
