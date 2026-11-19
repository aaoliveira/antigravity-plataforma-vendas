package com.sellerintelligence.modules.listinganalytics.model;

import java.math.BigDecimal;

/**
 * Resultado da apuração determinística de uma métrica calculada.
 */
public record MetricCalculationResult(
        String metricKey,
        BigDecimal value,
        MetricUnit unit,
        MetricProvenance provenance,
        MetricAvailabilityStatus status,
        String unavailabilityReason,
        String formulaDefinition,
        String numeratorMetric,
        BigDecimal numeratorValue,
        String denominatorMetric,
        BigDecimal denominatorValue
) {
}
