package br.com.sellerintelligence.reconciliation.service;

import br.com.sellerintelligence.reconciliation.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementIngestionServiceTest {

    @Mock
    private ReconciliationSettlementRepository settlementRepository;

    @Mock
    private ReconciliationSettlementItemRepository itemRepository;

    @Mock
    private ReconciliationMatchingEngine matchingEngine;

    private SettlementIngestionService ingestionService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ingestionService = new SettlementIngestionService(settlementRepository, itemRepository, matchingEngine);
    }

    @Test
    @DisplayName("Should ingest new settlement batch and items with idempotency hash")
    void shouldIngestNewSettlementBatch() {
        SettlementIngestionService.SettlementItemIngestDto item1 = new SettlementIngestionService.SettlementItemIngestDto(
                "MLB1029384", "TX-001", ReconciliationEntryType.ORDER_PAYOUT, new BigDecimal("179.90")
        );
        SettlementIngestionService.SettlementItemIngestDto item2 = new SettlementIngestionService.SettlementItemIngestDto(
                "MLB1029384", "TX-002", ReconciliationEntryType.COMMISSION_FEE, new BigDecimal("-21.50")
        );

        SettlementIngestionService.SettlementIngestRequest request = new SettlementIngestionService.SettlementIngestRequest(
                "SETTL-2026-11-01", "MERCADO_LIVRE", null, OffsetDateTime.now(),
                new BigDecimal("179.90"), new BigDecimal("158.40"), new BigDecimal("21.50"),
                BigDecimal.ZERO, BigDecimal.ZERO, "extrato_novembro.csv", List.of(item1, item2)
        );

        when(settlementRepository.findByTenantIdAndMarketplaceAndExternalSettlementId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(settlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ReconciliationSettlement result = ingestionService.ingestSettlement(tenantId, request);

        assertThat(result).isNotNull();
        assertThat(result.getExternalSettlementId()).isEqualTo("SETTL-2026-11-01");
        assertThat(result.getMarketplace()).isEqualTo("MERCADO_LIVRE");
        assertThat(result.getHashFingerprint()).isNotEmpty();

        verify(settlementRepository).save(any());
        verify(itemRepository).saveAll(anyList());
        verify(matchingEngine).reconcileBatch(any(), anyList());
    }

    @Test
    @DisplayName("Should return existing settlement and avoid duplication when reimporting existing batch (Idempotency)")
    void shouldReturnExistingBatchWhenAlreadyImported() {
        ReconciliationSettlement existing = new ReconciliationSettlement(
                tenantId, null, "MERCADO_LIVRE", "SETTL-DUPLICATE", "extrato.csv",
                OffsetDateTime.now(), new BigDecimal("100.00"), new BigDecimal("85.00"),
                new BigDecimal("15.00"), BigDecimal.ZERO, BigDecimal.ZERO, "dummy-hash"
        );

        SettlementIngestionService.SettlementIngestRequest request = new SettlementIngestionService.SettlementIngestRequest(
                "SETTL-DUPLICATE", "MERCADO_LIVRE", null, OffsetDateTime.now(),
                new BigDecimal("100.00"), new BigDecimal("85.00"), new BigDecimal("15.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, "extrato.csv", List.of()
        );

        when(settlementRepository.findByTenantIdAndMarketplaceAndExternalSettlementId(tenantId, "MERCADO_LIVRE", "SETTL-DUPLICATE"))
                .thenReturn(Optional.of(existing));

        ReconciliationSettlement result = ingestionService.ingestSettlement(tenantId, request);

        assertThat(result).isSameAs(existing);
        verify(settlementRepository, never()).save(any());
        verify(itemRepository, never()).saveAll(any());
        verify(matchingEngine, never()).reconcileBatch(any(), any());
    }
}
