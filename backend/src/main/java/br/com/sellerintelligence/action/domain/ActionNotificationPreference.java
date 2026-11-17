package br.com.sellerintelligence.action.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "action_notification_preferences")
public class ActionNotificationPreference {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(name = "is_opted_in", nullable = false)
    private boolean optedIn;

    @Column(name = "target_address", length = 255)
    private String targetAddress;

    @Column(name = "frequency_limit_per_hour", nullable = false)
    private int frequencyLimitPerHour;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActionNotificationPreference() {}

    public ActionNotificationPreference(UUID tenantId, UUID userId, NotificationChannel channel,
                                        boolean optedIn, String targetAddress, int frequencyLimitPerHour) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.channel = Objects.requireNonNull(channel, "channel cannot be null");
        this.optedIn = optedIn;
        this.targetAddress = targetAddress;
        this.frequencyLimitPerHour = frequencyLimitPerHour > 0 ? frequencyLimitPerHour : 10;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(boolean optedIn, String targetAddress, int frequencyLimitPerHour) {
        this.optedIn = optedIn;
        if (targetAddress != null) this.targetAddress = targetAddress;
        if (frequencyLimitPerHour > 0) this.frequencyLimitPerHour = frequencyLimitPerHour;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public boolean isOptedIn() { return optedIn; }
    public String getTargetAddress() { return targetAddress; }
    public int getFrequencyLimitPerHour() { return frequencyLimitPerHour; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
