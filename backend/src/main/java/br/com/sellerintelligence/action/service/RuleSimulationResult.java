package br.com.sellerintelligence.action.service;

import br.com.sellerintelligence.action.domain.ActionAlertSeverity;
import br.com.sellerintelligence.action.domain.AlertRuleType;
import br.com.sellerintelligence.action.domain.ThresholdOperator;

import java.math.BigDecimal;
import java.util.UUID;

public record RuleSimulationResult(
        boolean triggered,
        UUID ruleId,
        String ruleName,
        AlertRuleType ruleType,
        BigDecimal thresholdValue,
        ThresholdOperator operator,
        BigDecimal observedValue,
        ActionAlertSeverity severity,
        String explanation,
        UUID generatedAlertId
) {}
