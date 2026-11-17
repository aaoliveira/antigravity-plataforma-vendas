package br.com.sellerintelligence.connector.adapters.mercadolivre;

import br.com.sellerintelligence.connector.model.*;
import br.com.sellerintelligence.connector.ports.MarketplaceConnector;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class MercadoLivreConnector implements MarketplaceConnector {

    private static final Logger log = LoggerFactory.getLogger(MercadoLivreConnector.class);

    private final String appId;
    private final String clientSecret;
    private final String authBaseUrl;
    private final String apiBaseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MercadoLivreConnector(
            @Value("${connectors.mercadolivre.app-id:}") String appId,
            @Value("${connectors.mercadolivre.client-secret:}") String clientSecret,
            @Value("${connectors.mercadolivre.auth-base-url:https://auth.mercadolivre.com.br}") String authBaseUrl,
            @Value("${connectors.mercadolivre.api-base-url:https://api.mercadolibre.com}") String apiBaseUrl,
            ObjectMapper objectMapper) {
        this.appId = appId;
        this.clientSecret = clientSecret;
        this.authBaseUrl = authBaseUrl;
        this.apiBaseUrl = apiBaseUrl;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(apiBaseUrl)
                .build();
    }

    @Override
    public Marketplace getMarketplace() {
        return Marketplace.MERCADO_LIVRE;
    }

    @Override
    public List<ConnectorCapability> getCapabilities() {
        return List.of(
                ConnectorCapability.supported(ConnectorCapabilityType.CATALOG, "Leitura de itens próprios via /users/{user_id}/items/search e /items/{id}"),
                ConnectorCapability.supported(ConnectorCapabilityType.LISTINGS, "Publicação e alteração de anúncios via /items"),
                ConnectorCapability.supported(ConnectorCapabilityType.ORDERS, "Consulta de pedidos próprios via /orders/search e /orders/{id}"),
                ConnectorCapability.pendingValidation(ConnectorCapabilityType.ADS, "Mercado Ads requer aprovação de escopo especial advertising/product_ads", true),
                ConnectorCapability.supported(ConnectorCapabilityType.WEBHOOKS, "Notificações em tempo real para orders_v2 e items"),
                ConnectorCapability.supported(ConnectorCapabilityType.PUBLIC_SEARCH, "Pesquisa pública de mercado via /sites/MLB/search"),
                ConnectorCapability.supported(ConnectorCapabilityType.PUBLIC_TRENDS, "Tendências de busca agregadas via /trends/MLB/{category_id}")
        );
    }

    @Override
    public OAuthAuthorizationUrl generateAuthorizationUrl(String state, String redirectUri) {
        if (appId == null || appId.isBlank()) {
            throw new BusinessException(
                    "MISSING_CONNECTOR_CREDENTIALS",
                    "Mercado Livre App ID is not configured. Real OAuth requires registered application credentials.",
                    HttpStatus.BAD_REQUEST
            );
        }

        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);

        String url = String.format("%s/authorization?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
                authBaseUrl, appId, encodedRedirect, encodedState);

        return new OAuthAuthorizationUrl(url, state, OffsetDateTime.now().plusMinutes(15));
    }

    @Override
    public OAuthTokenResult exchangeAuthorizationCode(String code, String redirectUri) {
        validateConfiguredCredentials();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", appId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        try {
            String response = restClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            String accessToken = node.get("access_token").asText();
            String refreshToken = node.has("refresh_token") ? node.get("refresh_token").asText() : null;
            String userId = node.get("user_id").asText();
            long expiresIn = node.has("expires_in") ? node.get("expires_in").asLong() : 21600L; // 6 hours default

            OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);
            OffsetDateTime refreshExpiresAt = OffsetDateTime.now().plusMonths(6);

            log.info("Successfully exchanged Mercado Livre OAuth code for seller ID: {}", userId);

            return new OAuthTokenResult(
                    accessToken,
                    refreshToken,
                    userId,
                    "offline_access,read,write",
                    expiresAt,
                    refreshExpiresAt,
                    "PRODUCTION"
            );
        } catch (Exception e) {
            log.error("Failed to exchange Mercado Livre authorization code: {}", e.getMessage());
            throw new BusinessException("MERCADO_LIVRE_AUTH_FAILED", "Failed to exchange authorization code: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public OAuthTokenResult refreshAccessToken(String refreshToken) {
        validateConfiguredCredentials();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", appId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        try {
            String response = restClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            String newAccessToken = node.get("access_token").asText();
            String newRefreshToken = node.has("refresh_token") ? node.get("refresh_token").asText() : refreshToken;
            String userId = node.get("user_id").asText();
            long expiresIn = node.has("expires_in") ? node.get("expires_in").asLong() : 21600L;

            return new OAuthTokenResult(
                    newAccessToken,
                    newRefreshToken,
                    userId,
                    "offline_access,read,write",
                    OffsetDateTime.now().plusSeconds(expiresIn),
                    OffsetDateTime.now().plusMonths(6),
                    "PRODUCTION"
            );
        } catch (Exception e) {
            log.error("Failed to refresh Mercado Livre token: {}", e.getMessage());
            throw new BusinessException("MERCADO_LIVRE_REFRESH_FAILED", "Failed to refresh token: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public void revokeToken(String accessToken, String externalAccountId) {
        log.info("Revoking Mercado Livre token for seller ID: {}", externalAccountId);
        // Mercado Livre handles revocation through application unlinking or token expiration
    }

    @Override
    public boolean validateConnection(String accessToken, String externalAccountId) {
        try {
            restClient.get()
                    .uri("/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Mercado Livre connection validation failed for seller {}: {}", externalAccountId, e.getMessage());
            return false;
        }
    }

    private void validateConfiguredCredentials() {
        if (appId == null || appId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new BusinessException(
                    "MISSING_CONNECTOR_CREDENTIALS",
                    "Mercado Livre App credentials (App ID and Secret) are missing in configuration.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
