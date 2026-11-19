package com.sellerintelligence.modules.aiassistant.model;

import java.math.BigDecimal;

/**
 * Métricas de governança operacional e financeira de IA.
 */
public record AiUsageMetricsDto(
        int promptTokens,
        int completionTokens,
        int totalTokens,
        long latencyMs,
        BigDecimal estimatedCostBrl
) {
}
