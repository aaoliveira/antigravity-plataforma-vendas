package br.com.sellerintelligence.action.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "action_notification_deliveries")
public class ActionNotificationDelivery {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 64)
    private DeliveryStatus deliveryStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected ActionNotificationDelivery() {}

    public ActionNotificationDelivery(UUID tenantId, UUID alertId, UUID userId,
                                      NotificationChannel channel, String recipient,
                                      DeliveryStatus deliveryStatus, String errorMessage) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.alertId = Objects.requireNonNull(alertId, "alertId cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.channel = Objects.requireNonNull(channel, "channel cannot be null");
        this.recipient = Objects.requireNonNull(recipient, "recipient cannot be null");
        this.deliveryStatus = Objects.requireNonNull(deliveryStatus, "deliveryStatus cannot be null");
        this.errorMessage = errorMessage;
        this.sentAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getAlertId() { return alertId; }
    public UUID getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getSentAt() { return sentAt; }
}
