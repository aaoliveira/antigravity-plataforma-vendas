package com.sellerintelligence.modules.financialanalytics.aggregation;

import com.sellerintelligence.modules.company.entity.CompanyRecord;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
import com.sellerintelligence.modules.financialanalytics.entity.OrderFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.model.FinancialValueProvenance;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AggregatedKpiService - Testes de Agregação, Fórmulas Determinísticas e Cache")
class AggregatedKpiServiceTest {

    @Mock
    private OrderFinancialFactRepository factRepository;

    @Mock
    private CompanyRepository companyRepository;

    private TenantAwareKpiCacheService cacheService;
    private AggregatedKpiService kpiService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId1 = UUID.randomUUID();
    private final UUID companyId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cacheService = new TenantAwareKpiCacheService();
        kpiService = new AggregatedKpiService(factRepository, companyRepository, cacheService);
    }

    @Test
    @DisplayName("Critério de Aceite: Agregações conferem rigorosamente com as fontes individuais")
    void shouldComputeAggregatedSummaryAccuratelyMatchingSources() {
        // Pedido 1: Mercado Livre (R$ 100 bruto, R$ 10 desconto, R$ 15 comissão, R$ 5 fixo, R$ 10 frete, R$ 30 CMV, R$ 5 imposto)
        // Deduções = 15 + 5 + 10 + 30 + 5 = R$ 65
        // Margem = 90 - 65 = R$ 25
        OrderFinancialFactRecord fact1 = createFact(
                tenantId, companyId1, "MERCADO_LIVRE", "MLB-101",
                Instant.parse("2026-11-20T10:00:00Z"),
                "100.0000", "10.0000", "90.0000", "0.0000", "0.0000",
                "15.0000", "5.0000", "10.0000", "30.0000", "0.0000", "0.0000",
                "5.0000", "0.0000", "65.0000", "25.0000",
                ReconciliationStatus.RECONCILED, false
        );

        // Pedido 2: Shopee (R$ 200 bruto, R$ 20 desconto, R$ 28 comissão, R$ 6 fixo, R$ 15 frete, R$ 60 CMV, R$ 11 imposto)
        // Deduções = 28 + 6 + 15 + 60 + 11 = R$ 120
        // Margem = 180 - 120 = R$ 60
        OrderFinancialFactRecord fact2 = createFact(
                tenantId, companyId2, "SHOPEE", "SHO-202",
                Instant.parse("2026-11-21T14:30:00Z"),
                "200.0000", "20.0000", "180.0000", "0.0000", "0.0000",
                "28.0000", "6.0000", "15.0000", "60.0000", "0.0000", "0.0000",
                "11.0000", "0.0000", "120.0000", "60.0000",
                ReconciliationStatus.ESTIMATED, false
        );

        when(factRepository.findForAggregation(eq(tenantId), any(), any(), any(), any(), any()))
                .thenReturn(List.of(fact1, fact2));

        AggregatedKpiFilter filter = new AggregatedKpiFilter(null, null, null, null, null);
        AggregatedKpiSummaryDto summary = kpiService.getAggregatedSummary(tenantId, filter);

        // Validações da conferência matemática exata (soma centavo a centavo)
        assertThat(summary.totalOrders()).isEqualTo(2);
        assertThat(summary.totalGrossRevenue()).isEqualByComparingTo("300.0000");
        assertThat(summary.totalDiscountAmount()).isEqualByComparingTo("30.0000");
        assertThat(summary.totalNetRevenue()).isEqualByComparingTo("270.0000");
        assertThat(summary.totalMarketplaceFees()).isEqualByComparingTo("43.0000");
        assertThat(summary.totalFixedFees()).isEqualByComparingTo("11.0000");
        assertThat(summary.totalShippingCost()).isEqualByComparingTo("25.0000");
        assertThat(summary.totalProductCost()).isEqualByComparingTo("90.0000");
        assertThat(summary.totalTaxAmount()).isEqualByComparingTo("16.0000");
        assertThat(summary.totalDeductions()).isEqualByComparingTo("185.0000");
        assertThat(summary.totalContributionMarginAmount()).isEqualByComparingTo("85.0000"); // 25 + 60 = 85

        // Margem de Contribuição % = (85 / 270) * 100 = 31.4815%
        BigDecimal expectedMarginRatio = new BigDecimal("85.0000")
                .multiply(BigDecimal.valueOf(100))
                .divide(new BigDecimal("270.0000"), 4, RoundingMode.HALF_EVEN);
        assertThat(summary.averageContributionMarginRatio()).isEqualByComparingTo(expectedMarginRatio);

        // Ticket Médio = 300 / 2 = R$ 150.0000
        assertThat(summary.averageOrderValue()).isEqualByComparingTo("150.0000");

        // Taxa de Conciliação: 1 de 2 = 50.0000%
        assertThat(summary.reconciliationRatePercent()).isEqualByComparingTo("50.0000");
        assertThat(summary.reconciledCount()).isEqualTo(1);
        assertThat(summary.estimatedCount()).isEqualTo(1);

        // Metadados de segurança e conformidade
        assertThat(summary.cached()).isFalse();
        assertThat(summary.antiHallucinationDisclaimer()).contains("proibido rotular este valor como Lucro Líquido");
        assertThat(summary.kpiDefinitions()).hasSize(18);
    }

    @Test
    @DisplayName("Deve retornar dados do cache na segunda chamada e evitar ida ao banco")
    void shouldReturnCachedSummaryOnSubsequentCalls() {
        OrderFinancialFactRecord fact1 = createFact(
                tenantId, companyId1, "MERCADO_LIVRE", "MLB-1",
                Instant.now(), "100.0000", "0.0000", "100.0000", "0.0000", "0.0000",
                "15.0000", "0.0000", "0.0000", "50.0000", "0.0000", "0.0000",
                "0.0000", "0.0000", "65.0000", "35.0000",
                ReconciliationStatus.ESTIMATED, false
        );
        when(factRepository.findForAggregation(eq(tenantId), any(), any(), any(), any(), any()))
                .thenReturn(List.of(fact1));

        AggregatedKpiFilter filter = new AggregatedKpiFilter(null, null, null, null, null);

        // 1ª chamada: vai ao banco
        AggregatedKpiSummaryDto first = kpiService.getAggregatedSummary(tenantId, filter);
        assertThat(first.cached()).isFalse();
        verify(factRepository, times(1)).findForAggregation(any(), any(), any(), any(), any(), any());

        // 2ª chamada: retorna do cache
        AggregatedKpiSummaryDto second = kpiService.getAggregatedSummary(tenantId, filter);
        assertThat(second.cached()).isTrue();
        assertThat(second.totalGrossRevenue()).isEqualByComparingTo("100.0000");
        verify(factRepository, times(1)).findForAggregation(any(), any(), any(), any(), any(), any()); // sem novas chamadas
    }

    @Test
    @DisplayName("Critério de Aceite: Invalidação de cache por evento relevante")
    void shouldInvalidateCacheOnMutationEvent() {
        OrderFinancialFactRecord fact = createFact(
                tenantId, companyId1, "MERCADO_LIVRE", "MLB-1",
                Instant.now(), "100.0000", "0.0000", "100.0000", "0.0000", "0.0000",
                "15.0000", "0.0000", "0.0000", "50.0000", "0.0000", "0.0000",
                "0.0000", "0.0000", "65.0000", "35.0000",
                ReconciliationStatus.ESTIMATED, false
        );
        when(factRepository.findForAggregation(eq(tenantId), any(), any(), any(), any(), any()))
                .thenReturn(List.of(fact));

        AggregatedKpiFilter filter = new AggregatedKpiFilter(null, null, null, null, null);
        kpiService.getAggregatedSummary(tenantId, filter);

        assertThat(cacheService.size()).isGreaterThan(0);

        // Dispara evento de mutação através do listener
        KpiCacheInvalidationListener listener = new KpiCacheInvalidationListener(cacheService);
        listener.onFinancialFactMutated(new FinancialFactMutatedEvent(tenantId, fact.getOrderId(), "FACT_COMPUTED"));

        // Cache do tenant deve ter sido esvaziado
        assertThat(cacheService.size()).isEqualTo(0);

        // Próxima chamada volta a buscar no repositório
        kpiService.getAggregatedSummary(tenantId, filter);
        verify(factRepository, times(2)).findForAggregation(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve agregar KPIs por Canal com cálculo de share de receita e margem")
    void shouldAggregateKpisByChannelAccurately() {
        OrderFinancialFactRecord meliFact = createFact(
                tenantId, companyId1, "MERCADO_LIVRE", "MLB-1",
                Instant.now(), "300.0000", "0.0000", "300.0000", "0.0000", "0.0000",
                "45.0000", "0.0000", "0.0000", "100.0000", "0.0000", "0.0000",
                "0.0000", "0.0000", "145.0000", "155.0000",
                ReconciliationStatus.ESTIMATED, false
        );

        OrderFinancialFactRecord shopeeFact = createFact(
                tenantId, companyId1, "SHOPEE", "SHO-1",
                Instant.now(), "100.0000", "0.0000", "100.0000", "0.0000", "0.0000",
                "14.0000", "0.0000", "0.0000", "40.0000", "0.0000", "0.0000",
                "0.0000", "0.0000", "54.0000", "46.0000",
                ReconciliationStatus.ESTIMATED, false
        );

        when(factRepository.findForAggregation(eq(tenantId), any(), any(), any(), any(), any()))
                .thenReturn(List.of(meliFact, shopeeFact));

        List<ChannelKpiAggregateDto> channels = kpiService.getKpisByChannel(tenantId, null);

        assertThat(channels).hasSize(2);

        // Mercado Livre tem R$ 300 de R$ 400 total líquido = 75.0000%
        ChannelKpiAggregateDto meli = channels.get(0);
        assertThat(meli.marketplace()).isEqualTo("MERCADO_LIVRE");
        assertThat(meli.grossRevenue()).isEqualByComparingTo("300.0000");
        assertThat(meli.contributionMarginAmount()).isEqualByComparingTo("155.0000");
        assertThat(meli.revenueSharePercent()).isEqualByComparingTo("75.0000");

        // Shopee tem R$ 100 de R$ 400 total líquido = 25.0000%
        ChannelKpiAggregateDto shopee = channels.get(1);
        assertThat(shopee.marketplace()).isEqualTo("SHOPEE");
        assertThat(shopee.grossRevenue()).isEqualByComparingTo("100.0000");
        assertThat(shopee.revenueSharePercent()).isEqualByComparingTo("25.0000");
    }

    @Test
    @DisplayName("Deve agregar KPIs por Período temporal (DAILY)")
    void shouldAggregateKpisByPeriodDaily() {
        OrderFinancialFactRecord day1 = createFact(
                tenantId, companyId1, "MERCADO_LIVRE", "MLB-D1",
                Instant.parse("2026-11-20T10:00:00Z"), "100.0000", "0.0000", "100.0000", "0.0000", "0.0000",
                "15.0000", "0.0000", "0.0000", "50.0000", "0.0000", "0.0000",
                "0.0000", "0.0000", "65.0000", "35.0000",
                ReconciliationStatus.ESTIMATED, false
        );

        OrderFinancialFactRecord day2 = createFact(
                tenantId, companyId1, "MERCADO_LIVRE", "MLB-D2",
                Instant.parse("2026-11-21T15:00:00Z"), "200.0000", "0.0000", "200.0000", "0.0000", "0.0000",
                "30.0000", "0.0000", "0.0000", "100.0000", "0.0000", "0.0000",
                "0.0000", "0.0000", "130.0000", "70.0000",
                ReconciliationStatus.ESTIMATED, false
        );

        when(factRepository.findForAggregation(eq(tenantId), any(), any(), any(), any(), any()))
                .thenReturn(List.of(day1, day2));

        List<PeriodicKpiAggregateDto> periods = kpiService.getKpisByPeriod(tenantId, null, KpiGranularity.DAILY);

        assertThat(periods).hasSize(2);
        assertThat(periods.get(0).periodKey()).isEqualTo("2026-11-20");
        assertThat(periods.get(0).grossRevenue()).isEqualByComparingTo("100.0000");
        assertThat(periods.get(1).periodKey()).isEqualTo("2026-11-21");
        assertThat(periods.get(1).grossRevenue()).isEqualByComparingTo("200.0000");
    }

    @Test
    @DisplayName("Deve agregar KPIs por Empresa vinculando razão social")
    void shouldAggregateKpisByCompany() {
        OrderFinancialFactRecord factComp1 = createFact(
                tenantId, companyId1, "MERCADO_LIVRE", "MLB-C1",
                Instant.now(), "500.0000", "0.0000", "500.0000", "0.0000", "0.0000",
                "75.0000", "0.0000", "0.0000", "200.0000", "0.0000", "0.0000",
                "0.0000", "0.0000", "275.0000", "225.0000",
                ReconciliationStatus.ESTIMATED, false
        );

        when(factRepository.findForAggregation(eq(tenantId), any(), any(), any(), any(), any()))
                .thenReturn(List.of(factComp1));

        CompanyRecord compRecord = mock(CompanyRecord.class);
        when(compRecord.getCorporateName()).thenReturn("Alpha E-Commerce LTDA");
        when(companyRepository.findByIdAndTenantId(companyId1, tenantId)).thenReturn(Optional.of(compRecord));

        List<CompanyKpiAggregateDto> companies = kpiService.getKpisByCompany(tenantId, null);

        assertThat(companies).hasSize(1);
        assertThat(companies.get(0).corporateName()).isEqualTo("Alpha E-Commerce LTDA");
        assertThat(companies.get(0).grossRevenue()).isEqualByComparingTo("500.0000");
    }

    @Test
    @DisplayName("Critério de Aceite: Contrato documentado - Deve disponibilizar catálogo de definições oficiais")
    void shouldProvideDocumentedKpiDefinitionsCatalog() {
        List<KpiDefinitionDto> definitions = kpiService.getKpiDefinitions();
        assertThat(definitions).isNotEmpty();
        assertThat(definitions).extracting(KpiDefinitionDto::code)
                .contains("GROSS_REVENUE", "NET_REVENUE", "CONTRIBUTION_MARGIN", "TOTAL_DEDUCTIONS", "AVERAGE_ORDER_VALUE");
    }

    private OrderFinancialFactRecord createFact(
            UUID tenantId, UUID companyId, String marketplace, String externalOrderId,
            Instant orderDate, String gross, String disc, String net, String cancel, String ret,
            String fee, String fixedFee, String ship, String prodCost, String packCost, String addCost,
            String tax, String otherExp, String totalDed, String margin,
            ReconciliationStatus status, boolean missing
    ) {
        return new OrderFinancialFactRecord(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(), companyId,
                marketplace, externalOrderId, "PAID", orderDate,
                new BigDecimal(gross), new BigDecimal(disc), new BigDecimal(net),
                new BigDecimal(cancel), new BigDecimal(ret), new BigDecimal(fee),
                new BigDecimal(fixedFee), new BigDecimal(ship), new BigDecimal(prodCost),
                new BigDecimal(packCost), new BigDecimal(addCost), new BigDecimal(tax),
                new BigDecimal(otherExp), new BigDecimal(totalDed), new BigDecimal(margin),
                new BigDecimal("25.0000"), "BRL", status,
                FinancialValueProvenance.ESTIMATED, FinancialValueProvenance.ESTIMATED,
                FinancialValueProvenance.CONFIRMED, FinancialValueProvenance.USER_INFORMED,
                missing, "[]", "v1.0.0", "{}"
        );
    }
}
