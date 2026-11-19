package com.sellerintelligence.modules.financialanalytics.entity;

import com.sellerintelligence.modules.financialanalytics.model.FinancialValueProvenance;
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
 * Entidade JPA representando os fatos financeiros por item de pedido.
 * Garante conciliação matemática aditiva com o pedido consolidador.
 */
@Entity
@Table(name = "order_item_financial_facts")
public class OrderItemFinancialFactRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "order_fact_id", nullable = false)
    private UUID orderFactId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitPrice;

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

    @Column(name = "unit_product_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitProductCost;

    @Column(name = "product_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal productCost;

    @Column(name = "packaging_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal packagingCost;

    @Column(name = "additional_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal additionalCost;

    @Column(name = "tax_rate_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal taxRatePercent;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_provenance", nullable = false, length = 30)
    private FinancialValueProvenance costProvenance;

    @Column(name = "has_missing_data", nullable = false)
    private boolean hasMissingData;

    @Column(name = "missing_data_reasons", columnDefinition = "TEXT")
    private String missingDataReasons;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderItemFinancialFactRecord() {
        // JPA requirement
    }

    public OrderItemFinancialFactRecord(
            UUID id,
            UUID tenantId,
            UUID orderFactId,
            UUID orderId,
            UUID orderItemId,
            UUID productId,
            UUID variantId,
            String sku,
            String title,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal netRevenue,
            BigDecimal cancellationAmount,
            BigDecimal returnAmount,
            BigDecimal marketplaceFee,
            BigDecimal fixedFee,
            BigDecimal shippingCost,
            BigDecimal unitProductCost,
            BigDecimal productCost,
            BigDecimal packagingCost,
            BigDecimal additionalCost,
            BigDecimal taxRatePercent,
            BigDecimal taxAmount,
            BigDecimal otherExpensesAmount,
            BigDecimal totalDeductions,
            BigDecimal contributionMarginAmount,
            BigDecimal contributionMarginRatio,
            FinancialValueProvenance costProvenance,
            boolean hasMissingData,
            String missingDataReasons
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.orderFactId = Objects.requireNonNull(orderFactId, "orderFactId não pode ser nulo");
        this.orderId = Objects.requireNonNull(orderId, "orderId não pode ser nulo");
        this.orderItemId = Objects.requireNonNull(orderItemId, "orderItemId não pode ser nulo");
        this.productId = productId;
        this.variantId = variantId;
        this.sku = sku;
        this.title = Objects.requireNonNull(title, "title não pode ser nulo");
        this.quantity = quantity != null ? quantity : 1;
        this.unitPrice = scale4(unitPrice);
        this.grossRevenue = scale4(grossRevenue);
        this.discountAmount = scale4(discountAmount);
        this.netRevenue = scale4(netRevenue);
        this.cancellationAmount = scale4(cancellationAmount);
        this.returnAmount = scale4(returnAmount);
        this.marketplaceFee = scale4(marketplaceFee);
        this.fixedFee = scale4(fixedFee);
        this.shippingCost = scale4(shippingCost);
        this.unitProductCost = scale4(unitProductCost);
        this.productCost = scale4(productCost);
        this.packagingCost = scale4(packagingCost);
        this.additionalCost = scale4(additionalCost);
        this.taxRatePercent = scale4(taxRatePercent);
        this.taxAmount = scale4(taxAmount);
        this.otherExpensesAmount = scale4(otherExpensesAmount);
        this.totalDeductions = scale4(totalDeductions);
        this.contributionMarginAmount = scale4(contributionMarginAmount);
        this.contributionMarginRatio = scale4(contributionMarginRatio);
        this.costProvenance = costProvenance != null ? costProvenance : FinancialValueProvenance.USER_INFORMED;
        this.hasMissingData = hasMissingData;
        this.missingDataReasons = missingDataReasons;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateValues(
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal netRevenue,
            BigDecimal cancellationAmount,
            BigDecimal returnAmount,
            BigDecimal marketplaceFee,
            BigDecimal fixedFee,
            BigDecimal shippingCost,
            BigDecimal unitProductCost,
            BigDecimal productCost,
            BigDecimal packagingCost,
            BigDecimal additionalCost,
            BigDecimal taxRatePercent,
            BigDecimal taxAmount,
            BigDecimal otherExpensesAmount,
            BigDecimal totalDeductions,
            BigDecimal contributionMarginAmount,
            BigDecimal contributionMarginRatio,
            FinancialValueProvenance costProvenance,
            boolean hasMissingData,
            String missingDataReasons
    ) {
        if (quantity != null) this.quantity = quantity;
        this.unitPrice = scale4(unitPrice);
        this.grossRevenue = scale4(grossRevenue);
        this.discountAmount = scale4(discountAmount);
        this.netRevenue = scale4(netRevenue);
        this.cancellationAmount = scale4(cancellationAmount);
        this.returnAmount = scale4(returnAmount);
        this.marketplaceFee = scale4(marketplaceFee);
        this.fixedFee = scale4(fixedFee);
        this.shippingCost = scale4(shippingCost);
        this.unitProductCost = scale4(unitProductCost);
        this.productCost = scale4(productCost);
        this.packagingCost = scale4(packagingCost);
        this.additionalCost = scale4(additionalCost);
        this.taxRatePercent = scale4(taxRatePercent);
        this.taxAmount = scale4(taxAmount);
        this.otherExpensesAmount = scale4(otherExpensesAmount);
        this.totalDeductions = scale4(totalDeductions);
        this.contributionMarginAmount = scale4(contributionMarginAmount);
        this.contributionMarginRatio = scale4(contributionMarginRatio);
        this.costProvenance = costProvenance != null ? costProvenance : this.costProvenance;
        this.hasMissingData = hasMissingData;
        this.missingDataReasons = missingDataReasons;
        this.updatedAt = Instant.now();
    }

    public void updateReconciledFees(BigDecimal reconciledFee, BigDecimal reconciledFixedFee, BigDecimal reconciledShipping) {
        if (reconciledFee != null) this.marketplaceFee = scale4(reconciledFee);
        if (reconciledFixedFee != null) this.fixedFee = scale4(reconciledFixedFee);
        if (reconciledShipping != null) this.shippingCost = scale4(reconciledShipping);

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

    public UUID getOrderFactId() {
        return orderFactId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getOrderItemId() {
        return orderItemId;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
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

    public BigDecimal getUnitProductCost() {
        return unitProductCost;
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

    public BigDecimal getTaxRatePercent() {
        return taxRatePercent;
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

    public FinancialValueProvenance getCostProvenance() {
        return costProvenance;
    }

    public boolean isHasMissingData() {
        return hasMissingData;
    }

    public String getMissingDataReasons() {
        return missingDataReasons;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
