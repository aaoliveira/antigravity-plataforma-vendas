package br.com.sellerintelligence.market.web;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SaveProductRequest(
        Marketplace marketplace,
        @NotBlank(message = "externalItemId cannot be empty")
        String externalItemId,
        @NotBlank(message = "title cannot be empty")
        String title,
        @NotNull(message = "price cannot be null")
        BigDecimal price,
        String currency,
        String sellerNickname,
        String permalink,
        String thumbnailUrl,
        String notes
) {}
