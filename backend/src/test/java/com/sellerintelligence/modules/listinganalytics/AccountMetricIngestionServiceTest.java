package com.sellerintelligence.modules.listinganalytics;

import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.listinganalytics.entity.AccountMetricRecord;
import com.sellerintelligence.modules.listinganalytics.model.AccountMetricDto;
import com.sellerintelligence.modules.listinganalytics.model.IngestMetricsCommand;
import com.sellerintelligence.modules.listinganalytics.model.MetricAvailabilityStatus;
import com.sellerintelligence.modules.listinganalytics.model.MetricFormulaEngine;
import com.sellerintelligence.modules.listinganalytics.model.MetricGranularity;
import com.sellerintelligence.modules.listinganalytics.model.MetricProvenance;
import com.sellerintelligence.modules.listinganalytics.model.MetricScope;
import com.sellerintelligence.modules.listinganalytics.model.MetricsSummaryDto;
import com.sellerintelligence.modules.listinganalytics.model.OfficialMetricCatalog;
import com.sellerintelligence.modules.listinganalytics.repository.AccountMetricRepository;
import com.sellerintelligence.modules.listinganalytics.service.AccountMetricIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountMetricIngestionServiceTest {

    @Mock
    private AccountMetricRepository metricRepository;

    @Mock
    private SellerAccountRepository sellerAccountRepository;

    @Mock
    private MarketplaceListingRepository marketplaceListingRepository;

    private OfficialMetricCatalog metricCatalog;
    private MetricFormulaEngine formulaEngine;
    private AccountMetricIngestionService ingestionService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
    private final Instant end = start.plus(1, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        metricCatalog = new OfficialMetricCatalog();
        formulaEngine = new MetricFormulaEngine();
        ingestionService = new AccountMetricIngestionService(
                metricRepository,
                sellerAccountRepository,
                marketplaceListingRepository,
                metricCatalog,
                formulaEngine
        );
    }

    @Test
    @DisplayName("Ingestão Mercado Livre: Deve salvar visitas e marcar CTR como UNSUPPORTED_BY_CHANNEL sem forjar estimativas silenciosas")
    void shouldIngestMercadoLivreMetricsAndMarkCtrAsUnsupported() {
        SellerAccount account = new SellerAccount(accountId, tenantId, "MERCADO_LIVRE", "MLB_123456", "Loja Oficial ML", "ACTIVE");
        when(sellerAccountRepository.findByIdAndTenantId(accountId, tenantId)).thenReturn(Optional.of(account));
        when(metricRepository.save(any(AccountMetricRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, BigDecimal> metrics = new HashMap<>();
        metrics.put("VISITS", new BigDecimal("2000"));
        metrics.put("ORDERS_COUNT", new BigDecimal("50"));
        metrics.put("UNITS_SOLD", new BigDecimal("75"));
        metrics.put("REVENUE", new BigDecimal("7500.00"));

        IngestMetricsCommand command = new IngestMetricsCommand(
                accountId,
                "MERCADO_LIVRE",
                "MLB999888",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                start,
                end,
                metrics,
                "/items/MLB999888/visits/time_window"
        );

        List<AccountMetricDto> result = ingestionService.ingestMetrics(tenantId, command);

        assertThat(result).isNotEmpty();

        // 1. VISITS
        AccountMetricDto visitsDto = result.stream().filter(d -> d.metricKey().equals("VISITS")).findFirst().orElseThrow();
        assertThat(visitsDto.availabilityStatus()).isEqualTo(MetricAvailabilityStatus.AVAILABLE);
        assertThat(visitsDto.metricValue()).isEqualByComparingTo("2000");
        assertThat(visitsDto.provenance()).isEqualTo(MetricProvenance.OFFICIAL_API);

        // 2. CTR no ML orgânico -> UNSUPPORTED_BY_CHANNEL com valor nulo
        AccountMetricDto ctrDto = result.stream().filter(d -> d.metricKey().equals("CTR")).findFirst().orElseThrow();
        assertThat(ctrDto.availabilityStatus()).isEqualTo(MetricAvailabilityStatus.UNSUPPORTED_BY_CHANNEL);
        assertThat(ctrDto.metricValue()).isNull();
        assertThat(ctrDto.unavailabilityReason()).contains("Mercado Ads");

        // 3. Taxa de Conversão de Pedidos -> 50 / 2000 = 2.5000%
        AccountMetricDto convOrdersDto = result.stream().filter(d -> d.metricKey().equals("CONVERSION_RATE_ORDERS")).findFirst().orElseThrow();
        assertThat(convOrdersDto.availabilityStatus()).isEqualTo(MetricAvailabilityStatus.AVAILABLE);
        assertThat(convOrdersDto.metricValue()).isEqualByComparingTo("2.5000");
        assertThat(convOrdersDto.denominatorMetric()).isEqualTo("VISITS");
        assertThat(convOrdersDto.denominatorValue()).isEqualByComparingTo("2000");

        // 4. Taxa de Conversão de Unidades -> 75 / 2000 = 3.7500%
        AccountMetricDto convUnitsDto = result.stream().filter(d -> d.metricKey().equals("CONVERSION_RATE_UNITS")).findFirst().orElseThrow();
        assertThat(convUnitsDto.availabilityStatus()).isEqualTo(MetricAvailabilityStatus.AVAILABLE);
        assertThat(convUnitsDto.metricValue()).isEqualByComparingTo("3.7500");
    }

    @Test
    @DisplayName("Ingestão Shopee com Ads: Deve calcular CTR derivado a partir de cliques e impressões reais")
    void shouldIngestShopeeMetricsWithAdsAndDeriveCtr() {
        SellerAccount account = new SellerAccount(accountId, tenantId, "SHOPEE", "SHOP_9988", "Loja Shopee", "ACTIVE");
        when(sellerAccountRepository.findByIdAndTenantId(accountId, tenantId)).thenReturn(Optional.of(account));
        when(metricRepository.save(any(AccountMetricRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, BigDecimal> metrics = new HashMap<>();
        metrics.put("UNIQUE_VISITORS", new BigDecimal("1000"));
        metrics.put("ORDERS_COUNT", new BigDecimal("40"));
        metrics.put("CLICKS", new BigDecimal("200"));
        metrics.put("IMPRESSIONS", new BigDecimal("4000"));

        IngestMetricsCommand command = new IngestMetricsCommand(
                accountId,
                "SHOPEE",
                "SHOPEE_ITEM_77",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                start,
                end,
                metrics,
                "v2.analytics"
        );

        List<AccountMetricDto> result = ingestionService.ingestMetrics(tenantId, command);

        // CTR = 200 / 4000 = 5.0000%
        AccountMetricDto ctrDto = result.stream().filter(d -> d.metricKey().equals("CTR")).findFirst().orElseThrow();
        assertThat(ctrDto.availabilityStatus()).isEqualTo(MetricAvailabilityStatus.AVAILABLE);
        assertThat(ctrDto.metricValue()).isEqualByComparingTo("5.0000");
        assertThat(ctrDto.provenance()).isEqualTo(MetricProvenance.DERIVED_FORMULA);
        assertThat(ctrDto.denominatorMetric()).isEqualTo("IMPRESSIONS");
        assertThat(ctrDto.denominatorValue()).isEqualByComparingTo("4000");

        // Conversão Pedidos = 40 / 1000 = 4.0000%
        AccountMetricDto convDto = result.stream().filter(d -> d.metricKey().equals("CONVERSION_RATE_ORDERS")).findFirst().orElseThrow();
        assertThat(convDto.availabilityStatus()).isEqualTo(MetricAvailabilityStatus.AVAILABLE);
        assertThat(convDto.metricValue()).isEqualByComparingTo("4.0000");
    }

    @Test
    @DisplayName("Ingestão: Denominador de visitas igual a zero resulta em INSUFFICIENT_DATA sem divisão por zero")
    void shouldHandleZeroVisitsDeterministically() {
        SellerAccount account = new SellerAccount(accountId, tenantId, "MERCADO_LIVRE", "MLB_123", "Loja ML", "ACTIVE");
        when(sellerAccountRepository.findByIdAndTenantId(accountId, tenantId)).thenReturn(Optional.of(account));
        when(metricRepository.save(any(AccountMetricRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, BigDecimal> metrics = new HashMap<>();
        metrics.put("VISITS", BigDecimal.ZERO);
        metrics.put("ORDERS_COUNT", BigDecimal.ZERO);

        IngestMetricsCommand command = new IngestMetricsCommand(
                accountId,
                "MERCADO_LIVRE",
                "MLB_NEW_ITEM",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                start,
                end,
                metrics,
                "/visits"
        );

        List<AccountMetricDto> result = ingestionService.ingestMetrics(tenantId, command);

        AccountMetricDto convDto = result.stream().filter(d -> d.metricKey().equals("CONVERSION_RATE_ORDERS")).findFirst().orElseThrow();
        assertThat(convDto.availabilityStatus()).isEqualTo(MetricAvailabilityStatus.INSUFFICIENT_DATA);
        assertThat(convDto.metricValue()).isEqualByComparingTo("0.0000");
        assertThat(convDto.unavailabilityReason()).contains("Denominador de visitas igual a zero");
    }

    @Test
    @DisplayName("Idempotência: Reingestão da mesma janela atualiza registro existente sem gerar duplicatas")
    void shouldUpdateExistingRecordOnReingestion() {
        SellerAccount account = new SellerAccount(accountId, tenantId, "MERCADO_LIVRE", "MLB_123", "Loja ML", "ACTIVE");
        when(sellerAccountRepository.findByIdAndTenantId(accountId, tenantId)).thenReturn(Optional.of(account));

        AccountMetricRecord existingRecord = new AccountMetricRecord(
                UUID.randomUUID(),
                tenantId,
                accountId,
                "MERCADO_LIVRE",
                null,
                "MLB_123",
                "MLB_123",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                start,
                end,
                "VISITS",
                new BigDecimal("1000"),
                com.sellerintelligence.modules.listinganalytics.model.MetricUnit.COUNT,
                MetricProvenance.OFFICIAL_API,
                null,
                null,
                null,
                null,
                null,
                MetricAvailabilityStatus.AVAILABLE,
                null,
                "/visits",
                Instant.now(),
                null,
                Instant.now(),
                Instant.now()
        );

        when(metricRepository.findByTenantIdAndSellerAccountIdAndMarketplaceAndTargetIdentifierAndMetricKeyAndGranularityAndWindowStartAndWindowEnd(
                eq(tenantId), eq(accountId), eq("MERCADO_LIVRE"), eq("MLB_123"), eq("VISITS"), eq(MetricGranularity.DAILY), eq(start), eq(end)
        )).thenReturn(Optional.of(existingRecord));

        when(metricRepository.save(any(AccountMetricRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, BigDecimal> metrics = Map.of("VISITS", new BigDecimal("1500"));
        IngestMetricsCommand command = new IngestMetricsCommand(
                accountId,
                "MERCADO_LIVRE",
                "MLB_123",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                start,
                end,
                metrics,
                "/visits"
        );

        List<AccountMetricDto> result = ingestionService.ingestMetrics(tenantId, command);

        AccountMetricDto visitsDto = result.stream().filter(d -> d.metricKey().equals("VISITS")).findFirst().orElseThrow();
        assertThat(visitsDto.metricValue()).isEqualByComparingTo("1500");
        assertThat(existingRecord.getMetricValue()).isEqualByComparingTo("1500");
    }

    @Test
    @DisplayName("Segurança Multi-Tenant: Rejeita conta inexistente ou de outro tenant")
    void shouldRejectUnknownOrCrossTenantAccount() {
        when(sellerAccountRepository.findByIdAndTenantId(accountId, tenantId)).thenReturn(Optional.empty());

        IngestMetricsCommand command = new IngestMetricsCommand(
                accountId,
                "MERCADO_LIVRE",
                null,
                MetricScope.ACCOUNT,
                MetricGranularity.DAILY,
                start,
                end,
                Map.of(),
                "/orders"
        );

        assertThatThrownBy(() -> ingestionService.ingestMetrics(tenantId, command))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conta de vendedor não encontrada");
    }

    @Test
    @DisplayName("Resumo Consolidado: Deve agregar totais e taxas médias com aviso anti-alucinação")
    void shouldComputeMetricsSummaryAccurately() {
        AccountMetricRecord m1 = new AccountMetricRecord(
                UUID.randomUUID(), tenantId, accountId, "MERCADO_LIVRE", null, null, "__ACCOUNT__",
                MetricScope.ACCOUNT, MetricGranularity.DAILY, start, end, "VISITS", new BigDecimal("2000"),
                com.sellerintelligence.modules.listinganalytics.model.MetricUnit.COUNT, MetricProvenance.OFFICIAL_API,
                null, null, null, null, null, MetricAvailabilityStatus.AVAILABLE, null, "/visits",
                Instant.now(), null, Instant.now(), Instant.now()
        );
        AccountMetricRecord m2 = new AccountMetricRecord(
                UUID.randomUUID(), tenantId, accountId, "MERCADO_LIVRE", null, null, "__ACCOUNT__",
                MetricScope.ACCOUNT, MetricGranularity.DAILY, start, end, "ORDERS_COUNT", new BigDecimal("60"),
                com.sellerintelligence.modules.listinganalytics.model.MetricUnit.COUNT, MetricProvenance.OFFICIAL_API,
                null, null, null, null, null, MetricAvailabilityStatus.AVAILABLE, null, "/orders",
                Instant.now(), null, Instant.now(), Instant.now()
        );
        AccountMetricRecord m3 = new AccountMetricRecord(
                UUID.randomUUID(), tenantId, accountId, "MERCADO_LIVRE", null, null, "__ACCOUNT__",
                MetricScope.ACCOUNT, MetricGranularity.DAILY, start, end, "CTR", null,
                com.sellerintelligence.modules.listinganalytics.model.MetricUnit.PERCENTAGE, MetricProvenance.UNAVAILABLE,
                null, null, null, null, null, MetricAvailabilityStatus.UNSUPPORTED_BY_CHANNEL, "Mercado Ads", "/ctr",
                Instant.now(), null, Instant.now(), Instant.now()
        );

        when(metricRepository.findForSummary(eq(tenantId), eq(accountId), eq("MERCADO_LIVRE"), any(), any()))
                .thenReturn(List.of(m1, m2, m3));

        MetricsSummaryDto summary = ingestionService.getSummary(tenantId, accountId, "MERCADO_LIVRE", start, end);

        assertThat(summary.totalMetricsRecorded()).isEqualTo(3);
        assertThat(summary.availableMetricsCount()).isEqualTo(2);
        assertThat(summary.unsupportedByChannelCount()).isEqualTo(1);
        assertThat(summary.totalVisits()).isEqualByComparingTo("2000");
        assertThat(summary.totalOrders()).isEqualByComparingTo("60");
        // 60 / 2000 = 3.0000%
        assertThat(summary.overallConversionRateOrders()).isEqualByComparingTo("3.0000");
        assertThat(summary.antiHallucinationNotice()).contains("Estimativas silenciosas, scrapers não licenciados e números de concorrentes não autorizados são expressamente proibidos");
    }
}
