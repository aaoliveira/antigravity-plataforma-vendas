package com.sellerintelligence.modules.competitorresearch.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resultado normalizado de pesquisa de produto ou anúncio com rastreabilidade completa e metadados de proveniência.
 * Segue a regra estrita de não fabricação de métricas não disponíveis oficialmente.
 */
public record NormalizedMarketSearchResult(
        String itemId,
        String title,
        String marketplace,
        BigDecimal price,
        String currency,
        String sellerName,
        String sellerClassification,
        String listingUrl,
        String gtin,
        String itemCondition,
        Integer availableQuantity,
        String sourcePolicyId,
        String provenanceType,
        Instant collectedAt,
        String salesMetricNotice
) {
}
