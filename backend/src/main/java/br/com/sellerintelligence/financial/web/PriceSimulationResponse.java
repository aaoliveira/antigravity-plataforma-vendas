package br.com.sellerintelligence.financial.web;

import br.com.sellerintelligence.financial.domain.CalculationQuality;
import br.com.sellerintelligence.financial.engine.CalculationMemory;
import br.com.sellerintelligence.financial.engine.PriceSimulationResult;

import java.math.BigDecimal;
import java.util.List;

public record PriceSimulationResponse(
        BigDecimal minimumPrice,
        BigDecimal targetPrice,
        BigDecimal simulatedPrice,
        BigDecimal expectedMarginValue,
        BigDecimal expectedMarginPercentage,
        CalculationQuality quality,
        List<String> missingFields,
        String assumptions,
        CalculationMemory memory
) {
    public static PriceSimulationResponse fromResult(PriceSimulationResult res) {
        return new PriceSimulationResponse(
                res.minimumPrice(),
                res.targetPrice(),
                res.simulatedPrice(),
                res.expectedMarginValue(),
                res.expectedMarginPercentage(),
                res.quality(),
                res.missingFields(),
                res.assumptions(),
                res.memory()
        );
    }
}
