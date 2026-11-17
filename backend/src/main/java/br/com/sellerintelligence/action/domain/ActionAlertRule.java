package br.com.sellerintelligence.action.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "action_alert_rules")
public class ActionAlertRule {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 64)
    private AlertRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 32)
    private AlertScopeType scopeType;

    @Column(name = "scope_id", length = 128)
    private String scopeId;

    @Column(name = "threshold_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_operator", nullable = false, length = 32)
    private ThresholdOperator thresholdOperator;

    @Column(name = "time_window_minutes", nullable = false)
    private int timeWindowMinutes;

    @Column(name = "cooldown_minutes", nullable = false)
    private int cooldownMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private ActionAlertSeverity severity;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ActionAlertRule() {}

    public ActionAlertRule(UUID tenantId, String name, AlertRuleType ruleType, AlertScopeType scopeType,
                           String scopeId, BigDecimal thresholdValue, ThresholdOperator thresholdOperator,
                           int timeWindowMinutes, int cooldownMinutes, ActionAlertSeverity severity) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.ruleType = Objects.requireNonNull(ruleType, "ruleType cannot be null");
        this.scopeType = scopeType != null ? scopeType : AlertScopeType.ORGANIZATION;
        this.scopeId = scopeId;
        this.thresholdValue = Objects.requireNonNull(thresholdValue, "thresholdValue cannot be null");
        this.thresholdOperator = thresholdOperator != null ? thresholdOperator : ThresholdOperator.LESS_THAN;
        this.timeWindowMinutes = timeWindowMinutes > 0 ? timeWindowMinutes : 60;
        this.cooldownMinutes = cooldownMinutes >= 0 ? cooldownMinutes : 120;
        this.severity = severity != null ? severity : ActionAlertSeverity.WARNING;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, BigDecimal thresholdValue, ThresholdOperator thresholdOperator,
                       int timeWindowMinutes, int cooldownMinutes, ActionAlertSeverity severity, boolean active) {
        if (name != null && !name.isBlank()) this.name = name;
        if (thresholdValue != null) this.thresholdValue = thresholdValue;
        if (thresholdOperator != null) this.thresholdOperator = thresholdOperator;
        if (timeWindowMinutes > 0) this.timeWindowMinutes = timeWindowMinutes;
        if (cooldownMinutes >= 0) this.cooldownMinutes = cooldownMinutes;
        if (severity != null) this.severity = severity;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public AlertRuleType getRuleType() { return ruleType; }
    public AlertScopeType getScopeType() { return scopeType; }
    public String getScopeId() { return scopeId; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public ThresholdOperator getThresholdOperator() { return thresholdOperator; }
    public int getTimeWindowMinutes() { return timeWindowMinutes; }
    public int getCooldownMinutes() { return cooldownMinutes; }
    public ActionAlertSeverity getSeverity() { return severity; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
