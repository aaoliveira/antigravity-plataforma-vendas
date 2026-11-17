package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.*;
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
class MonitoringAlertServiceTest {

    @Mock
    private MonitoringAlertRuleRepository ruleRepository;

    @Mock
    private MonitoringAlertRepository alertRepository;

    private MonitoringAlertService alertService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID watchlistId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        alertService = new MonitoringAlertService(ruleRepository, alertRepository);
    }

    @Test
    @DisplayName("Should trigger alert when price drop exceeds configured percentage threshold")
    void triggerAlertOnSignificantPriceDrop() {
        MonitoringAlertRule rule = new MonitoringAlertRule(
                tenantId, watchlistId, "Queda Expressiva de Preço (> 5%)",
                MonitoringEventType.PRICE_CHANGE, new BigDecimal("5.00"), AlertDirection.DECREASE,
                1440, AlertChannel.IN_APP
        );

        when(ruleRepository.findByTenantIdAndStatus(tenantId, "ACTIVE")).thenReturn(List.of(rule));
        when(alertRepository.existsByTenantIdAndDedupKey(any(), any())).thenReturn(false);
        when(alertRepository.save(any(MonitoringAlert.class))).thenAnswer(i -> i.getArgument(0));

        WatchlistItem item = new WatchlistItem(
                watchlistId, tenantId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING,
                "MLB123", "Produto Competidor", null, "rival_store", 360
        );

        MonitoringChangeEvent changeEvent = new MonitoringChangeEvent(
                tenantId, itemId, UUID.randomUUID(), MonitoringEventType.PRICE_CHANGE,
                "100.00", "85.00", new BigDecimal("-15.00"), new BigDecimal("-15.0000"),
                "hash123", Instant.now()
        );

        List<MonitoringAlert> alerts = alertService.evaluateEvent(changeEvent, item);

        assertThat(alerts).hasSize(1);
        MonitoringAlert alert = alerts.get(0);
        assertThat(alert.getTitle()).contains("Queda de Preço");
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.WARNING);
        verify(alertRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should suppress duplicate alert within cooldown window")
    void suppressAlertWithinCooldownWindow() {
        MonitoringAlertRule rule = new MonitoringAlertRule(
                tenantId, watchlistId, "Alerta de Queda",
                MonitoringEventType.PRICE_CHANGE, new BigDecimal("5.00"), AlertDirection.DECREASE,
                1440, AlertChannel.IN_APP
        );

        when(ruleRepository.findByTenantIdAndStatus(tenantId, "ACTIVE")).thenReturn(List.of(rule));
        // Cooldown dedupKey already exists
        when(alertRepository.existsByTenantIdAndDedupKey(any(), any())).thenReturn(true);

        WatchlistItem item = new WatchlistItem(
                watchlistId, tenantId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING,
                "MLB123", "Produto", null, null, 360
        );

        MonitoringChangeEvent changeEvent = new MonitoringChangeEvent(
                tenantId, itemId, UUID.randomUUID(), MonitoringEventType.PRICE_CHANGE,
                "100.00", "85.00", new BigDecimal("-15.00"), new BigDecimal("-15.0000"),
                "hash123", Instant.now()
        );

        List<MonitoringAlert> alerts = alertService.evaluateEvent(changeEvent, item);

        assertThat(alerts).isEmpty();
        verify(alertRepository, never()).save(any());
    }
}
