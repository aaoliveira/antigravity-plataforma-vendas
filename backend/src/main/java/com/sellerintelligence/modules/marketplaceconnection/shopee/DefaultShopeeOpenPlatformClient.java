package com.sellerintelligence.modules.marketplaceconnection.shopee;

import com.sellerintelligence.modules.marketplaceconnection.exception.ShopeeOAuthException;
import com.sellerintelligence.modules.marketplaceconnection.exception.ShopeeRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Implementação cliente HTTP oficial para Shopee Open Platform v2 utilizando Spring RestClient.
 */
@Component
public class DefaultShopeeOpenPlatformClient implements ShopeeOpenPlatformClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultShopeeOpenPlatformClient.class);

    private static final String AUTH_PARTNER_PATH = "/api/v2/shop/auth_partner";
    private static final String TOKEN_GET_PATH = "/api/v2/auth/token/get";
    private static final String ACCESS_TOKEN_GET_PATH = "/api/v2/auth/access_token/get";

    private final RestClient restClient;

    public DefaultShopeeOpenPlatformClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String buildAuthorizationUrl(long partnerId, String partnerKey, String redirectUrl, String authBaseUrl) {
        long timestamp = Instant.now().getEpochSecond();
        String baseString = ShopeeSignatureUtil.buildPublicBaseString(partnerId, AUTH_PARTNER_PATH, timestamp);
        String sign = ShopeeSignatureUtil.calculateHmacSha256(baseString, partnerKey);
        String encodedRedirect = URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);

        String cleanBaseUrl = authBaseUrl.replaceAll("/+$", "");
        return String.format("%s%s?partner_id=%d&timestamp=%d&sign=%s&redirect=%s",
                cleanBaseUrl, AUTH_PARTNER_PATH, partnerId, timestamp, sign, encodedRedirect);
    }

    @Override
    public ShopeeTokenResponse getAccessToken(String code, long shopId, long partnerId, String partnerKey, String apiBaseUrl) {
        long timestamp = Instant.now().getEpochSecond();
        String baseString = ShopeeSignatureUtil.buildPublicBaseString(partnerId, TOKEN_GET_PATH, timestamp);
        String sign = ShopeeSignatureUtil.calculateHmacSha256(baseString, partnerKey);

        String cleanBaseUrl = apiBaseUrl.replaceAll("/+$", "");
        String url = String.format("%s%s?partner_id=%d&timestamp=%d&sign=%s",
                cleanBaseUrl, TOKEN_GET_PATH, partnerId, timestamp, sign);

        Map<String, Object> body = Map.of(
                "code", code,
                "partner_id", partnerId,
                "shop_id", shopId
        );

        try {
            ShopeeTokenResponse response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ShopeeTokenResponse.class);

            validateTokenResponse(response);
            return response;
        } catch (ShopeeRateLimitException | ShopeeOAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro de comunicação ao trocar authorization code na Shopee: {}", e.getMessage());
            throw new ShopeeOAuthException("network_error", "Instabilidade na comunicação com a Shopee Open Platform: " + e.getMessage(), e);
        }
    }

    @Override
    public ShopeeTokenResponse refreshAccessToken(String refreshToken, long shopId, long partnerId, String partnerKey, String apiBaseUrl) {
        long timestamp = Instant.now().getEpochSecond();
        String baseString = ShopeeSignatureUtil.buildPublicBaseString(partnerId, ACCESS_TOKEN_GET_PATH, timestamp);
        String sign = ShopeeSignatureUtil.calculateHmacSha256(baseString, partnerKey);

        String cleanBaseUrl = apiBaseUrl.replaceAll("/+$", "");
        String url = String.format("%s%s?partner_id=%d&timestamp=%d&sign=%s",
                cleanBaseUrl, ACCESS_TOKEN_GET_PATH, partnerId, timestamp, sign);

        Map<String, Object> body = Map.of(
                "refresh_token", refreshToken,
                "partner_id", partnerId,
                "shop_id", shopId
        );

        try {
            ShopeeTokenResponse response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ShopeeTokenResponse.class);

            validateTokenResponse(response);
            return response;
        } catch (ShopeeRateLimitException | ShopeeOAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro de comunicação ao renovar access token na Shopee: {}", e.getMessage());
            throw new ShopeeOAuthException("network_error", "Instabilidade na comunicação ao renovar token na Shopee: " + e.getMessage(), e);
        }
    }

    private void validateTokenResponse(ShopeeTokenResponse response) {
        if (response == null) {
            throw new ShopeeOAuthException("empty_response", "Resposta nula recebida da Shopee Open Platform");
        }

        if (response.error() != null && !response.error().isBlank()) {
            String error = response.error();
            String message = response.message() != null ? response.message() : error;

            if ("error_rate_limit".equalsIgnoreCase(error) || "error_limit".equalsIgnoreCase(error)) {
                log.warn("Shopee retornou restrição de cota/taxa: [{}] {}", error, message);
                throw new ShopeeRateLimitException(error, message, 60);
            }

            log.error("Shopee retornou erro no token endpoint: [{}] {}", error, message);
            throw new ShopeeOAuthException(error, message);
        }

        if (!response.isSuccess()) {
            throw new ShopeeOAuthException("invalid_token_payload", "Resposta de token incompleta retornada pela Shopee");
        }
    }
}
