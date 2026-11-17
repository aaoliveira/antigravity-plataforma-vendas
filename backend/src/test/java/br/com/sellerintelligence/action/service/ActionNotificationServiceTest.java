package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionNotificationServiceTest {

    @Mock
    private ActionNotificationPreferenceRepository preferenceRepository;

    @Mock
    private ActionNotificationDeliveryRepository deliveryRepository;

    @Mock
    private NotificationSender notificationSender;

    private ActionNotificationService notificationService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificationService = new ActionNotificationService(preferenceRepository, deliveryRepository, notificationSender);
    }

    @Test
    @DisplayName("Should suppress notification when channel opt-in is false (Strict Consent Guardrail)")
    void shouldSuppressNotificationWhenNoOptIn() {
        ActionNotificationPreference pref = new ActionNotificationPreference(
                tenantId, userId, NotificationChannel.EMAIL, false, "seller@example.com", 10
        );
        when(preferenceRepository.findByTenantId(tenantId)).thenReturn(List.of(pref));

        ActionAlert alert = new ActionAlert(
                tenantId, null, "Alerta Importante", "Conteúdo do alerta",
                ActionAlertSeverity.WARNING, ActionAlertDomain.FINANCIAL,
                AlertScopeType.ORGANIZATION, null, "CALCULATED", "{}", "dedup-1"
        );

        notificationService.dispatchAlertNotifications(alert);

        verify(notificationSender, never()).send(any(), any(), any());
        verify(deliveryRepository, times(1)).save(argThat(delivery ->
                delivery.getDeliveryStatus() == DeliveryStatus.SUPPRESSED_NO_OPT_IN &&
                delivery.getChannel() == NotificationChannel.EMAIL
        ));
    }

    @Test
    @DisplayName("Should suppress notification when frequency limit per hour is exceeded")
    void shouldSuppressNotificationWhenFrequencyLimitExceeded() {
        ActionNotificationPreference pref = new ActionNotificationPreference(
                tenantId, userId, NotificationChannel.WEBHOOK, true, "https://webhook.site/test", 3
        );
        when(preferenceRepository.findByTenantId(tenantId)).thenReturn(List.of(pref));
        when(deliveryRepository.countByTenantIdAndUserIdAndChannelAndDeliveryStatusAndSentAtAfter(
                eq(tenantId), eq(userId), eq(NotificationChannel.WEBHOOK), eq(DeliveryStatus.SENT), any(Instant.class)
        )).thenReturn(3L); // already sent 3 notifications in the last hour

        ActionAlert alert = new ActionAlert(
                tenantId, null, "Alerta Frequente", "Descrição",
                ActionAlertSeverity.INFO, ActionAlertDomain.MARKETPLACE_SYNC,
                AlertScopeType.ORGANIZATION, null, "SYSTEM", "{}", "dedup-2"
        );

        notificationService.dispatchAlertNotifications(alert);

        verify(notificationSender, never()).send(any(), any(), any());
        verify(deliveryRepository, times(1)).save(argThat(delivery ->
                delivery.getDeliveryStatus() == DeliveryStatus.SUPPRESSED_COOLDOWN &&
                delivery.getChannel() == NotificationChannel.WEBHOOK
        ));
    }

    @Test
    @DisplayName("Should successfully dispatch notification when opted-in and within frequency limit")
    void shouldDispatchNotificationWhenOptedInAndWithinLimit() {
        ActionNotificationPreference pref = new ActionNotificationPreference(
                tenantId, userId, NotificationChannel.EMAIL, true, "approved@example.com", 10
        );
        when(preferenceRepository.findByTenantId(tenantId)).thenReturn(List.of(pref));
        when(deliveryRepository.countByTenantIdAndUserIdAndChannelAndDeliveryStatusAndSentAtAfter(
                eq(tenantId), eq(userId), eq(NotificationChannel.EMAIL), eq(DeliveryStatus.SENT), any(Instant.class)
        )).thenReturn(1L);
        when(notificationSender.send(any(), eq(NotificationChannel.EMAIL), eq("approved@example.com"))).thenReturn(true);

        ActionAlert alert = new ActionAlert(
                tenantId, null, "Alerta Aprovado", "Margem recalculada",
                ActionAlertSeverity.CRITICAL, ActionAlertDomain.FINANCIAL,
                AlertScopeType.ORGANIZATION, null, "CALCULATED", "{}", "dedup-3"
        );

        notificationService.dispatchAlertNotifications(alert);

        verify(notificationSender, times(1)).send(alert, NotificationChannel.EMAIL, "approved@example.com");
        verify(deliveryRepository, times(1)).save(argThat(delivery ->
                delivery.getDeliveryStatus() == DeliveryStatus.SENT &&
                delivery.getRecipient().equals("approved@example.com")
        ));
    }
}
