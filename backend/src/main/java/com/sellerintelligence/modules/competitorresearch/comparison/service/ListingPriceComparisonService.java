package com.sellerintelligence.modules.competitorresearch.comparison.service;

import com.sellerintelligence.modules.competitorresearch.comparison.model.ComparableListingItem;
import com.sellerintelligence.modules.competitorresearch.comparison.model.PriceComparisonResult;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ShippingStatus;
import com.sellerintelligence.modules.competitorresearch.comparison.model.TemporalComparabilityStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Serviço de comparação determinística de preços.
 * Cumpre as regras:
 * 1. "Calcule variação de preço apenas entre observações com janela temporal comparável"
 * 2. "Não inferir preço final com frete não conhecido"
 * 3. Cálculos monetários e percentuais estritamente em BigDecimal.
 */
@Service
public class ListingPriceComparisonService {

    public PriceComparisonResult comparePrices(
            ComparableListingItem base,
            ComparableListingItem target,
            int maxComparableDays
    ) {
        Objects.requireNonNull(base, "Item base é obrigatório");
        Objects.requireNonNull(target, "Item alvo é obrigatório");
        Objects.requireNonNull(base.price(), "Preço do item base é obrigatório");
        Objects.requireNonNull(target.price(), "Preço do item alvo é obrigatório");

        Instant baseTime = base.observedAt() != null ? base.observedAt() : Instant.now();
        Instant targetTime = target.observedAt() != null ? target.observedAt() : Instant.now();

        long deltaSeconds = Math.abs(Duration.between(baseTime, targetTime).getSeconds());
        long deltaDays = deltaSeconds / 86400;

        boolean isTemporalComparable = deltaDays <= maxComparableDays;
        BigDecimal priceDiff = null;
        BigDecimal priceVarPct = null;
        TemporalComparabilityStatus temporalStatus;
        String temporalWarning = null;

        if (isTemporalComparable) {
            temporalStatus = TemporalComparabilityStatus.COMPARABLE;
            priceDiff = target.price().subtract(base.price()).setScale(4, RoundingMode.HALF_UP);
            if (base.price().compareTo(BigDecimal.ZERO) > 0) {
                priceVarPct = target.price().subtract(base.price())
                        .divide(base.price(), 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(4, RoundingMode.HALF_UP);
            }
        } else {
            temporalStatus = TemporalComparabilityStatus.NON_COMPARABLE;
            temporalWarning = String.format(
                    "Variação de preço não calculada: a diferença temporal entre as observações (%d dias) excede a janela máxima comparável (%d dias). Comparar momentos temporais díspares violaria a regra de comparabilidade.",
                    deltaDays, maxComparableDays
            );
        }

        // Tratamento de Frete (Risco Zero de Inferência Indevida)
        ShippingStatus baseShippingStatus = base.shippingCost() != null ? ShippingStatus.KNOWN : ShippingStatus.UNKNOWN;
        ShippingStatus targetShippingStatus = target.shippingCost() != null ? ShippingStatus.KNOWN : ShippingStatus.UNKNOWN;

        BigDecimal baseFinalPriceWithShipping = null;
        if (baseShippingStatus == ShippingStatus.KNOWN) {
            baseFinalPriceWithShipping = base.price().add(base.shippingCost()).setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal targetFinalPriceWithShipping = null;
        if (targetShippingStatus == ShippingStatus.KNOWN) {
            targetFinalPriceWithShipping = target.price().add(target.shippingCost()).setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal totalPriceDiffWithShipping = null;
        BigDecimal totalPriceVarPctWithShipping = null;
        String shippingNotice = null;

        if (baseShippingStatus == ShippingStatus.KNOWN && targetShippingStatus == ShippingStatus.KNOWN) {
            if (isTemporalComparable) {
                totalPriceDiffWithShipping = targetFinalPriceWithShipping.subtract(baseFinalPriceWithShipping).setScale(4, RoundingMode.HALF_UP);
                if (baseFinalPriceWithShipping.compareTo(BigDecimal.ZERO) > 0) {
                    totalPriceVarPctWithShipping = targetFinalPriceWithShipping.subtract(baseFinalPriceWithShipping)
                            .divide(baseFinalPriceWithShipping, 6, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"))
                            .setScale(4, RoundingMode.HALF_UP);
                }
            }
        } else {
            shippingNotice = "Custo de frete não informado ou indisponível em um ou ambos os anúncios (UNKNOWN). O preço final com frete NÃO foi inferido.";
        }

        return new PriceComparisonResult(
                base.price(),
                target.price(),
                priceDiff,
                priceVarPct,
                isTemporalComparable,
                deltaDays,
                temporalStatus,
                temporalWarning,
                base.shippingCost(),
                target.shippingCost(),
                baseShippingStatus,
                targetShippingStatus,
                baseFinalPriceWithShipping,
                targetFinalPriceWithShipping,
                totalPriceDiffWithShipping,
                totalPriceVarPctWithShipping,
                shippingNotice
        );
    }
}
