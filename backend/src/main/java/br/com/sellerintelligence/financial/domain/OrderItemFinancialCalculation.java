package br.com.sellerintelligence.financial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_item_financial_calculations")
public class OrderItemFinancialCalculation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_calculation_id", nullable = false)
    private OrderFinancialCalculation orderCalculation;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(length = 120)
    private String sku;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "unit_cogs", precision = 15, scale = 4)
    private BigDecimal unitCogs;

    @Column(name = "total_cogs", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCogs;

    @Column(name = "marketplace_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal marketplaceFee;

    @Column(name = "shipping_seller", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingSeller;

    @Column(name = "tax_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxValue;

    @Column(name = "packaging_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal packagingCost;

    @Column(name = "other_costs", nullable = false, precision = 15, scale = 2)
    private BigDecimal otherCosts;

    @Column(name = "contribution_margin_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal contributionMarginValue;

    @Column(name = "contribution_margin_percentage", nullable = false, precision = 7, scale = 4)
    private BigDecimal contributionMarginPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_quality", nullable = false, length = 50)
    private CalculationQuality calculationQuality;

    @Column(name = "memory_breakdown", nullable = false, columnDefinition = "TEXT")
    private String memoryBreakdown;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrderItemFinancialCalculation() {}

    public OrderItemFinancialCalculation(
            UUID id, UUID tenantId, OrderFinancialCalculation orderCalculation, UUID orderItemId,
            String sku, BigDecimal unitPrice, Integer quantity, BigDecimal totalPrice,
            BigDecimal unitCogs, BigDecimal totalCogs, BigDecimal marketplaceFee,
            BigDecimal shippingSeller, BigDecimal taxValue, BigDecimal packagingCost,
            BigDecimal otherCosts, BigDecimal contributionMarginValue,
            BigDecimal contributionMarginPercentage, CalculationQuality calculationQuality,
            String memoryBreakdown
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.orderCalculation = orderCalculation;
        this.orderItemId = orderItemId;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity != null ? quantity : 1;
        this.totalPrice = totalPrice;
        this.unitCogs = unitCogs;
        this.totalCogs = totalCogs;
        this.marketplaceFee = marketplaceFee;
        this.shippingSeller = shippingSeller;
        this.taxValue = taxValue;
        this.packagingCost = packagingCost;
        this.otherCosts = otherCosts;
        this.contributionMarginValue = contributionMarginValue;
        this.contributionMarginPercentage = contributionMarginPercentage;
        this.calculationQuality = calculationQuality != null ? calculationQuality : CalculationQuality.COMPLETE;
        this.memoryBreakdown = memoryBreakdown;
        this.createdAt = Instant.now();
    }

    public void setOrderCalculation(OrderFinancialCalculation orderCalculation) {
        this.orderCalculation = orderCalculation;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public OrderFinancialCalculation getOrderCalculation() {
        return orderCalculation;
    }

    public UUID getOrderItemId() {
        return orderItemId;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public BigDecimal getUnitCogs() {
        return unitCogs;
    }

    public BigDecimal getTotalCogs() {
        return totalCogs;
    }

    public BigDecimal getMarketplaceFee() {
        return marketplaceFee;
    }

    public BigDecimal getShippingSeller() {
        return shippingSeller;
    }

    public BigDecimal getTaxValue() {
        return taxValue;
    }

    public BigDecimal getPackagingCost() {
        return packagingCost;
    }

    public BigDecimal getOtherCosts() {
        return otherCosts;
    }

    public BigDecimal getContributionMarginValue() {
        return contributionMarginValue;
    }

    public BigDecimal getContributionMarginPercentage() {
        return contributionMarginPercentage;
    }

    public CalculationQuality getCalculationQuality() {
        return calculationQuality;
    }

    public String getMemoryBreakdown() {
        return memoryBreakdown;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
