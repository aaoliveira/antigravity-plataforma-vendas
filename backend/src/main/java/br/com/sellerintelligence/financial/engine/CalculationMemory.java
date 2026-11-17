package br.com.sellerintelligence.financial.engine;

import br.com.sellerintelligence.financial.domain.CalculationQuality;

import java.util.List;

public record CalculationMemory(
        CalculationQuality quality,
        List<CalculationMemoryLine> lines,
        List<String> missingFields,
        String assumptions,
        String summary
) {}
