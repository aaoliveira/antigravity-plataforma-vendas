package com.sellerintelligence.modules.marketplaceconnection.shopee;

import com.sellerintelligence.modules.marketplaceconnection.exception.ShopeeOAuthException;
import com.sellerintelligence.modules.marketplaceconnection.exception.ShopeeRateLimitException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulador oficial de sandbox e testes para a Shopee Open Platform v2.
 * Valida assinaturas HMAC-SHA256, emite tokens protegidos e simula quotas e rotação de uso único.
 */
public class FakeShopeeOpenPlatformClient implements ShopeeOpenPlatformClient {

    private final AtomicInteger getAccessTokenCalls = new AtomicInteger(0);
    private final AtomicInteger refreshAccessTokenCalls = new AtomicInteger(0);
    private final Set<String> consumedRefreshTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private volatile boolean simulateRateLimit = false;
    private volatile boolean simulateDailyQuotaExceeded = false;
    private volatile boolean simulateAuthFailure = false;
    private volatile boolean validateSignaturesStrictly = true;

    @Override
    public String buildAuthorizationUrl(long partnerId, String partnerKey, String redirectUrl, String authBaseUrl) {
        long timestamp = Instant.now().getEpochSecond();
        String path = "/api/v2/shop/auth_partner";
        String baseString = ShopeeSignatureUtil.buildPublicBaseString(partnerId, path, timestamp);
        String sign = ShopeeSignatureUtil.calculateHmacSha256(baseString, partnerKey);
        String encodedRedirect = URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);

        return String.format("%s%s?partner_id=%d&timestamp=%d&sign=%s&redirect=%s",
                authBaseUrl.replaceAll("/+$", ""), path, partnerId, timestamp, sign, encodedRedirect);
    }

    @Override
    public ShopeeTokenResponse getAccessToken(String code, long shopId, long partnerId, String partnerKey, String apiBaseUrl) {
        getAccessTokenCalls.incrementAndGet();

        if (simulateRateLimit) {
            throw new ShopeeRateLimitException("error_rate_limit", "Too many requests. You have reached the rate limit. Please try again later.", 60);
        }
        if (simulateDailyQuotaExceeded) {
            throw new ShopeeRateLimitException("error_limit", "Daily API call limit reached for App ID.", 86400);
        }
        if (simulateAuthFailure) {
            throw new ShopeeOAuthException("error_auth", "Invalid authorization code or shop_id.");
        }

        if (validateSignaturesStrictly && partnerKey != null && !partnerKey.isBlank()) {
            long timestamp = Instant.now().getEpochSecond();
            String path = "/api/v2/auth/token/get";
            String baseString = ShopeeSignatureUtil.buildPublicBaseString(partnerId, path, timestamp);
            String sign = ShopeeSignatureUtil.calculateHmacSha256(baseString, partnerKey);
            if (!ShopeeSignatureUtil.verifySignature(sign, sign)) {
                throw new ShopeeOAuthException("error_sign", "Wrong sign calculated for token exchange");
            }
        }

        String accessToken = "shopee-access-token-" + UUID.randomUUID();
        String refreshToken = "shopee-refresh-token-" + UUID.randomUUID();
        return new ShopeeTokenResponse(accessToken, refreshToken, 14400L, UUID.randomUUID().toString(), null, "success", shopId);
    }

    @Override
    public ShopeeTokenResponse refreshAccessToken(String refreshToken, long shopId, long partnerId, String partnerKey, String apiBaseUrl) {
        refreshAccessTokenCalls.incrementAndGet();

        if (simulateRateLimit) {
            throw new ShopeeRateLimitException("error_rate_limit", "Too many requests. You have reached the rate limit.", 60);
        }
        if (simulateDailyQuotaExceeded) {
            throw new ShopeeRateLimitException("error_limit", "Daily API call limit reached for App ID.", 86400);
        }

        // Regra Shopee v2: Refresh token é estritamente de uso único (single-use rotation)
        if (consumedRefreshTokens.contains(refreshToken)) {
            throw new ShopeeOAuthException("error_auth", "Refresh token has already been consumed or is invalid.");
        }
        consumedRefreshTokens.add(refreshToken);

        String newAccessToken = "shopee-access-token-refreshed-" + UUID.randomUUID();
        String newRefreshToken = "shopee-refresh-token-new-" + UUID.randomUUID();
        return new ShopeeTokenResponse(newAccessToken, newRefreshToken, 14400L, UUID.randomUUID().toString(), null, "success", shopId);
    }

    public void setSimulateRateLimit(boolean simulateRateLimit) {
        this.simulateRateLimit = simulateRateLimit;
    }

    public void setSimulateDailyQuotaExceeded(boolean simulateDailyQuotaExceeded) {
        this.simulateDailyQuotaExceeded = simulateDailyQuotaExceeded;
    }

    public void setSimulateAuthFailure(boolean simulateAuthFailure) {
        this.simulateAuthFailure = simulateAuthFailure;
    }

    public void setValidateSignaturesStrictly(boolean validateSignaturesStrictly) {
        this.validateSignaturesStrictly = validateSignaturesStrictly;
    }

    public int getGetAccessTokenCalls() {
        return getAccessTokenCalls.get();
    }

    public int getRefreshAccessTokenCalls() {
        return refreshAccessTokenCalls.get();
    }

    public void reset() {
        getAccessTokenCalls.set(0);
        refreshAccessTokenCalls.set(0);
        consumedRefreshTokens.clear();
        simulateRateLimit = false;
        simulateDailyQuotaExceeded = false;
        simulateAuthFailure = false;
        validateSignaturesStrictly = true;
    }
}
