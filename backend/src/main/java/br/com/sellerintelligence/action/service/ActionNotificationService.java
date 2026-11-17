package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ActionNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ActionNotificationService.class);

    private final ActionNotificationPreferenceRepository preferenceRepository;
    private final ActionNotificationDeliveryRepository deliveryRepository;
    private final NotificationSender notificationSender;

    public ActionNotificationService(ActionNotificationPreferenceRepository preferenceRepository,
                                     ActionNotificationDeliveryRepository deliveryRepository,
                                     NotificationSender notificationSender) {
        this.preferenceRepository = preferenceRepository;
        this.deliveryRepository = deliveryRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public ActionNotificationPreference setPreference(UUID tenantId, UUID userId, NotificationChannel channel,
                                                      boolean optedIn, String targetAddress, int frequencyLimitPerHour) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");

        Optional<ActionNotificationPreference> existing = preferenceRepository.findByTenantIdAndUserIdAndChannel(tenantId, userId, channel);
        ActionNotificationPreference pref;
        if (existing.isPresent()) {
            pref = existing.get();
            pref.update(optedIn, targetAddress, frequencyLimitPerHour);
        } else {
            pref = new ActionNotificationPreference(tenantId, userId, channel, optedIn, targetAddress, frequencyLimitPerHour);
        }
        log.info("Saved notification preference: tenant={}, user={}, channel={}, optedIn={}", tenantId, userId, channel, optedIn);
        return preferenceRepository.save(pref);
    }

    @Transactional(readOnly = true)
    public List<ActionNotificationPreference> getPreferences(UUID tenantId, UUID userId) {
        return preferenceRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    @Transactional
    public void dispatchAlertNotifications(ActionAlert alert) {
        Objects.requireNonNull(alert, "alert cannot be null");
        List<ActionNotificationPreference> preferences = preferenceRepository.findByTenantId(alert.getTenantId());

        if (preferences.isEmpty()) {
            log.debug("No notification preferences configured for tenant {}", alert.getTenantId());
            return;
        }

        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

        for (ActionNotificationPreference pref : preferences) {
            String recipient = pref.getTargetAddress() != null ? pref.getTargetAddress() : ("user:" + pref.getUserId());

            // 1. Strict Opt-In check (NFR-PRV & FR-ACT-006)
            if (!pref.isOptedIn()) {
                log.info("Notification suppressed (NO_OPT_IN): tenant={}, user={}, channel={}",
                        alert.getTenantId(), pref.getUserId(), pref.getChannel());
                ActionNotificationDelivery suppressed = new ActionNotificationDelivery(
                        alert.getTenantId(), alert.getId(), pref.getUserId(), pref.getChannel(),
                        recipient, DeliveryStatus.SUPPRESSED_NO_OPT_IN,
                        "Envio bloqueado: canal desativado sem consentimento/opt-in do usuário."
                );
                deliveryRepository.save(suppressed);
                continue;
            }

            // 2. Frequency Control / Cooldown check
            long sentLastHour = deliveryRepository.countByTenantIdAndUserIdAndChannelAndDeliveryStatusAndSentAtAfter(
                    alert.getTenantId(), pref.getUserId(), pref.getChannel(), DeliveryStatus.SENT, oneHourAgo
            );

            if (sentLastHour >= pref.getFrequencyLimitPerHour()) {
                log.warn("Notification suppressed (COOLDOWN/RATE_LIMIT): tenant={}, user={}, channel={}, sentLastHour={}, limit={}",
                        alert.getTenantId(), pref.getUserId(), pref.getChannel(), sentLastHour, pref.getFrequencyLimitPerHour());
                ActionNotificationDelivery suppressed = new ActionNotificationDelivery(
                        alert.getTenantId(), alert.getId(), pref.getUserId(), pref.getChannel(),
                        recipient, DeliveryStatus.SUPPRESSED_COOLDOWN,
                        "Envio bloqueado por controle de frequência horária (" + sentLastHour + "/" + pref.getFrequencyLimitPerHour() + ")."
                );
                deliveryRepository.save(suppressed);
                continue;
            }

            // 3. Dispatch
            try {
                boolean sent = notificationSender.send(alert, pref.getChannel(), recipient);
                DeliveryStatus status = sent ? DeliveryStatus.SENT : DeliveryStatus.FAILED;
                ActionNotificationDelivery delivery = new ActionNotificationDelivery(
                        alert.getTenantId(), alert.getId(), pref.getUserId(), pref.getChannel(),
                        recipient, status, sent ? null : "Falha ao enviar notificação externa"
                );
                deliveryRepository.save(delivery);
            } catch (Exception ex) {
                log.error("Error dispatching notification via {}: {}", pref.getChannel(), ex.getMessage());
                ActionNotificationDelivery failed = new ActionNotificationDelivery(
                        alert.getTenantId(), alert.getId(), pref.getUserId(), pref.getChannel(),
                        recipient, DeliveryStatus.FAILED, ex.getMessage()
                );
                deliveryRepository.save(failed);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ActionNotificationDelivery> getDeliveriesForAlert(UUID alertId) {
        return deliveryRepository.findByAlertIdOrderBySentAtDesc(alertId);
    }

    @Transactional(readOnly = true)
    public List<ActionNotificationDelivery> getDeliveriesForTenant(UUID tenantId) {
        return deliveryRepository.findByTenantIdOrderBySentAtDesc(tenantId);
    }
}
