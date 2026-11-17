package br.com.sellerintelligence.monitoring.web;

import br.com.sellerintelligence.monitoring.domain.AlertChannel;
import br.com.sellerintelligence.monitoring.domain.AlertDirection;
import br.com.sellerintelligence.monitoring.domain.MonitoringEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAlertRuleRequest(
        UUID watchlistId,

        @NotBlank(message = "O nome da regra de alerta é obrigatório")
        @Size(max = 255)
        String name,

        MonitoringEventType eventType,

        BigDecimal minPriceDeltaPercentage,

        AlertDirection direction,

        Integer cooldownMinutes,

        AlertChannel channel
) {}
