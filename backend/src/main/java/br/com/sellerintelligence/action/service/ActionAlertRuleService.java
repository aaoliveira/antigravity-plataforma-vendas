package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class ActionAlertRuleService {

    private static final Logger log = LoggerFactory.getLogger(ActionAlertRuleService.class);

    private final ActionAlertRuleRepository ruleRepository;
    private final ActionAlertRepository alertRepository;

    public ActionAlertRuleService(ActionAlertRuleRepository ruleRepository,
                                  ActionAlertRepository alertRepository) {
        this.ruleRepository = ruleRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional
    public ActionAlertRule createRule(UUID tenantId, String name, AlertRuleType ruleType,
                                      AlertScopeType scopeType, String scopeId,
                                      BigDecimal thresholdValue, ThresholdOperator operator,
                                      int timeWindowMinutes, int cooldownMinutes,
                                      ActionAlertSeverity severity) {
        ActionAlertRule rule = new ActionAlertRule(
                tenantId, name, ruleType, scopeType, scopeId,
                thresholdValue, operator, timeWindowMinutes, cooldownMinutes, severity
        );
        log.info("Created alert rule: id={}, name='{}', type={}, tenantId={}", rule.getId(), name, ruleType, tenantId);
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<ActionAlertRule> getRules(UUID tenantId) {
        return ruleRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public ActionAlertRule getRule(UUID tenantId, UUID ruleId) {
        return ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Regra de alerta não encontrada: " + ruleId));
    }

    @Transactional
    public ActionAlertRule updateRule(UUID tenantId, UUID ruleId, String name,
                                      BigDecimal thresholdValue, ThresholdOperator operator,
                                      int timeWindowMinutes, int cooldownMinutes,
                                      ActionAlertSeverity severity, boolean active) {
        ActionAlertRule rule = getRule(tenantId, ruleId);
        rule.update(name, thresholdValue, operator, timeWindowMinutes, cooldownMinutes, severity, active);
        log.info("Updated alert rule: id={}, active={}", ruleId, active);
        return ruleRepository.save(rule);
    }

    @Transactional
    public RuleSimulationResult simulateRule(UUID tenantId, UUID ruleId, BigDecimal observedValue,
                                             String targetContext, boolean persistAlert) {
        ActionAlertRule rule = getRule(tenantId, ruleId);
        Objects.requireNonNull(observedValue, "observedValue cannot be null for simulation");

        boolean triggered = evaluate(observedValue, rule.getThresholdValue(), rule.getThresholdOperator());

        String explanation;
        if (triggered) {
            explanation = String.format("Simulação DISPAROU: Valor observado (%s) atendeu ao critério %s que o limiar configurado (%s) para a regra '%s'.",
                    observedValue.toPlainString(), rule.getThresholdOperator(), rule.getThresholdValue().toPlainString(), rule.getName());
        } else {
            explanation = String.format("Simulação NÃO disparou: Valor observado (%s) não violou a condição %s (%s) da regra '%s'.",
                    observedValue.toPlainString(), rule.getThresholdOperator(), rule.getThresholdValue().toPlainString(), rule.getName());
        }

        UUID generatedAlertId = null;
        if (triggered && persistAlert) {
            String dedupKey = "sim:" + rule.getId() + ":" + System.currentTimeMillis();
            ActionAlert alert = new ActionAlert(
                    tenantId,
                    rule.getId(),
                    "Simulação: " + rule.getName(),
                    explanation + (targetContext != null ? " Contexto: " + targetContext : ""),
                    rule.getSeverity(),
                    mapRuleTypeToDomain(rule.getRuleType()),
                    rule.getScopeType(),
                    rule.getScopeId(),
                    "CALCULATED",
                    "{\"observedValue\": " + observedValue.toPlainString() + ", \"threshold\": " + rule.getThresholdValue().toPlainString() + ", \"simulation\": true}",
                    dedupKey
            );
            ActionAlert saved = alertRepository.save(alert);
            generatedAlertId = saved.getId();
        }

        return new RuleSimulationResult(
                triggered,
                rule.getId(),
                rule.getName(),
                rule.getRuleType(),
                rule.getThresholdValue(),
                rule.getThresholdOperator(),
                observedValue,
                rule.getSeverity(),
                explanation,
                generatedAlertId
        );
    }

    private boolean evaluate(BigDecimal observed, BigDecimal threshold, ThresholdOperator operator) {
        int cmp = observed.compareTo(threshold);
        return switch (operator) {
            case LESS_THAN -> cmp < 0;
            case GREATER_THAN -> cmp > 0;
            case EQUALS -> cmp == 0;
        };
    }

    private ActionAlertDomain mapRuleTypeToDomain(AlertRuleType type) {
        return switch (type) {
            case MARGIN_THRESHOLD -> ActionAlertDomain.FINANCIAL;
            case PRICE_UNDERCUT -> ActionAlertDomain.LISTING_PRICE;
            case SYNC_FAILURE -> ActionAlertDomain.MARKETPLACE_SYNC;
            case SALES_DROP -> ActionAlertDomain.FINANCIAL;
            case STOCK_DEPLETION -> ActionAlertDomain.INVENTORY;
        };
    }
}
