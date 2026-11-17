package br.com.sellerintelligence.sync.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace", nullable = false, length = 50)
    private Marketplace marketplace;

    @Column(name = "external_event_id", nullable = false, length = 255)
    private String externalEventId;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WebhookStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected WebhookEvent() {}

    public WebhookEvent(UUID tenantId, Marketplace marketplace, String externalEventId,
                        String topic, String resourceId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.externalEventId = Objects.requireNonNull(externalEventId, "externalEventId cannot be null").trim();
        this.topic = Objects.requireNonNull(topic, "topic cannot be null").trim();
        this.resourceId = resourceId;
        this.receivedAt = Instant.now();
        this.status = WebhookStatus.RECEIVED;
    }

    public void markProcessed() {
        this.status = WebhookStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void markDuplicate() {
        this.status = WebhookStatus.DUPLICATE_IGNORED;
        this.processedAt = Instant.now();
    }

    public void markOutOfOrder() {
        this.status = WebhookStatus.OUT_OF_ORDER_IGNORED;
        this.processedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = WebhookStatus.FAILED;
        this.errorMessage = error;
        this.processedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public String getTopic() {
        return topic;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public WebhookStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
