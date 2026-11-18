package com.sellerintelligence.modules.marketplaceconnection.shopee.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShopeeWebhookPayload(
        @JsonProperty("code") Integer code,
        @JsonProperty("shop_id") Long shopId,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("data") Map<String, Object> data
) {

    public static final int CODE_SHOP_AUTH_CANCELED = 2;
    public static final int CODE_SHOP_AUTH_GRANTED = 1;

    public boolean isAuthorizationCanceled() {
        return code != null && code == CODE_SHOP_AUTH_CANCELED;
    }
}
