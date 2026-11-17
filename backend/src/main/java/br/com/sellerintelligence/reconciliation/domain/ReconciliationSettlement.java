package br.com.sellerintelligence.reconciliation.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_settlements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_settlement_idempotency", columnNames = {"tenant_id", "marketplace", "external_settlement_id"})
})
public class ReconciliationSettlement {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "external_settlement_id", nullable = false, length = 100)
    private String externalSettlementId;

    @Column(name = "source_filename", length = 255)
    private String sourceFilename;

    @Column(name = "settlement_date", nullable = false)
    private OffsetDateTime settlementDate;

    @Column(name = "gross_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "net_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "total_fees", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalFees = BigDecimal.ZERO;

    @Column(name = "total_shipping", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalShipping = BigDecimal.ZERO;

    @Column(name = "total_adjustments", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAdjustments = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "BRL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SettlementStatus status = SettlementStatus.IMPORTED;

    @Column(name = "hash_fingerprint", nullable = false, length = 64)
    private String hashFingerprint;

    @Column(name = "total_items_count", nullable = false)
    private int totalItemsCount = 0;

    @Column(name = "reconciled_items_count", nullable = false)
    private int reconciledItemsCount = 0;

    @Column(name = "discrepant_items_count", nullable = false)
    private int discrepantItemsCount = 0;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ReconciliationSettlement() {}

    public ReconciliationSettlement(UUID tenantId, UUID storeId, String marketplace,
                                  String externalSettlementId, String sourceFilename,
                                  OffsetDateTime settlementDate, BigDecimal grossAmount,
                                  BigDecimal netAmount, BigDecimal totalFees,
                                  BigDecimal totalShipping, BigDecimal totalAdjustments,
                                  String hashFingerprint) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.marketplace = marketplace;
        this.externalSettlementId = externalSettlementId;
        this.sourceFilename = sourceFilename;
        this.settlementDate = settlementDate;
        this.grossAmount = grossAmount;
        this.netAmount = netAmount;
        this.totalFees = totalFees != null ? totalFees : BigDecimal.ZERO;
        this.totalShipping = totalShipping != null ? totalShipping : BigDecimal.ZERO;
        this.totalAdjustments = totalAdjustments != null ? totalAdjustments : BigDecimal.ZERO;
        this.hashFingerprint = hashFingerprint;
        this.status = SettlementStatus.IMPORTED;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void updateMetrics(int total, int reconciled, int discrepant) {
        this.totalItemsCount = total;
        this.reconciledItemsCount = reconciled;
        this.discrepantItemsCount = discrepant;
        if (discrepant > 0) {
            this.status = SettlementStatus.HAS_DISCREPANCIES;
        } else if (reconciled == total && total > 0) {
            this.status = SettlementStatus.RECONCILED;
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getStoreId() { return storeId; }
    public String getMarketplace() { return marketplace; }
    public String getExternalSettlementId() { return externalSettlementId; }
    public String getSourceFilename() { return sourceFilename; }
    public OffsetDateTime getSettlementDate() { return settlementDate; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public BigDecimal getTotalFees() { return totalFees; }
    public BigDecimal getTotalShipping() { return totalShipping; }
    public BigDecimal getTotalAdjustments() { return totalAdjustments; }
    public String getCurrency() { return currency; }
    public SettlementStatus getStatus() { return status; }
    public String getHashFingerprint() { return hashFingerprint; }
    public int getTotalItemsCount() { return totalItemsCount; }
    public int getReconciledItemsCount() { return reconciledItemsCount; }
    public int getDiscrepantItemsCount() { return discrepantItemsCount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
