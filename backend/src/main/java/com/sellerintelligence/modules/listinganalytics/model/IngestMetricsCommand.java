package com.sellerintelligence.modules.listinganalytics.model;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Comando de entrada para disparar a ingestão e derivação de métricas oficiais de uma conta.
 */
public record IngestMetricsCommand(
        @NotNull(message = "sellerAccountId é obrigatório")
        UUID sellerAccountId,

        @NotNull(message = "marketplace é obrigatório")
        String marketplace,

        String externalListingId,

        @NotNull(message = "metricScope é obrigatório")
        MetricScope metricScope,

        @NotNull(message = "granularity é obrigatória")
        MetricGranularity granularity,

        @NotNull(message = "windowStart é obrigatório")
        Instant windowStart,

        @NotNull(message = "windowEnd é obrigatório")
        Instant windowEnd,

        Map<String, BigDecimal> metrics,

        String sourceEndpoint
) {
}
