package br.com.sellerintelligence.reconciliation.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_audit_logs")
public class ReconciliationAuditLog {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "settlement_item_id", nullable = false)
    private UUID settlementItemId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private ReconciliationAuditAction action;

    @Column(name = "previous_state", columnDefinition = "TEXT")
    private String previousState;

    @Column(name = "new_state", columnDefinition = "TEXT")
    private String newState;

    @Column(name = "justification", nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ReconciliationAuditLog() {}

    public ReconciliationAuditLog(UUID tenantId, UUID settlementItemId, UUID userId,
                                  ReconciliationAuditAction action, String previousState,
                                  String newState, String justification) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.settlementItemId = settlementItemId;
        this.userId = userId;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
        this.justification = justification;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSettlementItemId() { return settlementItemId; }
    public UUID getUserId() { return userId; }
    public ReconciliationAuditAction getAction() { return action; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getJustification() { return justification; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
