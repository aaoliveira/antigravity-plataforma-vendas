package br.com.sellerintelligence.reconciliation.web;

import br.com.sellerintelligence.reconciliation.domain.DisputeChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LogDisputeRequest(
        @NotBlank(message = "protocolNumber é obrigatório")
        String protocolNumber,
        @NotBlank(message = "marketplace é obrigatório")
        String marketplace,
        @NotNull(message = "disputedAmount é obrigatório")
        BigDecimal disputedAmount,
        DisputeChannel channel,
        String notes
) {}
