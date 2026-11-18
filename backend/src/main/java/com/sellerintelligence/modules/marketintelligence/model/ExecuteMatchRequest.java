package com.sellerintelligence.modules.marketintelligence.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * Parâmetros opcionais para personalização do threshold na execução de matching.
 */
public record ExecuteMatchRequest(
        @DecimalMin(value = "0.1000", message = "threshold mínimo é 0.10")
        @DecimalMax(value = "1.0000", message = "threshold máximo é 1.00")
        BigDecimal threshold
) {
}
