package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.CollectionStatus;
import br.com.sellerintelligence.monitoring.domain.MonitoringChangeEvent;
import br.com.sellerintelligence.monitoring.domain.MonitoringChangeEventRepository;
import br.com.sellerintelligence.monitoring.domain.MonitoringEventType;
import br.com.sellerintelligence.monitoring.domain.MonitoringSnapshot;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringDiffEngineTest {

    @Mock
    private MonitoringChangeEventRepository changeEventRepository;

    private MonitoringDiffEngine diffEngine;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        diffEngine = new MonitoringDiffEngine(changeEventRepository);
    }

    @Test
    @DisplayName("Should detect price drop and calculate exact delta in amount and percentage")
    void detectPriceDrop() {
        when(changeEventRepository.existsByTenantIdAndEventHash(any(), any())).thenReturn(false);
        when(changeEventRepository.save(any(MonitoringChangeEvent.class))).thenAnswer(i -> i.getArgument(0));

        Instant t1 = Instant.parse("2026-11-17T10:00:00Z");
        Instant t2 = Instant.parse("2026-11-17T16:00:00Z");

        MonitoringSnapshot previous = new MonitoringSnapshot(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB123",
                new BigDecimal("200.00"), null, "BRL", "ACTIVE",
                "Teclado Gamer Pro", "seller_alpha", "Platinum", true,
                null, CollectionStatus.SUCCESS, null, "API", "OBSERVED", t1
        );

        MonitoringSnapshot current = new MonitoringSnapshot(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB123",
                new BigDecimal("180.00"), null, "BRL", "ACTIVE",
                "Teclado Gamer Pro", "seller_alpha", "Platinum", true,
                null, CollectionStatus.SUCCESS, null, "API", "OBSERVED", t2
        );

        List<MonitoringChangeEvent> changes = diffEngine.detectChanges(previous, current);

        assertThat(changes).hasSize(1);
        MonitoringChangeEvent event = changes.get(0);
        assertThat(event.getEventType()).isEqualTo(MonitoringEventType.PRICE_CHANGE);
        assertThat(event.getPreviousValue()).isEqualTo("200.00");
        assertThat(event.getCurrentValue()).isEqualTo("180.00");
        assertThat(event.getPriceDeltaAmount()).isEqualByComparingTo("-20.00");
        assertThat(event.getPriceDeltaPercentage()).isEqualByComparingTo("-10.0000");
    }

    @Test
    @DisplayName("Should detect status change to OUT_OF_STOCK")
    void detectStatusChangeOutOfStock() {
        when(changeEventRepository.existsByTenantIdAndEventHash(any(), any())).thenReturn(false);
        when(changeEventRepository.save(any(MonitoringChangeEvent.class))).thenAnswer(i -> i.getArgument(0));

        Instant t1 = Instant.parse("2026-11-17T10:00:00Z");
        Instant t2 = Instant.parse("2026-11-17T16:00:00Z");

        MonitoringSnapshot previous = new MonitoringSnapshot(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB123",
                new BigDecimal("150.00"), null, "BRL", "ACTIVE",
                "Mouse Gamer", "seller_alpha", "Platinum", true,
                null, CollectionStatus.SUCCESS, null, "API", "OBSERVED", t1
        );

        MonitoringSnapshot current = new MonitoringSnapshot(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB123",
                new BigDecimal("150.00"), null, "BRL", "OUT_OF_STOCK",
                "Mouse Gamer", "seller_alpha", "Platinum", true,
                null, CollectionStatus.SUCCESS, null, "API", "OBSERVED", t2
        );

        List<MonitoringChangeEvent> changes = diffEngine.detectChanges(previous, current);

        assertThat(changes).hasSize(1);
        MonitoringChangeEvent event = changes.get(0);
        assertThat(event.getEventType()).isEqualTo(MonitoringEventType.OUT_OF_STOCK);
        assertThat(event.getPreviousValue()).isEqualTo("ACTIVE");
        assertThat(event.getCurrentValue()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("Deduplication: existing event hash must prevent duplicate change events")
    void duplicateEventHashSuppressed() {
        when(changeEventRepository.existsByTenantIdAndEventHash(any(), any())).thenReturn(true);

        MonitoringSnapshot previous = new MonitoringSnapshot(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB123",
                new BigDecimal("100.00"), null, "BRL", "ACTIVE",
                "Item", "seller", null, null, null, CollectionStatus.SUCCESS, null, "API", "OBSERVED", Instant.now().minusSeconds(3600)
        );

        MonitoringSnapshot current = new MonitoringSnapshot(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB123",
                new BigDecimal("90.00"), null, "BRL", "ACTIVE",
                "Item", "seller", null, null, null, CollectionStatus.SUCCESS, null, "API", "OBSERVED", Instant.now()
        );

        List<MonitoringChangeEvent> changes = diffEngine.detectChanges(previous, current);

        assertThat(changes).isEmpty();
        verify(changeEventRepository, never()).save(any());
    }
}
