package br.com.sellerintelligence.action.web;

import br.com.sellerintelligence.action.domain.ActionAlertSeverity;
import br.com.sellerintelligence.action.domain.AlertRuleType;
import br.com.sellerintelligence.action.domain.AlertScopeType;
import br.com.sellerintelligence.action.domain.ThresholdOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAlertRuleRequest(
        @NotBlank(message = "Nome da regra é obrigatório")
        String name,

        @NotNull(message = "Tipo de regra é obrigatório")
        AlertRuleType ruleType,

        AlertScopeType scopeType,
        String scopeId,

        @NotNull(message = "Valor de limiar é obrigatório")
        BigDecimal thresholdValue,

        ThresholdOperator thresholdOperator,
        int timeWindowMinutes,
        int cooldownMinutes,
        ActionAlertSeverity severity
) {}
