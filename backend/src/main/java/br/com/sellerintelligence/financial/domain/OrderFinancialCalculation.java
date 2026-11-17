package br.com.sellerintelligence.financial.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_financial_calculations")
public class OrderFinancialCalculation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "rule_set_id")
    private UUID ruleSetId;

    @Column(name = "rule_version", nullable = false)
    private Integer ruleVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_status", nullable = false, length = 50)
    private CalculationStatus calculationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_quality", nullable = false, length = 50)
    private CalculationQuality calculationQuality;

    @Column(name = "gross_revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossRevenue;

    @Column(name = "net_revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal netRevenue;

    @Column(name = "total_cogs", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCogs;

    @Column(name = "total_marketplace_fees", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalMarketplaceFees;

    @Column(name = "total_shipping_seller", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalShippingSeller;

    @Column(name = "total_taxes", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTaxes;

    @Column(name = "total_packaging", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPackaging;

    @Column(name = "total_other_costs", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalOtherCosts;

    @Column(name = "contribution_margin_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal contributionMarginValue;

    @Column(name = "contribution_margin_percentage", nullable = false, precision = 7, scale = 4)
    private BigDecimal contributionMarginPercentage;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "missing_fields", columnDefinition = "TEXT")
    private String missingFields;

    @Column(columnDefinition = "TEXT")
    private String assumptions;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "recalculated_at")
    private Instant recalculatedAt;

    @Column(name = "recalculation_reason", columnDefinition = "TEXT")
    private String recalculationReason;

    @OneToMany(mappedBy = "orderCalculation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemFinancialCalculation> items = new ArrayList<>();

    protected OrderFinancialCalculation() {}

    public OrderFinancialCalculation(
            UUID id, UUID tenantId, UUID orderId, UUID ruleSetId, Integer ruleVersion,
            CalculationStatus calculationStatus, CalculationQuality calculationQuality,
            BigDecimal grossRevenue, BigDecimal netRevenue, BigDecimal totalCogs,
            BigDecimal totalMarketplaceFees, BigDecimal totalShippingSeller, BigDecimal totalTaxes,
            BigDecimal totalPackaging, BigDecimal totalOtherCosts, BigDecimal contributionMarginValue,
            BigDecimal contributionMarginPercentage, String currency, String missingFields,
            String assumptions, Instant calculatedAt, Instant closedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.orderId = orderId;
        this.ruleSetId = ruleSetId;
        this.ruleVersion = ruleVersion != null ? ruleVersion : 1;
        this.calculationStatus = calculationStatus != null ? calculationStatus : CalculationStatus.CLOSED;
        this.calculationQuality = calculationQuality != null ? calculationQuality : CalculationQuality.COMPLETE;
        this.grossRevenue = grossRevenue;
        this.netRevenue = netRevenue;
        this.totalCogs = totalCogs;
        this.totalMarketplaceFees = totalMarketplaceFees;
        this.totalShippingSeller = totalShippingSeller;
        this.totalTaxes = totalTaxes;
        this.totalPackaging = totalPackaging;
        this.totalOtherCosts = totalOtherCosts;
        this.contributionMarginValue = contributionMarginValue;
        this.contributionMarginPercentage = contributionMarginPercentage;
        this.currency = currency != null ? currency : "BRL";
        this.missingFields = missingFields;
        this.assumptions = assumptions;
        this.calculatedAt = calculatedAt != null ? calculatedAt : Instant.now();
        this.closedAt = closedAt;
    }

    public void addItemCalculation(OrderItemFinancialCalculation itemCalc) {
        itemCalc.setOrderCalculation(this);
        this.items.add(itemCalc);
    }

    public void markRecalculated(String reason) {
        this.calculationStatus = CalculationStatus.RECALCULATED;
        this.recalculatedAt = Instant.now();
        this.recalculationReason = reason;
    }

    public void markCancelled(String reason) {
        this.calculationStatus = CalculationStatus.CANCELLED;
        this.grossRevenue = BigDecimal.ZERO.setScale(2);
        this.netRevenue = BigDecimal.ZERO.setScale(2);
        this.totalMarketplaceFees = BigDecimal.ZERO.setScale(2);
        this.totalTaxes = BigDecimal.ZERO.setScale(2);
        this.totalCogs = BigDecimal.ZERO.setScale(2);
        this.totalShippingSeller = BigDecimal.ZERO.setScale(2);
        this.totalPackaging = BigDecimal.ZERO.setScale(2);
        this.totalOtherCosts = BigDecimal.ZERO.setScale(2);
        this.contributionMarginValue = BigDecimal.ZERO.setScale(2);
        this.contributionMarginPercentage = BigDecimal.ZERO.setScale(4);
        this.recalculatedAt = Instant.now();
        this.recalculationReason = reason;
    }

    public void markReturned(BigDecimal shippingLoss, BigDecimal packagingLoss, BigDecimal cogsLoss, String reason) {
        this.calculationStatus = CalculationStatus.RETURNED;
        this.grossRevenue = BigDecimal.ZERO.setScale(2);
        this.netRevenue = BigDecimal.ZERO.setScale(2);
        this.totalMarketplaceFees = BigDecimal.ZERO.setScale(2);
        this.totalTaxes = BigDecimal.ZERO.setScale(2);
        this.totalCogs = cogsLoss.setScale(2);
        this.totalShippingSeller = shippingLoss.setScale(2);
        this.totalPackaging = packagingLoss.setScale(2);
        BigDecimal totalLoss = totalCogs.add(totalShippingSeller).add(totalPackaging);
        this.contributionMarginValue = totalLoss.negate();
        this.contributionMarginPercentage = BigDecimal.ZERO.setScale(4);
        this.recalculatedAt = Instant.now();
        this.recalculationReason = reason;
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

    public UUID getRuleSetId() {
        return ruleSetId;
    }

    public Integer getRuleVersion() {
        return ruleVersion;
    }

    public CalculationStatus getCalculationStatus() {
        return calculationStatus;
    }

    public CalculationQuality getCalculationQuality() {
        return calculationQuality;
    }

    public BigDecimal getGrossRevenue() {
        return grossRevenue;
    }

    public BigDecimal getNetRevenue() {
        return netRevenue;
    }

    public BigDecimal getTotalCogs() {
        return totalCogs;
    }

    public BigDecimal getTotalMarketplaceFees() {
        return totalMarketplaceFees;
    }

    public BigDecimal getTotalShippingSeller() {
        return totalShippingSeller;
    }

    public BigDecimal getTotalTaxes() {
        return totalTaxes;
    }

    public BigDecimal getTotalPackaging() {
        return totalPackaging;
    }

    public BigDecimal getTotalOtherCosts() {
        return totalOtherCosts;
    }

    public BigDecimal getContributionMarginValue() {
        return contributionMarginValue;
    }

    public BigDecimal getContributionMarginPercentage() {
        return contributionMarginPercentage;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMissingFields() {
        return missingFields;
    }

    public String getAssumptions() {
        return assumptions;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getRecalculatedAt() {
        return recalculatedAt;
    }

    public String getRecalculationReason() {
        return recalculationReason;
    }

    public List<OrderItemFinancialCalculation> getItems() {
        return items;
    }
}
