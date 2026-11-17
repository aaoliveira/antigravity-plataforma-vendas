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
@Table(name = "financial_rule_components")
public class FinancialRuleComponent {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rule_set_id", nullable = false)
    private FinancialRuleSet ruleSet;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 60)
    private FinancialComponentType componentType;

    @Column(name = "component_value", nullable = false, precision = 15, scale = 4)
    private BigDecimal componentValue;

    @Column(name = "threshold_amount", precision = 15, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "applies_to", nullable = false, length = 30)
    private String appliesTo;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FinancialRuleComponent() {}

    public FinancialRuleComponent(UUID id, FinancialRuleSet ruleSet, FinancialComponentType componentType,
                                  BigDecimal componentValue, BigDecimal thresholdAmount,
                                  String appliesTo, String description) {
        this.id = id != null ? id : UUID.randomUUID();
        this.ruleSet = ruleSet;
        this.componentType = componentType;
        this.componentValue = componentValue;
        this.thresholdAmount = thresholdAmount;
        this.appliesTo = appliesTo != null ? appliesTo : "ITEM";
        this.description = description;
        this.createdAt = Instant.now();
    }

    public void setRuleSet(FinancialRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public UUID getId() {
        return id;
    }

    public FinancialRuleSet getRuleSet() {
        return ruleSet;
    }

    public FinancialComponentType getComponentType() {
        return componentType;
    }

    public BigDecimal getComponentValue() {
        return componentValue;
    }

    public BigDecimal getThresholdAmount() {
        return thresholdAmount;
    }

    public String getAppliesTo() {
        return appliesTo;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
