package com.sellerintelligence.modules.competitorresearch.comparison.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Resposta de histórico de preços de um anúncio.
 * Cumpre a regra de ouro: "histórico sem retroatividade inventada".
 * Apenas pontos reais persistidos são devolvidos; nunca interpola ou forja histórico passado.
 */
public record ListingPriceHistoryResponse(
        String marketplace,
        String externalListingId,
        String title,
        int dataPointsCount,
        boolean hasHistoricalTrend,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal latestPrice,
        BigDecimal earliestPrice,
        BigDecimal overallVariationPercentage,
        Long timeWindowDays,
        String trendDescription,
        String historyNotice,
        List<ListingPricePointDto> points
) {
    public ListingPriceHistoryResponse {
        if (points == null) {
            points = Collections.emptyList();
        }
    }
}
