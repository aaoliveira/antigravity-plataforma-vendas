package br.com.sellerintelligence.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_disputes")
public class ReconciliationDispute {

    @Id
    private UUID id;

    @Column(name = "settlement_item_id", nullable = false)
    private UUID settlementItemId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "protocol_number", nullable = false, length = 100)
    private String protocolNumber;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "disputed_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal disputedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private DisputeChannel channel = DisputeChannel.HELP_CENTER;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "disclaimer", nullable = false, columnDefinition = "TEXT")
    private String disclaimer = "Protocolo registrado para controle interno do seller. O sistema não executa envio automático à adquirente.";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ReconciliationDispute() {}

    public ReconciliationDispute(UUID settlementItemId, UUID tenantId, String protocolNumber,
                                 String marketplace, BigDecimal disputedAmount,
                                 DisputeChannel channel, String notes) {
        this.id = UUID.randomUUID();
        this.settlementItemId = settlementItemId;
        this.tenantId = tenantId;
        this.protocolNumber = protocolNumber;
        this.marketplace = marketplace;
        this.disputedAmount = disputedAmount;
        this.channel = channel != null ? channel : DisputeChannel.HELP_CENTER;
        this.notes = notes;
        this.status = DisputeStatus.OPEN;
        this.disclaimer = "Protocolo registrado para controle interno do seller. O sistema não executa envio automático à adquirente.";
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateStatus(DisputeStatus newStatus, String additionalNotes) {
        this.status = newStatus;
        if (additionalNotes != null && !additionalNotes.isBlank()) {
            this.notes = (this.notes != null ? this.notes + "\n" : "") + additionalNotes;
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getSettlementItemId() { return settlementItemId; }
    public UUID getTenantId() { return tenantId; }
    public String getProtocolNumber() { return protocolNumber; }
    public String getMarketplace() { return marketplace; }
    public BigDecimal getDisputedAmount() { return disputedAmount; }
    public DisputeStatus getStatus() { return status; }
    public DisputeChannel getChannel() { return channel; }
    public String getNotes() { return notes; }
    public String getDisclaimer() { return disclaimer; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
