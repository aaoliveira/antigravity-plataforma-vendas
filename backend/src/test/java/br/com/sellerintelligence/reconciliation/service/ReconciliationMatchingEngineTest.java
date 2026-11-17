package br.com.sellerintelligence.reconciliation.service;

import br.com.sellerintelligence.reconciliation.domain.*;
import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Order;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationMatchingEngineTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReconciliationSettlementItemRepository itemRepository;

    @Mock
    private ReconciliationSettlementRepository settlementRepository;

    private ReconciliationMatchingEngine matchingEngine;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        matchingEngine = new ReconciliationMatchingEngine(orderRepository, itemRepository, settlementRepository);
    }

    @Test
    @DisplayName("Should reconcile item as CONCILED when settled amount exactly matches expected order payout")
    void shouldReconcileItemExactly() {
        ReconciliationSettlement settlement = new ReconciliationSettlement(
                tenantId, storeId, "MERCADO_LIVRE", "SETTL-100", "extrato.csv",
                OffsetDateTime.now(), new BigDecimal("200.00"), new BigDecimal("170.00"),
                new BigDecimal("20.00"), new BigDecimal("10.00"), BigDecimal.ZERO, "hash-1"
        );

        // Order with total 200.00, fee 20.00, shipping 10.00 -> expected payout = 170.00
        Order order = new Order(
                tenantId, storeId, br.com.sellerintelligence.tenancy.store.Marketplace.MERCADO_LIVRE,
                "MLB8881", "PAID", new BigDecimal("200.00"),
                "BRL", Instant.now(), "comprador1", "SHIP-1", new BigDecimal("10.00"),
                new BigDecimal("20.00"), DataProvenance.IMPORTED, "{}", Instant.now()
        );

        ReconciliationSettlementItem item = new ReconciliationSettlementItem(
                settlement.getId(), tenantId, "MLB8881", "TX-1",
                ReconciliationEntryType.ORDER_PAYOUT, new BigDecimal("170.00")
        );

        when(orderRepository.findByTenantIdAndExternalOrderId(tenantId, "MLB8881")).thenReturn(Optional.of(order));

        matchingEngine.reconcileBatch(settlement, new ArrayList<>(List.of(item)));

        assertThat(item.getReconciliationStatus()).isEqualTo(ReconciliationItemStatus.CONCILED);
        assertThat(item.getMatchingMethod()).isEqualTo(MatchingMethod.ORDER_ID_EXACT);
        assertThat(item.getConfidenceScore()).isEqualByComparingTo("1.0000");
        assertThat(item.getDiscrepancyAmount()).isEqualByComparingTo("0.00");
        assertThat(item.getOrderId()).isEqualTo(order.getId());
        assertThat(settlement.getReconciledItemsCount()).isEqualTo(1);
        assertThat(settlement.getDiscrepantItemsCount()).isEqualTo(0);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.RECONCILED);
    }

    @Test
    @DisplayName("Should detect DISCREPANT status and compute exact difference when marketplace settles less than expected")
    void shouldDetectDiscrepancy() {
        ReconciliationSettlement settlement = new ReconciliationSettlement(
                tenantId, storeId, "MERCADO_LIVRE", "SETTL-101", "extrato.csv",
                OffsetDateTime.now(), new BigDecimal("200.00"), new BigDecimal("150.00"),
                new BigDecimal("20.00"), new BigDecimal("10.00"), BigDecimal.ZERO, "hash-2"
        );

        // Expected payout = 170.00, but marketplace settled 150.00 (discrepancy = -20.00)
        Order order = new Order(
                tenantId, storeId, br.com.sellerintelligence.tenancy.store.Marketplace.MERCADO_LIVRE,
                "MLB8882", "PAID", new BigDecimal("200.00"),
                "BRL", Instant.now(), "comprador2", "SHIP-2", new BigDecimal("10.00"),
                new BigDecimal("20.00"), DataProvenance.IMPORTED, "{}", Instant.now()
        );

        ReconciliationSettlementItem item = new ReconciliationSettlementItem(
                settlement.getId(), tenantId, "MLB8882", "TX-2",
                ReconciliationEntryType.ORDER_PAYOUT, new BigDecimal("150.00")
        );

        when(orderRepository.findByTenantIdAndExternalOrderId(tenantId, "MLB8882")).thenReturn(Optional.of(order));

        matchingEngine.reconcileBatch(settlement, new ArrayList<>(List.of(item)));

        assertThat(item.getReconciliationStatus()).isEqualTo(ReconciliationItemStatus.DISCREPANT);
        assertThat(item.getDiscrepancyAmount()).isEqualByComparingTo("-20.00");
        assertThat(item.getExpectedAmount()).isEqualByComparingTo("170.00");
        assertThat(settlement.getDiscrepantItemsCount()).isEqualTo(1);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.HAS_DISCREPANCIES);
    }

    @Test
    @DisplayName("Should mark UNIDENTIFIED when payout references an unknown order")
    void shouldMarkUnidentifiedWhenOrderNotFound() {
        ReconciliationSettlement settlement = new ReconciliationSettlement(
                tenantId, storeId, "MERCADO_LIVRE", "SETTL-102", "extrato.csv",
                OffsetDateTime.now(), new BigDecimal("100.00"), new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "hash-3"
        );

        ReconciliationSettlementItem item = new ReconciliationSettlementItem(
                settlement.getId(), tenantId, "MLB-UNKNOWN-999", "TX-3",
                ReconciliationEntryType.ORDER_PAYOUT, new BigDecimal("100.00")
        );

        when(orderRepository.findByTenantIdAndExternalOrderId(tenantId, "MLB-UNKNOWN-999")).thenReturn(Optional.empty());

        matchingEngine.reconcileBatch(settlement, new ArrayList<>(List.of(item)));

        assertThat(item.getReconciliationStatus()).isEqualTo(ReconciliationItemStatus.UNIDENTIFIED);
        assertThat(item.getMatchingMethod()).isEqualTo(MatchingMethod.UNMATCHED);
        assertThat(item.getConfidenceScore()).isEqualByComparingTo("0.0000");
    }
}
