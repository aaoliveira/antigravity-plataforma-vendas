package com.sellerintelligence.modules.financialanalytics.entity;

import com.sellerintelligence.modules.financialanalytics.model.FinancialValueProvenance;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade JPA representando os fatos financeiros consolidados por pedido de venda próprio.
 * Escopada estritamente por tenant_id e protegida por RLS.
 */
@Entity
@Table(name = "order_financial_facts")
public class OrderFinancialFactRecord {

    public static final String ANTI_HALLUCINATION_DISCLAIMER =
            "Este demonstrativo apura estritamente a Margem de Contribuição (Receita Líquida deduzida de custos diretos variáveis, comissões, fretes e impostos informados). Despesas fixas estruturais e operacionais da empresa não estão contempladas, sendo proibido rotular este valor como Lucro Líquido.";

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "seller_account_id", nullable = false)
    private UUID sellerAccountId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Column(name = "order_status", nullable = false, length = 50)
    private String orderStatus;

    @Column(name = "order_date", nullable = false)
    private Instant orderDate;

    @Column(name = "gross_revenue", nullable = false, precision = 15, scale = 4)
    private BigDecimal grossRevenue;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "net_revenue", nullable = false, precision = 15, scale = 4)
    private BigDecimal netRevenue;

    @Column(name = "cancellation_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal cancellationAmount;

    @Column(name = "return_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal returnAmount;

    @Column(name = "marketplace_fee", nullable = false, precision = 15, scale = 4)
    private BigDecimal marketplaceFee;

    @Column(name = "fixed_fee", nullable = false, precision = 15, scale = 4)
    private BigDecimal fixedFee;

    @Column(name = "shipping_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal shippingCost;

    @Column(name = "product_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal productCost;

    @Column(name = "packaging_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal packagingCost;

    @Column(name = "additional_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal additionalCost;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "other_expenses_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal otherExpensesAmount;

    @Column(name = "total_deductions", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalDeductions;

    @Column(name = "contribution_margin_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal contributionMarginAmount;

    @Column(name = "contribution_margin_ratio", nullable = false, precision = 8, scale = 4)
    private BigDecimal contributionMarginRatio;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    private ReconciliationStatus reconciliationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_provenance", nullable = false, length = 30)
    private FinancialValueProvenance feeProvenance;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_provenance", nullable = false, length = 30)
    private FinancialValueProvenance shippingProvenance;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_provenance", nullable = false, length = 30)
    private FinancialValueProvenance costProvenance;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_provenance", nullable = false, length = 30)
    private FinancialValueProvenance taxProvenance;

    @Column(name = "has_missing_data", nullable = false)
    private boolean hasMissingData;

    @Column(name = "missing_data_reasons", columnDefinition = "TEXT")
    private String missingDataReasons;

    @Column(name = "calculation_version", nullable = false, length = 20)
    private String calculationVersion;

    @Column(name = "calculation_trace_json", columnDefinition = "TEXT")
    private String calculationTraceJson;

    @Column(name = "settlement_external_id", length = 100)
    private String settlementExternalId;

    @Column(name = "settlement_date")
    private Instant settlementDate;

    @Column(name = "settlement_notes", columnDefinition = "TEXT")
    private String settlementNotes;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderFinancialFactRecord() {
        // JPA requirement
    }

    public OrderFinancialFactRecord(
            UUID id,
            UUID tenantId,
            UUID orderId,
            UUID sellerAccountId,
            UUID companyId,
            String marketplace,
            String externalOrderId,
            String orderStatus,
            Instant orderDate,
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal netRevenue,
            BigDecimal cancellationAmount,
            BigDecimal returnAmount,
            BigDecimal marketplaceFee,
            BigDecimal fixedFee,
            BigDecimal shippingCost,
            BigDecimal productCost,
            BigDecimal packagingCost,
            BigDecimal additionalCost,
            BigDecimal taxAmount,
            BigDecimal otherExpensesAmount,
            BigDecimal totalDeductions,
            BigDecimal contributionMarginAmount,
            BigDecimal contributionMarginRatio,
            String currency,
            ReconciliationStatus reconciliationStatus,
            FinancialValueProvenance feeProvenance,
            FinancialValueProvenance shippingProvenance,
            FinancialValueProvenance costProvenance,
            FinancialValueProvenance taxProvenance,
            boolean hasMissingData,
            String missingDataReasons,
            String calculationVersion,
            String calculationTraceJson
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.orderId = Objects.requireNonNull(orderId, "orderId não pode ser nulo");
        this.sellerAccountId = Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        this.companyId = companyId;
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.externalOrderId = Objects.requireNonNull(externalOrderId, "externalOrderId não pode ser nulo");
        this.orderStatus = Objects.requireNonNull(orderStatus, "orderStatus não pode ser nulo");
        this.orderDate = Objects.requireNonNull(orderDate, "orderDate não pode ser nulo");
        this.grossRevenue = scale4(grossRevenue);
        this.discountAmount = scale4(discountAmount);
        this.netRevenue = scale4(netRevenue);
        this.cancellationAmount = scale4(cancellationAmount);
        this.returnAmount = scale4(returnAmount);
        this.marketplaceFee = scale4(marketplaceFee);
        this.fixedFee = scale4(fixedFee);
        this.shippingCost = scale4(shippingCost);
        this.productCost = scale4(productCost);
        this.packagingCost = scale4(packagingCost);
        this.additionalCost = scale4(additionalCost);
        this.taxAmount = scale4(taxAmount);
        this.otherExpensesAmount = scale4(otherExpensesAmount);
        this.totalDeductions = scale4(totalDeductions);
        this.contributionMarginAmount = scale4(contributionMarginAmount);
        this.contributionMarginRatio = scale4(contributionMarginRatio);
        this.currency = currency != null ? currency : "BRL";
        this.reconciliationStatus = reconciliationStatus != null ? reconciliationStatus : ReconciliationStatus.ESTIMATED;
        this.feeProvenance = feeProvenance != null ? feeProvenance : FinancialValueProvenance.ESTIMATED;
        this.shippingProvenance = shippingProvenance != null ? shippingProvenance : FinancialValueProvenance.ESTIMATED;
        this.costProvenance = costProvenance != null ? costProvenance : FinancialValueProvenance.USER_INFORMED;
        this.taxProvenance = taxProvenance != null ? taxProvenance : FinancialValueProvenance.USER_INFORMED;
        this.hasMissingData = hasMissingData;
        this.missingDataReasons = missingDataReasons;
        this.calculationVersion = calculationVersion != null ? calculationVersion : "v1.0.0";
        this.calculationTraceJson = calculationTraceJson;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateComputedValues(
            UUID companyId,
            String orderStatus,
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal netRevenue,
            BigDecimal cancellationAmount,
            BigDecimal returnAmount,
            BigDecimal marketplaceFee,
            BigDecimal fixedFee,
            BigDecimal shippingCost,
            BigDecimal productCost,
            BigDecimal packagingCost,
            BigDecimal additionalCost,
            BigDecimal taxAmount,
            BigDecimal otherExpensesAmount,
            BigDecimal totalDeductions,
            BigDecimal contributionMarginAmount,
            BigDecimal contributionMarginRatio,
            FinancialValueProvenance feeProvenance,
            FinancialValueProvenance shippingProvenance,
            FinancialValueProvenance costProvenance,
            FinancialValueProvenance taxProvenance,
            boolean hasMissingData,
            String missingDataReasons,
            String calculationVersion,
            String calculationTraceJson
    ) {
        this.companyId = companyId;
        this.orderStatus = Objects.requireNonNull(orderStatus, "orderStatus não pode ser nulo");
        this.grossRevenue = scale4(grossRevenue);
        this.discountAmount = scale4(discountAmount);
        this.netRevenue = scale4(netRevenue);
        this.cancellationAmount = scale4(cancellationAmount);
        this.returnAmount = scale4(returnAmount);
        this.marketplaceFee = scale4(marketplaceFee);
        this.fixedFee = scale4(fixedFee);
        this.shippingCost = scale4(shippingCost);
        this.productCost = scale4(productCost);
        this.packagingCost = scale4(packagingCost);
        this.additionalCost = scale4(additionalCost);
        this.taxAmount = scale4(taxAmount);
        this.otherExpensesAmount = scale4(otherExpensesAmount);
        this.totalDeductions = scale4(totalDeductions);
        this.contributionMarginAmount = scale4(contributionMarginAmount);
        this.contributionMarginRatio = scale4(contributionMarginRatio);
        this.feeProvenance = feeProvenance != null ? feeProvenance : this.feeProvenance;
        this.shippingProvenance = shippingProvenance != null ? shippingProvenance : this.shippingProvenance;
        this.costProvenance = costProvenance != null ? costProvenance : this.costProvenance;
        this.taxProvenance = taxProvenance != null ? taxProvenance : this.taxProvenance;
        this.hasMissingData = hasMissingData;
        this.missingDataReasons = missingDataReasons;
        this.calculationVersion = calculationVersion != null ? calculationVersion : this.calculationVersion;
        this.calculationTraceJson = calculationTraceJson;
        this.updatedAt = Instant.now();
    }

    public void applyReconciliation(
            String settlementExternalId,
            Instant settlementDate,
            BigDecimal confirmedMarketplaceFee,
            BigDecimal confirmedFixedFee,
            BigDecimal confirmedShippingCost,
            ReconciliationStatus status,
            String settlementNotes,
            String updatedTraceJson
    ) {
        this.settlementExternalId = settlementExternalId;
        this.settlementDate = settlementDate != null ? settlementDate : Instant.now();
        if (confirmedMarketplaceFee != null) {
            this.marketplaceFee = scale4(confirmedMarketplaceFee);
            this.feeProvenance = FinancialValueProvenance.CONFIRMED;
        }
        if (confirmedFixedFee != null) {
            this.fixedFee = scale4(confirmedFixedFee);
        }
        if (confirmedShippingCost != null) {
            this.shippingCost = scale4(confirmedShippingCost);
            this.shippingProvenance = FinancialValueProvenance.CONFIRMED;
        }
        this.reconciliationStatus = Objects.requireNonNull(status, "status não pode ser nulo");
        this.settlementNotes = settlementNotes;
        this.reconciledAt = Instant.now();

        // Recalcula totais
        this.totalDeductions = scale4(
                this.marketplaceFee
                        .add(this.fixedFee)
                        .add(this.shippingCost)
                        .add(this.productCost)
                        .add(this.packagingCost)
                        .add(this.additionalCost)
                        .add(this.taxAmount)
                        .add(this.otherExpensesAmount)
        );
        this.contributionMarginAmount = scale4(this.netRevenue.subtract(this.totalDeductions));
        if (this.netRevenue.compareTo(BigDecimal.ZERO) > 0) {
            this.contributionMarginRatio = scale4(
                    this.contributionMarginAmount
                            .multiply(BigDecimal.valueOf(100))
                            .divide(this.netRevenue, 4, RoundingMode.HALF_EVEN)
            );
        } else {
            this.contributionMarginRatio = scale4(BigDecimal.ZERO);
        }
        if (updatedTraceJson != null) {
            this.calculationTraceJson = updatedTraceJson;
        }
        this.updatedAt = Instant.now();
    }

    private static BigDecimal scale4(BigDecimal val) {
        return val != null ? val.setScale(4, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getSellerAccountId() {
        return sellerAccountId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public Instant getOrderDate() {
        return orderDate;
    }

    public BigDecimal getGrossRevenue() {
        return grossRevenue;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getNetRevenue() {
        return netRevenue;
    }

    public BigDecimal getCancellationAmount() {
        return cancellationAmount;
    }

    public BigDecimal getReturnAmount() {
        return returnAmount;
    }

    public BigDecimal getMarketplaceFee() {
        return marketplaceFee;
    }

    public BigDecimal getFixedFee() {
        return fixedFee;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public BigDecimal getProductCost() {
        return productCost;
    }

    public BigDecimal getPackagingCost() {
        return packagingCost;
    }

    public BigDecimal getAdditionalCost() {
        return additionalCost;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getOtherExpensesAmount() {
        return otherExpensesAmount;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public BigDecimal getContributionMarginAmount() {
        return contributionMarginAmount;
    }

    public BigDecimal getContributionMarginRatio() {
        return contributionMarginRatio;
    }

    public String getCurrency() {
        return currency;
    }

    public ReconciliationStatus getReconciliationStatus() {
        return reconciliationStatus;
    }

    public FinancialValueProvenance getFeeProvenance() {
        return feeProvenance;
    }

    public FinancialValueProvenance getShippingProvenance() {
        return shippingProvenance;
    }

    public FinancialValueProvenance getCostProvenance() {
        return costProvenance;
    }

    public FinancialValueProvenance getTaxProvenance() {
        return taxProvenance;
    }

    public boolean isHasMissingData() {
        return hasMissingData;
    }

    public String getMissingDataReasons() {
        return missingDataReasons;
    }

    public String getCalculationVersion() {
        return calculationVersion;
    }

    public String getCalculationTraceJson() {
        return calculationTraceJson;
    }

    public String getSettlementExternalId() {
        return settlementExternalId;
    }

    public Instant getSettlementDate() {
        return settlementDate;
    }

    public String getSettlementNotes() {
        return settlementNotes;
    }

    public Instant getReconciledAt() {
        return reconciledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
