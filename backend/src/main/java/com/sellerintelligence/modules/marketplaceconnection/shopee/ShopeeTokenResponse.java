package com.sellerintelligence.modules.marketplaceconnection.shopee;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta oficial dos endpoints de autenticação e refresh da Shopee Open Platform v2.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShopeeTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expire_in") Long expireIn,
        @JsonProperty("request_id") String requestId,
        @JsonProperty("error") String error,
        @JsonProperty("message") String message,
        @JsonProperty("shop_id") Long shopId
) {

    public boolean isSuccess() {
        return (error == null || error.isBlank()) && accessToken != null && !accessToken.isBlank();
    }
}
