package com.sellerintelligence.modules.productmonitoring.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddMonitoredItemRequest(
        @NotBlank(message = "O marketplace é obrigatório.")
        @Size(max = 50)
        String marketplace,

        @NotBlank(message = "A política de fonte é obrigatória.")
        @Size(max = 100)
        String sourcePolicyId,

        @NotBlank(message = "O ID externo do anúncio é obrigatório.")
        @Size(max = 100)
        String externalListingId,

        @NotBlank(message = "O título do anúncio é obrigatório.")
        @Size(max = 255)
        String title,

        @NotNull(message = "O preço inicial é obrigatório.")
        BigDecimal initialPrice,

        String currency,

        String sourceUrl,

        String sellerName,

        String itemCondition,

        Integer monitoringIntervalMinutes
) {
    public AddMonitoredItemRequest {
        if (currency == null || currency.isBlank()) {
            currency = "BRL";
        }
        if (itemCondition == null || itemCondition.isBlank()) {
            itemCondition = "NEW";
        }
        if (monitoringIntervalMinutes == null || monitoringIntervalMinutes < 15) {
            monitoringIntervalMinutes = 60;
        }
    }
}
