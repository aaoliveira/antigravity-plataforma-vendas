package br.com.sellerintelligence.reconciliation.service;

import br.com.sellerintelligence.reconciliation.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualReconciliationServiceTest {

    @Mock
    private ReconciliationSettlementItemRepository itemRepository;

    @Mock
    private ReconciliationDisputeRepository disputeRepository;

    @Mock
    private ReconciliationAuditLogRepository auditLogRepository;

    private ManualReconciliationService manualService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        manualService = new ManualReconciliationService(itemRepository, disputeRepository, auditLogRepository);
    }

    @Test
    @DisplayName("Should manually associate order to an item, set CONCILED and create audit log")
    void shouldManuallyAssociateOrderWithAuditLog() {
        UUID itemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        ReconciliationSettlementItem item = new ReconciliationSettlementItem(
                UUID.randomUUID(), tenantId, "UNKNOWN-ORD", "TX-1",
                ReconciliationEntryType.ORDER_PAYOUT, new BigDecimal("120.00")
        );
        item.markUnidentified("Pedido não localizado.");

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReconciliationSettlementItem result = manualService.manuallyAssociateOrder(
                tenantId, itemId, orderId, "Comprovante de pagamento validado manualmente com suporte do ML", userId
        );

        assertThat(result.getReconciliationStatus()).isEqualTo(ReconciliationItemStatus.CONCILED);
        assertThat(result.getMatchingMethod()).isEqualTo(MatchingMethod.MANUAL_ASSOCIATION);
        assertThat(result.getOrderId()).isEqualTo(orderId);

        verify(auditLogRepository).save(argThat(log ->
                log.getAction() == ReconciliationAuditAction.MANUAL_MATCH &&
                log.getJustification().contains("Comprovante de pagamento")
        ));
    }

    @Test
    @DisplayName("Should ignore discrepancy and create audit log with user justification")
    void shouldIgnoreDiscrepancyWithAuditLog() {
        UUID itemId = UUID.randomUUID();
        ReconciliationSettlementItem item = new ReconciliationSettlementItem(
                UUID.randomUUID(), tenantId, "MLB123", "TX-2",
                ReconciliationEntryType.ORDER_PAYOUT, new BigDecimal("98.00")
        );
        item.reconcileMatched(UUID.randomUUID(), new BigDecimal("100.00"), new BigDecimal("-2.00"),
                MatchingMethod.ORDER_ID_EXACT, BigDecimal.ONE, "Divergência pequena");

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReconciliationSettlementItem result = manualService.ignoreDiscrepancy(
                tenantId, itemId, "Divergência irrelevante de centavos acordada comercialmente", userId
        );

        assertThat(result.getReconciliationStatus()).isEqualTo(ReconciliationItemStatus.CONCILED);
        verify(auditLogRepository).save(argThat(log ->
                log.getAction() == ReconciliationAuditAction.DISCREPANCY_IGNORED
        ));
    }

    @Test
    @DisplayName("Should log dispute protocol with mandatory non-automation disclaimer")
    void shouldLogDisputeWithDisclaimer() {
        UUID itemId = UUID.randomUUID();
        ReconciliationSettlementItem item = new ReconciliationSettlementItem(
                UUID.randomUUID(), tenantId, "MLB456", "TX-3",
                ReconciliationEntryType.ORDER_PAYOUT, new BigDecimal("80.00")
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReconciliationDispute dispute = manualService.logDispute(
                tenantId, itemId, "PROT-2026-99988", "MERCADO_LIVRE",
                new BigDecimal("20.00"), DisputeChannel.CHAT_SUPPORT,
                "Taxa indevida cobrada em pedido cancelado", userId
        );

        assertThat(dispute.getProtocolNumber()).isEqualTo("PROT-2026-99988");
        assertThat(dispute.getDisputedAmount()).isEqualByComparingTo("20.00");
        assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.OPEN);
        assertThat(dispute.getDisclaimer()).contains("O sistema não executa envio automático à adquirente");

        verify(auditLogRepository).save(argThat(log ->
                log.getAction() == ReconciliationAuditAction.DISPUTE_LOGGED
        ));
    }
}
