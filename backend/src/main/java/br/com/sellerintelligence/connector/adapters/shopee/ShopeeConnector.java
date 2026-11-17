package br.com.sellerintelligence.connector.adapters.shopee;

import br.com.sellerintelligence.connector.model.*;
import br.com.sellerintelligence.connector.ports.MarketplaceConnector;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Component
public class ShopeeConnector implements MarketplaceConnector {

    private static final Logger log = LoggerFactory.getLogger(ShopeeConnector.class);

    private final String partnerId;
    private final String partnerKey;
    private final String apiBaseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ShopeeConnector(
            @Value("${connectors.shopee.partner-id:}") String partnerId,
            @Value("${connectors.shopee.partner-key:}") String partnerKey,
            @Value("${connectors.shopee.api-base-url:https://partner.shopeemobile.com}") String apiBaseUrl,
            ObjectMapper objectMapper) {
        this.partnerId = partnerId;
        this.partnerKey = partnerKey;
        this.apiBaseUrl = apiBaseUrl;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(apiBaseUrl)
                .build();
    }

    @Override
    public Marketplace getMarketplace() {
        return Marketplace.SHOPEE;
    }

    @Override
    public List<ConnectorCapability> getCapabilities() {
        return List.of(
                ConnectorCapability.supported(ConnectorCapabilityType.CATALOG, "Leitura de itens via /api/v2/product/get_item_base_info"),
                ConnectorCapability.supported(ConnectorCapabilityType.LISTINGS, "Sincronização de catálogo e anúncios próprios"),
                ConnectorCapability.supported(ConnectorCapabilityType.ORDERS, "Consulta de pedidos e escrow via /api/v2/payment/get_escrow_detail"),
                ConnectorCapability.unsupported(ConnectorCapabilityType.ADS, "Shopee Ads não suportado no MVP"),
                ConnectorCapability.supported(ConnectorCapabilityType.WEBHOOKS, "Notificações de pedidos via webhook oficial"),
                ConnectorCapability.unsupported(ConnectorCapabilityType.PUBLIC_SEARCH, "Shopee não disponibiliza busca pública de mercado via API aberta"),
                ConnectorCapability.unsupported(ConnectorCapabilityType.PUBLIC_TRENDS, "Sem API de tendências públicas")
        );
    }

    @Override
    public OAuthAuthorizationUrl generateAuthorizationUrl(String state, String redirectUri) {
        if (partnerId == null || partnerId.isBlank() || partnerKey == null || partnerKey.isBlank()) {
            throw new BusinessException(
                    "MISSING_CONNECTOR_CREDENTIALS",
                    "Shopee Partner credentials (ID and Key) are not configured. Real OAuth requires registered partner application.",
                    HttpStatus.BAD_REQUEST
            );
        }

        long timestamp = Instant.now().getEpochSecond();
        String path = "/api/v2/shop/auth_partner";
        String sign = calculateSign(partnerId + path + timestamp, partnerKey);

        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        String url = String.format("%s%s?partner_id=%s&timestamp=%d&sign=%s&redirect=%s",
                apiBaseUrl, path, partnerId, timestamp, sign, encodedRedirect);

        return new OAuthAuthorizationUrl(url, state, OffsetDateTime.now().plusMinutes(5));
    }

    @Override
    public OAuthTokenResult exchangeAuthorizationCode(String code, String redirectUri) {
        validateConfiguredCredentials();

        long timestamp = Instant.now().getEpochSecond();
        String path = "/api/v2/auth/token/get";
        String sign = calculateSign(partnerId + path + timestamp, partnerKey);

        Map<String, Object> body = Map.of(
                "code", code,
                "partner_id", Long.parseLong(partnerId),
                "redirect_uri", redirectUri
        );

        try {
            String response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("partner_id", partnerId)
                            .queryParam("timestamp", timestamp)
                            .queryParam("sign", sign)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            if (node.has("error") && !node.get("error").asText().isBlank()) {
                throw new IllegalStateException("Shopee API error: " + node.get("message").asText());
            }

            JsonNode responseNode = node.get("response");
            String accessToken = responseNode.get("access_token").asText();
            String refreshToken = responseNode.has("refresh_token") ? responseNode.get("refresh_token").asText() : null;
            String shopId = responseNode.has("shop_id") ? responseNode.get("shop_id").asText() : "shopee-shop";
            long expiresIn = responseNode.has("expire_in") ? responseNode.get("expire_in").asLong() : 14400L; // 4 hours

            return new OAuthTokenResult(
                    accessToken,
                    refreshToken,
                    shopId,
                    "default_scope",
                    OffsetDateTime.now().plusSeconds(expiresIn),
                    OffsetDateTime.now().plusDays(30),
                    "PRODUCTION"
            );
        } catch (Exception e) {
            log.error("Failed to exchange Shopee authorization code: {}", e.getMessage());
            throw new BusinessException("SHOPEE_AUTH_FAILED", "Failed to exchange Shopee code: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public OAuthTokenResult refreshAccessToken(String refreshToken) {
        validateConfiguredCredentials();

        long timestamp = Instant.now().getEpochSecond();
        String path = "/api/v2/auth/access_token/get";
        String sign = calculateSign(partnerId + path + timestamp, partnerKey);

        Map<String, Object> body = Map.of(
                "refresh_token", refreshToken,
                "partner_id", Long.parseLong(partnerId)
        );

        try {
            String response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("partner_id", partnerId)
                            .queryParam("timestamp", timestamp)
                            .queryParam("sign", sign)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            JsonNode responseNode = node.get("response");
            String accessToken = responseNode.get("access_token").asText();
            String newRefreshToken = responseNode.has("refresh_token") ? responseNode.get("refresh_token").asText() : refreshToken;
            long expiresIn = responseNode.has("expire_in") ? responseNode.get("expire_in").asLong() : 14400L;

            return new OAuthTokenResult(
                    accessToken,
                    newRefreshToken,
                    "shopee-shop",
                    "default_scope",
                    OffsetDateTime.now().plusSeconds(expiresIn),
                    OffsetDateTime.now().plusDays(30),
                    "PRODUCTION"
            );
        } catch (Exception e) {
            log.error("Failed to refresh Shopee token: {}", e.getMessage());
            throw new BusinessException("SHOPEE_REFRESH_FAILED", "Failed to refresh token: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public void revokeToken(String accessToken, String externalAccountId) {
        log.info("Revoking Shopee connection for shop ID: {}", externalAccountId);
    }

    @Override
    public boolean validateConnection(String accessToken, String externalAccountId) {
        return accessToken != null && !accessToken.isBlank();
    }

    public static String calculateSign(String baseString, String partnerKey) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(partnerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] signedBytes = sha256Hmac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signedBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 sign for Shopee request", e);
        }
    }

    private void validateConfiguredCredentials() {
        if (partnerId == null || partnerId.isBlank() || partnerKey == null || partnerKey.isBlank()) {
            throw new BusinessException(
                    "MISSING_CONNECTOR_CREDENTIALS",
                    "Shopee Partner ID or Partner Key is missing in configuration.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
