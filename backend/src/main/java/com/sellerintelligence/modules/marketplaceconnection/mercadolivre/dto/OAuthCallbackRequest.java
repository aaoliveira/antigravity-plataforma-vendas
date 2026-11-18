package com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO com dados do callback de autorização recebidos do navegador/Mercado Livre.
 */
public record OAuthCallbackRequest(
        @NotBlank(message = "O código de autorização 'code' é obrigatório")
        String code,

        @NotBlank(message = "O parâmetro 'state' anti-CSRF é obrigatório")
        String state,

        String redirectUri
) {
}
