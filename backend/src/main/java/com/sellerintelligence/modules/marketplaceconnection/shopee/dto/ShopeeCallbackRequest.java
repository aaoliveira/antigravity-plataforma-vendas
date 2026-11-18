package com.sellerintelligence.modules.marketplaceconnection.shopee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShopeeCallbackRequest(
        @NotBlank(message = "code é obrigatório no callback da Shopee")
        String code,

        @NotNull(message = "shop_id é obrigatório no callback da Shopee")
        Long shopId,

        String redirectUrl
) {
}
