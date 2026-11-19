package com.sellerintelligence.modules.listinganalytics.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO para transferência e apresentação pública de métricas de contas e anúncios.
 */
public record AccountMetricDto(
        UUID id,
        UUID tenantId,
        UUID sellerAccountId,
        String marketplace,
        UUID listingId,
        String externalListingId,
        String targetIdentifier,
        MetricScope metricScope,
        MetricGranularity granularity,
        Instant windowStart,
        Instant windowEnd,
        String metricKey,
        BigDecimal metricValue,
        MetricUnit metricUnit,
        MetricProvenance provenance,
        String formulaDefinition,
        String denominatorMetric,
        BigDecimal denominatorValue,
        String numeratorMetric,
        BigDecimal numeratorValue,
        MetricAvailabilityStatus availabilityStatus,
        String unavailabilityReason,
        String sourceEndpoint,
        Instant ingestedAt,
        String metadataJson
) {
}
