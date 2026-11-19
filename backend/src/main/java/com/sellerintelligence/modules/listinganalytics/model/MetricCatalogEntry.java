package com.sellerintelligence.modules.listinganalytics.model;

import java.util.Objects;

/**
 * Entrada formal de uma métrica no catálogo oficial documentado por marketplace.
 */
public record MetricCatalogEntry(
        String metricKey,
        String displayName,
        String description,
        String marketplace,
        MetricScope scope,
        MetricGranularity granularity,
        MetricUnit unit,
        boolean supportedByChannel,
        String sourceEndpoint,
        String rateLimitsAndQuotas,
        Integer maxWindowDays,
        String formulaDefinition,
        String denominatorMetric,
        String unavailabilityReason
) {
    public MetricCatalogEntry {
        Objects.requireNonNull(metricKey, "metricKey é obrigatório");
        Objects.requireNonNull(displayName, "displayName é obrigatório");
        Objects.requireNonNull(marketplace, "marketplace é obrigatório");
        Objects.requireNonNull(scope, "scope é obrigatório");
        Objects.requireNonNull(granularity, "granularity é obrigatória");
        Objects.requireNonNull(unit, "unit é obrigatória");
    }
}
