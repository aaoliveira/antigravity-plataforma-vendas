package com.sellerintelligence.modules.pricing.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Parâmetros de entrada para o cálculo determinístico de preço, margem e ponto de equilíbrio.
 */
public record PricingCalculationRequest(
        @NotNull(message = "Preço bruto de venda é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço bruto de venda deve ser maior que zero")
        BigDecimal grossSellingPrice,

        @DecimalMin(value = "0.00", message = "Desconto promocional não pode ser negativo")
        BigDecimal promotionalDiscount,

        @NotNull(message = "Custo de aquisição é obrigatório")
        @DecimalMin(value = "0.00", message = "Custo de aquisição não pode ser negativo")
        BigDecimal acquisitionCost,

        @DecimalMin(value = "0.00", message = "Custo de embalagem não pode ser negativo")
        BigDecimal packagingCost,

        @DecimalMin(value = "0.00", message = "Custos adicionais não podem ser negativos")
        BigDecimal additionalCost,

        @DecimalMin(value = "0.00", message = "Frete subsidiado não pode ser negativo")
        BigDecimal subsidizedShippingCost,

        @DecimalMin(value = "0.00", message = "Comissão do canal não pode ser negativa")
        BigDecimal channelCommissionPercent,

        @DecimalMin(value = "0.00", message = "Tarifa fixa do canal não pode ser negativa")
        BigDecimal channelFixedFee,

        @DecimalMin(value = "0.00", message = "Taxa logística não pode ser negativa")
        BigDecimal logisticsFee,

        @DecimalMin(value = "0.00", message = "Alíquota de imposto não pode ser negativa")
        BigDecimal taxRatePercent,

        @DecimalMin(value = "0.00", message = "Outras despesas variáveis não podem ser negativas")
        BigDecimal otherVariableExpensesPercent,

        @DecimalMin(value = "0.00", message = "Outras deduções fixas não podem ser negativas")
        BigDecimal otherFixedDeductions,

        BigDecimal minMarginPercent,
        BigDecimal targetMarginPercent,
        BigDecimal targetMarginAmount,
        String currency
) {
    public PricingCalculationRequest {
        if (currency == null || currency.isBlank()) {
            currency = "BRL";
        }
    }
}
