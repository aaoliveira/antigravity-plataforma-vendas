package br.com.sellerintelligence.market.provider;

import br.com.sellerintelligence.sync.domain.DataProvenance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MarketProductDetail(
        String externalId,
        String title,
        BigDecimal price,
        String currency,
        String condition,
        String sellerNickname,
        String sellerReputation,
        String permalink,
        List<String> pictures,
        String categoryId,
        String categoryName,
        String brand,
        Map<String, String> technicalAttributes,
        Boolean freeShipping,
        BigDecimal reviewScore,
        Integer reviewCount,
        String estimatedSalesRange,
        String estimationMethod,
        DataProvenance provenance,
        Instant collectedAt
) {}
