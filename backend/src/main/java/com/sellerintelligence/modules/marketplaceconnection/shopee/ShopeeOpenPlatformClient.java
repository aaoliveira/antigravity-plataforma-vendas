package com.sellerintelligence.modules.marketplaceconnection.shopee;

/**
 * Contrato de cliente HTTP para operações oficiais da Shopee Open Platform v2.
 */
public interface ShopeeOpenPlatformClient {

    /**
     * Monta a URL oficial de autorização da Shopee (/api/v2/shop/auth_partner) com assinatura HMAC-SHA256.
     */
    String buildAuthorizationUrl(long partnerId, String partnerKey, String redirectUrl, String authBaseUrl);

    /**
     * Troca o authorization code temporário por tokens definitivos em /api/v2/auth/token/get.
     */
    ShopeeTokenResponse getAccessToken(String code, long shopId, long partnerId, String partnerKey, String apiBaseUrl);

    /**
     * Renova o access_token usando o refresh_token de uso único em /api/v2/auth/access_token/get.
     */
    ShopeeTokenResponse refreshAccessToken(String refreshToken, long shopId, long partnerId, String partnerKey, String apiBaseUrl);
}
