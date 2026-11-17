package br.com.sellerintelligence.action.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActionNotificationDeliveryRepository extends JpaRepository<ActionNotificationDelivery, UUID> {

    List<ActionNotificationDelivery> findByTenantIdOrderBySentAtDesc(UUID tenantId);

    List<ActionNotificationDelivery> findByAlertIdOrderBySentAtDesc(UUID alertId);

    long countByTenantIdAndUserIdAndChannelAndDeliveryStatusAndSentAtAfter(
            UUID tenantId, UUID userId, NotificationChannel channel, DeliveryStatus deliveryStatus, Instant sentAtAfter);
}
