package com.sellerintelligence.modules.competitorresearch.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Parâmetros de requisição para busca de produtos e anúncios em canais de mercado ou catálogo próprio.
 */
public record MarketSearchRequest(
        @NotBlank(message = "O termo de busca 'q' é obrigatório")
        @Size(min = 2, max = 255, message = "O termo de busca deve conter entre 2 e 255 caracteres")
        String q,

        String marketplace,

        String category,

        BigDecimal minPrice,

        BigDecimal maxPrice,

        Boolean onlyOwnCatalog,

        Integer page,

        Integer size
) {
    public MarketSearchRequest {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size < 1) {
            size = 20;
        } else if (size > 100) {
            size = 100;
        }
        if (onlyOwnCatalog == null) {
            onlyOwnCatalog = false;
        }
    }
}
