package br.com.sellerintelligence.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_settlement_items")
public class ReconciliationSettlementItem {

    @Id
    private UUID id;

    @Column(name = "settlement_id", nullable = false)
    private UUID settlementId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "external_order_id", length = 100)
    private String externalOrderId;

    @Column(name = "external_transaction_id", length = 100)
    private String externalTransactionId;

    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 50)
    private ReconciliationEntryType entryType;

    @Column(name = "expected_amount", precision = 15, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "settled_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal settledAmount;

    @Column(name = "discrepancy_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discrepancyAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 50)
    private ReconciliationItemStatus reconciliationStatus = ReconciliationItemStatus.PENDING_MATCH;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "matching_method", nullable = false, length = 50)
    private MatchingMethod matchingMethod = MatchingMethod.UNMATCHED;

    @Column(name = "discrepancy_reason", columnDefinition = "TEXT")
    private String discrepancyReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ReconciliationSettlementItem() {}

    public ReconciliationSettlementItem(UUID settlementId, UUID tenantId, String externalOrderId,
                                      String externalTransactionId, ReconciliationEntryType entryType,
                                      BigDecimal settledAmount) {
        this.id = UUID.randomUUID();
        this.settlementId = settlementId;
        this.tenantId = tenantId;
        this.externalOrderId = externalOrderId;
        this.externalTransactionId = externalTransactionId;
        this.entryType = entryType;
        this.settledAmount = settledAmount;
        this.discrepancyAmount = BigDecimal.ZERO;
        this.reconciliationStatus = ReconciliationItemStatus.PENDING_MATCH;
        this.confidenceScore = BigDecimal.ZERO;
        this.matchingMethod = MatchingMethod.UNMATCHED;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void reconcileMatched(UUID orderId, BigDecimal expectedAmount, BigDecimal discrepancy,
                               MatchingMethod method, BigDecimal confidence, String reason) {
        this.orderId = orderId;
        this.expectedAmount = expectedAmount;
        this.discrepancyAmount = discrepancy;
        this.matchingMethod = method;
        this.confidenceScore = confidence;
        this.discrepancyReason = reason;

        if (discrepancy.compareTo(new BigDecimal("0.01")) > 0 || discrepancy.compareTo(new BigDecimal("-0.01")) < 0) {
            this.reconciliationStatus = ReconciliationItemStatus.DISCREPANT;
        } else {
            this.reconciliationStatus = ReconciliationItemStatus.CONCILED;
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public void markUnidentified(String reason) {
        this.reconciliationStatus = ReconciliationItemStatus.UNIDENTIFIED;
        this.matchingMethod = MatchingMethod.UNMATCHED;
        this.confidenceScore = BigDecimal.ZERO;
        this.discrepancyReason = reason;
        this.updatedAt = OffsetDateTime.now();
    }

    public void manualOverride(UUID orderId, String justification) {
        this.orderId = orderId;
        this.reconciliationStatus = ReconciliationItemStatus.CONCILED;
        this.matchingMethod = MatchingMethod.MANUAL_ASSOCIATION;
        this.confidenceScore = new BigDecimal("1.0000");
        this.discrepancyReason = "Conciliado manualmente: " + justification;
        this.updatedAt = OffsetDateTime.now();
    }

    public void ignoreDiscrepancy(String justification) {
        this.reconciliationStatus = ReconciliationItemStatus.CONCILED;
        this.discrepancyReason = "Divergência abonada/ignorada pelo usuário: " + justification;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getSettlementId() { return settlementId; }
    public UUID getTenantId() { return tenantId; }
    public String getExternalOrderId() { return externalOrderId; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public UUID getOrderId() { return orderId; }
    public ReconciliationEntryType getEntryType() { return entryType; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public BigDecimal getSettledAmount() { return settledAmount; }
    public BigDecimal getDiscrepancyAmount() { return discrepancyAmount; }
    public ReconciliationItemStatus getReconciliationStatus() { return reconciliationStatus; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public MatchingMethod getMatchingMethod() { return matchingMethod; }
    public String getDiscrepancyReason() { return discrepancyReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
