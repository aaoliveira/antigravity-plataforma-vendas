package br.com.sellerintelligence.reconciliation.service;

import br.com.sellerintelligence.financial.service.SkuCostService;
import br.com.sellerintelligence.reconciliation.domain.*;
import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Order;
import br.com.sellerintelligence.sync.domain.OrderItem;
import br.com.sellerintelligence.sync.domain.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DreCalculationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReconciliationSettlementRepository settlementRepository;

    @Mock
    private ReconciliationSettlementItemRepository settlementItemRepository;

    @Mock
    private SkuCostService skuCostService;

    private DreCalculationService dreService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dreService = new DreCalculationService(orderRepository, settlementRepository, settlementItemRepository, skuCostService);
    }

    @Test
    @DisplayName("Should calculate DRE under ACCRUAL basis (Competência) with standard lines and formula breakdown")
    void shouldCalculateAccrualDre() {
        Instant now = Instant.now();
        Instant past = now.minus(30, ChronoUnit.DAYS);

        Order order = new Order(
                tenantId, UUID.randomUUID(), br.com.sellerintelligence.tenancy.store.Marketplace.MERCADO_LIVRE,
                "MLB-DRE-01", "PAID",
                new BigDecimal("1000.00"), "BRL", now.minus(5, ChronoUnit.DAYS),
                "comprador", "SHIP-01", new BigDecimal("50.00"),
                new BigDecimal("120.00"), DataProvenance.IMPORTED, "{}", now
        );
        OrderItem item = new OrderItem(tenantId, order, "EXT-1", "SKU-01", "Produto Teste", new BigDecimal("1000.00"), 1, BigDecimal.ZERO);
        order.addItem(item);

        when(orderRepository.findByTenantIdAndOrderDateBetween(any(), any(), any()))
                .thenReturn(List.of(order));

        DreCalculationService.ManagerialDreReportDto report = dreService.calculateDre(
                tenantId, DreAccountingBasis.ACCRUAL, past, now, "MERCADO_LIVRE"
        );

        assertThat(report.basis()).isEqualTo(DreAccountingBasis.ACCRUAL);
        assertThat(report.grossRevenue()).isEqualByComparingTo("1000.00");
        assertThat(report.disclaimer()).contains("DRE gerencial para apoio determinístico à tomada de decisão");
        assertThat(report.lines()).hasSize(10);

        // Verify formulas and subtotals
        assertThat(report.netRevenue()).isLessThan(report.grossRevenue());
        assertThat(report.contributionMargin()).isLessThan(report.netRevenue());
        assertThat(report.netOperatingResult()).isLessThan(report.contributionMargin());
    }

    @Test
    @DisplayName("Should calculate DRE under CASH basis (Caixa / Repasses) and export valid CSV with metadata headers")
    void shouldCalculateCashDreAndExportCsv() {
        Instant now = Instant.now();
        Instant past = now.minus(30, ChronoUnit.DAYS);

        ReconciliationSettlement settlement = new ReconciliationSettlement(
                tenantId, null, "MERCADO_LIVRE", "SETTL-CASH-01", "extrato.csv",
                OffsetDateTime.now(), new BigDecimal("2500.00"), new BigDecimal("2050.00"),
                new BigDecimal("350.00"), new BigDecimal("100.00"), BigDecimal.ZERO, "hash-cash"
        );

        when(settlementRepository.findByTenantIdAndSettlementDateBetween(any(), any(), any()))
                .thenReturn(List.of(settlement));

        DreCalculationService.ManagerialDreReportDto report = dreService.calculateDre(
                tenantId, DreAccountingBasis.CASH, past, now, "TODOS"
        );

        assertThat(report.basis()).isEqualTo(DreAccountingBasis.CASH);
        assertThat(report.grossRevenue()).isEqualByComparingTo("2500.00");

        String csv = dreService.exportDreToCsv(report);
        assertThat(csv).contains("# RELATÓRIO: DRE GERENCIAL");
        assertThat(csv).contains("# REGIME: CAIXA (REPASSES/LIQUIDAÇÕES)");
        assertThat(csv).contains("Linha;Código;Descrição;Fórmula / Memória de Cálculo;Valor (R$);% Receita Bruta");
        assertThat(csv).contains("Receita Bruta de Vendas");
    }
}
