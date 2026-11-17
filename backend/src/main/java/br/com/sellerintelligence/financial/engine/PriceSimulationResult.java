package br.com.sellerintelligence.financial.engine;

import br.com.sellerintelligence.financial.domain.CalculationQuality;

import java.math.BigDecimal;
import java.util.List;

public record PriceSimulationResult(
        BigDecimal minimumPrice,
        BigDecimal targetPrice,
        BigDecimal simulatedPrice,
        BigDecimal expectedMarginValue,
        BigDecimal expectedMarginPercentage,
        CalculationQuality quality,
        List<String> missingFields,
        String assumptions,
        CalculationMemory memory
) {}
