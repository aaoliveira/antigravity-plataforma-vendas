package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionAlertServiceTest {

    @Mock
    private ActionAlertRepository alertRepository;

    @Mock
    private ActionNotificationService notificationService;

    private ActionAlertService alertService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        alertService = new ActionAlertService(alertRepository, notificationService);
    }

    @Test
    @DisplayName("Should create alert with calculated provenance and dispatch notifications")
    void shouldCreateAlertWhenAbsent() {
        when(alertRepository.findByTenantIdAndDedupKey(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(alertRepository.save(any(ActionAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        ActionAlert alert = alertService.createAlertIfAbsent(
                tenantId, UUID.randomUUID(), "Alerta de Margem Negativa",
                "O pedido MLB123 gerou margem de -3.5%", ActionAlertSeverity.CRITICAL,
                ActionAlertDomain.FINANCIAL, AlertScopeType.STORE, "store-1",
                "CALCULATED", "{\"orderId\":\"MLB123\",\"margin\":-3.5}", 60
        );

        assertThat(alert).isNotNull();
        assertThat(alert.getTitle()).isEqualTo("Alerta de Margem Negativa");
        assertThat(alert.getSeverity()).isEqualTo(ActionAlertSeverity.CRITICAL);
        assertThat(alert.getDomainType()).isEqualTo(ActionAlertDomain.FINANCIAL);
        assertThat(alert.getStatus()).isEqualTo(ActionAlertStatus.NEW);
        assertThat(alert.getProvenance()).isEqualTo("CALCULATED");

        verify(alertRepository, times(1)).save(any(ActionAlert.class));
        verify(notificationService, times(1)).dispatchAlertNotifications(any(ActionAlert.class));
    }

    @Test
    @DisplayName("Should suppress duplicate alert within cooldown window and return existing alert")
    void shouldSuppressDuplicateAlertWithinCooldown() {
        ActionAlert existing = new ActionAlert(
                tenantId, UUID.randomUUID(), "Alerta de Queda de Vendas",
                "Queda de 40% nas últimas 2 horas", ActionAlertSeverity.WARNING,
                ActionAlertDomain.FINANCIAL, AlertScopeType.ORGANIZATION, null,
                "CALCULATED", "{\"drop\": 40.0}", "existing-dedup-hash"
        );
        when(alertRepository.findByTenantIdAndDedupKey(eq(tenantId), anyString())).thenReturn(Optional.of(existing));

        ActionAlert result = alertService.createAlertIfAbsent(
                tenantId, existing.getRuleId(), "Alerta de Queda de Vendas",
                "Queda de 40% nas últimas 2 horas", ActionAlertSeverity.WARNING,
                ActionAlertDomain.FINANCIAL, AlertScopeType.ORGANIZATION, null,
                "CALCULATED", "{\"drop\": 40.0}", 60
        );

        assertThat(result).isSameAs(existing);
        verify(alertRepository, never()).save(any(ActionAlert.class));
        verify(notificationService, never()).dispatchAlertNotifications(any(ActionAlert.class));
    }

    @Test
    @DisplayName("Should update alert status from NEW to RESOLVED")
    void shouldUpdateAlertStatus() {
        ActionAlert alert = new ActionAlert(
                tenantId, null, "Alerta Sincronização",
                "Falha ao conectar token", ActionAlertSeverity.CRITICAL,
                ActionAlertDomain.MARKETPLACE_SYNC, AlertScopeType.ORGANIZATION, null,
                "SYSTEM", "{\"errorCode\":\"401_UNAUTHORIZED\"}", "dedup-123"
        );
        when(alertRepository.findByIdAndTenantId(alert.getId(), tenantId)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(ActionAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        ActionAlert updated = alertService.updateStatus(tenantId, alert.getId(), ActionAlertStatus.RESOLVED);

        assertThat(updated.getStatus()).isEqualTo(ActionAlertStatus.RESOLVED);
        verify(alertRepository, times(1)).save(alert);
    }
}
