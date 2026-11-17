package br.com.sellerintelligence.connector.adapters.amazon;

import br.com.sellerintelligence.connector.model.*;
import br.com.sellerintelligence.connector.ports.MarketplaceConnector;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Official Amazon SP-API (Selling Partner API) connector adapter.
 * Handles LWA OAuth 2.0, Catalog/Listings, Orders v0, and Health checks for Amazon Brazil (A2Q3Y263D00KWC).
 */
@Component
public class AmazonSpApiConnector implements MarketplaceConnector {

    private static final Logger log = LoggerFactory.getLogger(AmazonSpApiConnector.class);

    public static final String DEFAULT_MARKETPLACE_ID_BR = "A2Q3Y263D00KWC";
    public static final String DEFAULT_LWA_AUTH_URL = "https://api.amazon.com/auth/o2/token";
    public static final String DEFAULT_SELLER_CENTRAL_URL = "https://sellercentral.amazon.com.br/apps/authorize/consent";
    public static final String DEFAULT_SP_API_BASE_URL = "https://sellingpartnerapi-na.amazon.com";

    private final String appId;
    private final String clientSecret;
    private final String lwaAuthUrl;
    private final String sellerCentralAuthUrl;
    private final String spApiBaseUrl;
    private final String marketplaceId;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public AmazonSpApiConnector(
            @Value("${connectors.amazon.app-id:}") String appId,
            @Value("${connectors.amazon.client-secret:}") String clientSecret,
            @Value("${connectors.amazon.lwa-auth-url:" + DEFAULT_LWA_AUTH_URL + "}") String lwaAuthUrl,
            @Value("${connectors.amazon.seller-central-auth-url:" + DEFAULT_SELLER_CENTRAL_URL + "}") String sellerCentralAuthUrl,
            @Value("${connectors.amazon.sp-api-base-url:" + DEFAULT_SP_API_BASE_URL + "}") String spApiBaseUrl,
            @Value("${connectors.amazon.marketplace-id:" + DEFAULT_MARKETPLACE_ID_BR + "}") String marketplaceId,
            ObjectMapper objectMapper) {
        this(appId, clientSecret, lwaAuthUrl, sellerCentralAuthUrl, spApiBaseUrl, marketplaceId, objectMapper,
                RestClient.builder().baseUrl(spApiBaseUrl).build());
    }

    public AmazonSpApiConnector(
            String appId,
            String clientSecret,
            String lwaAuthUrl,
            String sellerCentralAuthUrl,
            String spApiBaseUrl,
            String marketplaceId,
            ObjectMapper objectMapper,
            RestClient restClient) {
        this.appId = appId;
        this.clientSecret = clientSecret;
        this.lwaAuthUrl = (lwaAuthUrl != null && !lwaAuthUrl.isBlank()) ? lwaAuthUrl : DEFAULT_LWA_AUTH_URL;
        this.sellerCentralAuthUrl = (sellerCentralAuthUrl != null && !sellerCentralAuthUrl.isBlank()) ? sellerCentralAuthUrl : DEFAULT_SELLER_CENTRAL_URL;
        this.spApiBaseUrl = (spApiBaseUrl != null && !spApiBaseUrl.isBlank()) ? spApiBaseUrl : DEFAULT_SP_API_BASE_URL;
        this.marketplaceId = (marketplaceId != null && !marketplaceId.isBlank()) ? marketplaceId : DEFAULT_MARKETPLACE_ID_BR;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Override
    public Marketplace getMarketplace() {
        return Marketplace.AMAZON;
    }

    @Override
    public List<ConnectorCapability> getCapabilities() {
        return List.of(
                ConnectorCapability.supported(ConnectorCapabilityType.CATALOG, "Leitura de ofertas e catálogo via Listings Items API e Catalog Items API"),
                ConnectorCapability.supported(ConnectorCapabilityType.LISTINGS, "Criação, edição e sincronização de ofertas via Listings Items API"),
                ConnectorCapability.supported(ConnectorCapabilityType.ORDERS, "Consulta de pedidos e itens via Orders API v0 (/orders/v0/orders)"),
                ConnectorCapability.unsupported(ConnectorCapabilityType.ADS, "Amazon Advertising API requer credenciais segregadas fora do escopo base da SP-API"),
                ConnectorCapability.supported(ConnectorCapabilityType.WEBHOOKS, "Notificações em tempo real via Amazon EventBridge e Notifications API"),
                ConnectorCapability.unsupported(ConnectorCapabilityType.PUBLIC_SEARCH, "A Amazon não oferece endpoint aberto de busca pública concorrencial. Scraping proibido."),
                ConnectorCapability.unsupported(ConnectorCapabilityType.PUBLIC_TRENDS, "Sem API pública aberta de tendências de busca na Amazon")
        );
    }

    @Override
    public OAuthAuthorizationUrl generateAuthorizationUrl(String state, String redirectUri) {
        validateConfiguredCredentials();

        String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
        String url = String.format("%s?application_id=%s&state=%s&version=beta",
                sellerCentralAuthUrl, appId, encodedState);

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
            String response = RestClient.create().post()
                    .uri(lwaAuthUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            String accessToken = node.get("access_token").asText();
            String refreshToken = node.has("refresh_token") ? node.get("refresh_token").asText() : "";
            long expiresIn = node.has("expires_in") ? node.get("expires_in").asLong() : 3600L;

            // Identificador da conta externa: em SP-API o sellerId é obtido via participações
            String externalAccountId = fetchSellerId(accessToken);

            return new OAuthTokenResult(
                    accessToken,
                    refreshToken,
                    externalAccountId,
                    "sellingpartnerapi::notifications,sellingpartnerapi::migration",
                    OffsetDateTime.now().plusSeconds(expiresIn),
                    OffsetDateTime.now().plusYears(1),
                    "PRODUCTION"
            );
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Amazon LWA token exchange hit rate limit: {}", e.getMessage());
            throw new BusinessException("AMAZON_RATE_LIMIT_EXCEEDED", "Taxa limite da Amazon excedida (HTTP 429). Tente novamente em instantes.", HttpStatus.TOO_MANY_REQUESTS);
        } catch (Exception e) {
            log.error("Failed to exchange Amazon authorization code: {}", e.getMessage());
            throw new BusinessException("AMAZON_AUTH_FAILED", "Falha na troca de código de autorização Amazon SP-API: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
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
            String response = RestClient.create().post()
                    .uri(lwaAuthUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            String newAccessToken = node.get("access_token").asText();
            String newRefreshToken = node.has("refresh_token") ? node.get("refresh_token").asText() : refreshToken;
            long expiresIn = node.has("expires_in") ? node.get("expires_in").asLong() : 3600L;

            return new OAuthTokenResult(
                    newAccessToken,
                    newRefreshToken,
                    "amazon-selling-partner",
                    "sellingpartnerapi::notifications",
                    OffsetDateTime.now().plusSeconds(expiresIn),
                    OffsetDateTime.now().plusYears(1),
                    "PRODUCTION"
            );
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Amazon LWA token refresh hit rate limit: {}", e.getMessage());
            throw new BusinessException("AMAZON_RATE_LIMIT_EXCEEDED", "Taxa limite da Amazon excedida (HTTP 429). Tente novamente em instantes.", HttpStatus.TOO_MANY_REQUESTS);
        } catch (Exception e) {
            log.error("Failed to refresh Amazon LWA token: {}", e.getMessage());
            throw new BusinessException("AMAZON_REFRESH_FAILED", "Falha na renovação de token Amazon LWA: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public void revokeToken(String accessToken, String externalAccountId) {
        log.info("Revoking Amazon connection in platform for seller ID: {}", externalAccountId);
        // Amazon gerencia desautorizações de aplicativos no Seller Central (Apps & Services)
    }

    @Override
    public boolean validateConnection(String accessToken, String externalAccountId) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }

        try {
            restClient.get()
                    .uri("/sellers/v1/marketplaceParticipations")
                    .header("x-amz-access-token", accessToken)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Amazon SP-API connection validation failed for seller {}: {}", externalAccountId, e.getMessage());
            return false;
        }
    }

    @Override
    public ConnectorSyncBatch<ConnectorListing> fetchListings(String accessToken, String externalAccountId, String cursor, int limit) {
        int safeLimit = (limit > 0 && limit <= 50) ? limit : 20;

        try {
            String uri = String.format("/listings/2021-08-01/items/%s?marketplaceIds=%s&pageSize=%d%s",
                    externalAccountId != null ? externalAccountId : "me",
                    marketplaceId,
                    safeLimit,
                    (cursor != null && !cursor.isBlank()) ? "&pageToken=" + cursor : "");

            String response = restClient.get()
                    .uri(uri)
                    .header("x-amz-access-token", accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            List<ConnectorListing> items = new ArrayList<>();

            JsonNode listingsNode = root.has("items") ? root.get("items") : root.get("listings");
            if (listingsNode != null && listingsNode.isArray()) {
                for (JsonNode item : listingsNode) {
                    String sku = item.has("sku") ? item.get("sku").asText() : "";
                    String asin = item.has("asin") ? item.get("asin").asText() : sku;
                    String title = item.has("title") ? item.get("title").asText() : "Produto Amazon " + sku;
                    String status = item.has("status") ? item.get("status").asText() : "ACTIVE";

                    BigDecimal price = BigDecimal.ZERO;
                    String currency = "BRL";
                    if (item.has("price")) {
                        JsonNode priceNode = item.get("price");
                        if (priceNode.isObject() && priceNode.has("amount")) {
                            price = new BigDecimal(priceNode.get("amount").asText());
                            if (priceNode.has("currency")) currency = priceNode.get("currency").asText();
                        } else if (priceNode.isNumber()) {
                            price = new BigDecimal(priceNode.asText());
                        }
                    }

                    int quantity = item.has("quantity") ? item.get("quantity").asInt() : 0;
                    String permalink = "https://www.amazon.com.br/dp/" + asin;

                    items.add(new ConnectorListing(
                            asin,
                            sku,
                            title,
                            status,
                            price,
                            currency,
                            quantity,
                            0, // Amazon não expõe soldQuantity cumulativo na listagem de oferta
                            "BUYABLE",
                            permalink,
                            null,
                            item.toString(),
                            Instant.now()
                    ));
                }
            }

            String nextCursor = root.has("pagination") && root.get("pagination").has("nextToken")
                    ? root.get("pagination").get("nextToken").asText()
                    : null;

            return new ConnectorSyncBatch<>(items, nextCursor, nextCursor != null);

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Rate limit on Amazon fetchListings: {}", e.getMessage());
            throw new BusinessException("AMAZON_RATE_LIMIT_EXCEEDED", "Taxa limite da Amazon excedida ao sincronizar anúncios.", HttpStatus.TOO_MANY_REQUESTS);
        } catch (Exception e) {
            log.error("Failed to fetch Amazon listings: {}", e.getMessage());
            throw new BusinessException("AMAZON_SYNC_LISTINGS_FAILED", "Falha ao buscar anúncios na Amazon SP-API: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public ConnectorSyncBatch<ConnectorOrder> fetchOrders(String accessToken, String externalAccountId, Instant from, String cursor, int limit) {
        int safeLimit = (limit > 0 && limit <= 50) ? limit : 20;

        try {
            StringBuilder uri = new StringBuilder("/orders/v0/orders?MarketplaceIds=").append(marketplaceId)
                    .append("&MaxResultsPerPage=").append(safeLimit);

            if (from != null) {
                uri.append("&CreatedAfter=").append(from.toString());
            }
            if (cursor != null && !cursor.isBlank()) {
                uri.append("&NextToken=").append(cursor);
            }

            String response = restClient.get()
                    .uri(uri.toString())
                    .header("x-amz-access-token", accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            List<ConnectorOrder> orders = new ArrayList<>();

            JsonNode payload = root.has("payload") ? root.get("payload") : root;
            JsonNode ordersNode = payload.has("Orders") ? payload.get("Orders") : payload.get("orders");

            if (ordersNode != null && ordersNode.isArray()) {
                for (JsonNode o : ordersNode) {
                    String orderId = o.get("AmazonOrderId").asText();
                    String status = o.has("OrderStatus") ? o.get("OrderStatus").asText() : "Pending";
                    Instant orderDate = o.has("PurchaseDate") ? Instant.parse(o.get("PurchaseDate").asText()) : Instant.now();

                    BigDecimal totalAmount = BigDecimal.ZERO;
                    String currency = "BRL";
                    if (o.has("OrderTotal") && o.get("OrderTotal").has("Amount")) {
                        totalAmount = new BigDecimal(o.get("OrderTotal").get("Amount").asText());
                        if (o.get("OrderTotal").has("CurrencyCode")) {
                            currency = o.get("OrderTotal").get("CurrencyCode").asText();
                        }
                    }

                    // PII de comprador (nome/CPF) sem RDT não é inventada
                    String buyerNickname = o.has("BuyerInfo") && o.get("BuyerInfo").has("BuyerName")
                            ? o.get("BuyerInfo").get("BuyerName").asText()
                            : null;

                    List<ConnectorOrderItem> items = new ArrayList<>();
                    if (o.has("OrderItems") && o.get("OrderItems").isArray()) {
                        for (JsonNode it : o.get("OrderItems")) {
                            String itemId = it.has("OrderItemId") ? it.get("OrderItemId").asText() : orderId + "-1";
                            String sku = it.has("SellerSKU") ? it.get("SellerSKU").asText() : "";
                            String title = it.has("Title") ? it.get("Title").asText() : "Item " + sku;
                            int qty = it.has("QuantityOrdered") ? it.get("QuantityOrdered").asInt() : 1;
                            BigDecimal unitPrice = BigDecimal.ZERO;
                            if (it.has("ItemPrice") && it.get("ItemPrice").has("Amount")) {
                                BigDecimal totalItem = new BigDecimal(it.get("ItemPrice").get("Amount").asText());
                                unitPrice = totalItem.divide(BigDecimal.valueOf(Math.max(1, qty)), 2, java.math.RoundingMode.HALF_EVEN);
                            }
                            items.add(new ConnectorOrderItem(itemId, sku, title, unitPrice, qty, BigDecimal.ZERO));
                        }
                    }

                    orders.add(new ConnectorOrder(
                            orderId,
                            status,
                            totalAmount,
                            currency,
                            orderDate,
                            buyerNickname,
                            null,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            items,
                            o.toString(),
                            Instant.now()
                    ));
                }
            }

            String nextCursor = payload.has("NextToken") ? payload.get("NextToken").asText() : null;
            return new ConnectorSyncBatch<>(orders, nextCursor, nextCursor != null);

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Rate limit on Amazon fetchOrders: {}", e.getMessage());
            throw new BusinessException("AMAZON_RATE_LIMIT_EXCEEDED", "Taxa limite da Amazon Orders API excedida (HTTP 429).", HttpStatus.TOO_MANY_REQUESTS);
        } catch (Exception e) {
            log.error("Failed to fetch Amazon orders: {}", e.getMessage());
            throw new BusinessException("AMAZON_SYNC_ORDERS_FAILED", "Falha ao sincronizar pedidos Amazon SP-API: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    private String fetchSellerId(String accessToken) {
        try {
            String resp = restClient.get()
                    .uri("/sellers/v1/marketplaceParticipations")
                    .header("x-amz-access-token", accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(resp);
            JsonNode payload = node.has("payload") ? node.get("payload") : node;
            if (payload.isArray() && !payload.isEmpty()) {
                JsonNode first = payload.get(0);
                if (first.has("participation") && first.get("participation").has("sellerId")) {
                    return first.get("participation").get("sellerId").asText();
                }
            }
        } catch (Exception e) {
            log.debug("Could not determine sellerId via marketplaceParticipations: {}", e.getMessage());
        }
        return "amazon-seller-" + marketplaceId;
    }

    private void validateConfiguredCredentials() {
        if (appId == null || appId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new BusinessException(
                    "MISSING_CONNECTOR_CREDENTIALS",
                    "Credenciais da aplicação Amazon SP-API (App ID / Client Secret) não estão configuradas.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
