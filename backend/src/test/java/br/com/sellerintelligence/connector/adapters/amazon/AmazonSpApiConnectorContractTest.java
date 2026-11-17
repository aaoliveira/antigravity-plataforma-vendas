package br.com.sellerintelligence.connector.adapters.amazon;

import br.com.sellerintelligence.connector.model.*;
import br.com.sellerintelligence.connector.ports.MarketplaceConnector;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AmazonSpApiConnectorContractTest {

    private ObjectMapper objectMapper;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private AmazonSpApiConnector connector;

    private static final String APP_ID = "amzn1.sp.solution.12345678-abcd-1234-abcd-123456789012";
    private static final String CLIENT_SECRET = "amzn.secret.xyz987654321";
    private static final String LWA_AUTH_URL = "https://api.amazon.com/auth/o2/token";
    private static final String SELLER_CENTRAL_URL = "https://sellercentral.amazon.com.br/apps/authorize/consent";
    private static final String SP_API_BASE_URL = "https://sellingpartnerapi-na.amazon.com";
    private static final String MARKETPLACE_ID_BR = "A2Q3Y263D00KWC";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        restClientBuilder = RestClient.builder().baseUrl(SP_API_BASE_URL);
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        connector = new AmazonSpApiConnector(
                APP_ID,
                CLIENT_SECRET,
                LWA_AUTH_URL,
                SELLER_CENTRAL_URL,
                SP_API_BASE_URL,
                MARKETPLACE_ID_BR,
                objectMapper,
                restClient
        );
    }

    @Test
    @DisplayName("Contract: connector strictly implements MarketplaceConnector and declares Amazon marketplace")
    void shouldComplyWithMarketplaceConnectorContract() {
        assertThat(connector).isInstanceOf(MarketplaceConnector.class);
        assertThat(connector.getMarketplace()).isEqualTo(Marketplace.AMAZON);
    }

    @Test
    @DisplayName("Contract: capabilities matrix matches official SP-API specifications without fabricating search or ads")
    void shouldDeclareAccurateCapabilities() {
        assertThat(connector.getCapabilityStatus(ConnectorCapabilityType.CATALOG))
                .isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(connector.getCapabilityStatus(ConnectorCapabilityType.LISTINGS))
                .isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(connector.getCapabilityStatus(ConnectorCapabilityType.ORDERS))
                .isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(connector.getCapabilityStatus(ConnectorCapabilityType.WEBHOOKS))
                .isEqualTo(CapabilityStatus.SUPPORTED);

        // Explicitly unsupported without scraping or segregated developer ads invitations
        assertThat(connector.getCapabilityStatus(ConnectorCapabilityType.ADS))
                .isEqualTo(CapabilityStatus.UNSUPPORTED);
        assertThat(connector.getCapabilityStatus(ConnectorCapabilityType.PUBLIC_SEARCH))
                .isEqualTo(CapabilityStatus.UNSUPPORTED);
        assertThat(connector.getCapabilityStatus(ConnectorCapabilityType.PUBLIC_TRENDS))
                .isEqualTo(CapabilityStatus.UNSUPPORTED);
    }

    @Test
    @DisplayName("OAuth: generates valid Seller Central Brazil consent URL with state and application_id")
    void shouldGenerateValidLwaConsentUrl() {
        String state = "secure-random-state-123";
        OAuthAuthorizationUrl authUrl = connector.generateAuthorizationUrl(state, "http://localhost:3000/callback");

        assertThat(authUrl.authorizationUrl()).contains(SELLER_CENTRAL_URL);
        assertThat(authUrl.authorizationUrl()).contains("application_id=" + APP_ID);
        assertThat(authUrl.authorizationUrl()).contains("state=secure-random-state-123");
        assertThat(authUrl.authorizationUrl()).contains("version=beta");
        assertThat(authUrl.state()).isEqualTo(state);
        assertThat(authUrl.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("OAuth: throws MISSING_CONNECTOR_CREDENTIALS when credentials are not configured")
    void shouldThrowWhenCredentialsMissing() {
        AmazonSpApiConnector unconfiguredConnector = new AmazonSpApiConnector(
                "", "", "", "", "", "", objectMapper
        );

        assertThatThrownBy(() -> unconfiguredConnector.generateAuthorizationUrl("state", "redirect"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Credenciais da aplicação Amazon SP-API");
    }

    @Test
    @DisplayName("Health Check: validateConnection returns true on successful marketplaceParticipations 200 OK")
    void shouldValidateConnectionSuccessfully() {
        mockServer.expect(requestTo(SP_API_BASE_URL + "/sellers/v1/marketplaceParticipations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-amz-access-token", "valid-amazon-token"))
                .andRespond(withSuccess("{\"payload\":[]}", MediaType.APPLICATION_JSON));

        boolean isValid = connector.validateConnection("valid-amazon-token", "seller-123");
        assertThat(isValid).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("Health Check: validateConnection returns false on 401 Unauthorized or failure")
    void shouldReturnFalseOnValidationFailure() {
        mockServer.expect(requestTo(SP_API_BASE_URL + "/sellers/v1/marketplaceParticipations"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        boolean isValid = connector.validateConnection("expired-token", "seller-123");
        assertThat(isValid).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("Sync Listings: maps SP-API listings payload to ConnectorListing without inventing unavailable fields")
    void shouldFetchListingsAndMapCorrectly() {
        String jsonResponse = """
            {
              "items": [
                {
                  "sku": "FONE-BT-AMZ-01",
                  "asin": "B08N5WRWNW",
                  "title": "Fone de Ouvido Bluetooth Sem Fio Estéreo",
                  "status": "BUYABLE",
                  "price": {
                    "amount": "149.90",
                    "currency": "BRL"
                  },
                  "quantity": 25
                }
              ],
              "pagination": {
                "nextToken": "next-cursor-token-abc"
              }
            }
            """;

        mockServer.expect(requestTo(SP_API_BASE_URL + "/listings/2021-08-01/items/A123456789?marketplaceIds=A2Q3Y263D00KWC&pageSize=20"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-amz-access-token", "token-xyz"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ConnectorSyncBatch<ConnectorListing> batch = connector.fetchListings("token-xyz", "A123456789", null, 20);

        assertThat(batch.items()).hasSize(1);
        ConnectorListing listing = batch.items().get(0);
        assertThat(listing.externalId()).isEqualTo("B08N5WRWNW");
        assertThat(listing.sku()).isEqualTo("FONE-BT-AMZ-01");
        assertThat(listing.title()).isEqualTo("Fone de Ouvido Bluetooth Sem Fio Estéreo");
        assertThat(listing.price()).isEqualByComparingTo(new BigDecimal("149.90"));
        assertThat(listing.currency()).isEqualTo("BRL");
        assertThat(listing.availableQuantity()).isEqualTo(25);
        assertThat(listing.soldQuantity()).isEqualTo(0); // Not fabricated
        assertThat(listing.permalink()).isEqualTo("https://www.amazon.com.br/dp/B08N5WRWNW");
        assertThat(batch.hasMore()).isTrue();
        assertThat(batch.nextCursor()).isEqualTo("next-cursor-token-abc");

        mockServer.verify();
    }

    @Test
    @DisplayName("Sync Orders: maps Orders API v0 payload to ConnectorOrder and items without fake PII")
    void shouldFetchOrdersAndMapCorrectly() {
        String jsonResponse = """
            {
              "payload": {
                "Orders": [
                  {
                    "AmazonOrderId": "701-9876543-1234567",
                    "OrderStatus": "Shipped",
                    "PurchaseDate": "2026-11-17T15:30:00Z",
                    "OrderTotal": {
                      "Amount": "299.80",
                      "CurrencyCode": "BRL"
                    },
                    "OrderItems": [
                      {
                        "OrderItemId": "item-991",
                        "SellerSKU": "FONE-BT-AMZ-01",
                        "Title": "Fone de Ouvido Bluetooth",
                        "QuantityOrdered": 2,
                        "ItemPrice": {
                          "Amount": "299.80",
                          "CurrencyCode": "BRL"
                        }
                      }
                    ]
                  }
                ],
                "NextToken": "next-order-cursor-xyz"
              }
            }
            """;

        mockServer.expect(requestTo(SP_API_BASE_URL + "/orders/v0/orders?MarketplaceIds=A2Q3Y263D00KWC&MaxResultsPerPage=20"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("x-amz-access-token", "token-xyz"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        ConnectorSyncBatch<ConnectorOrder> batch = connector.fetchOrders("token-xyz", "A123456789", null, null, 20);

        assertThat(batch.items()).hasSize(1);
        ConnectorOrder order = batch.items().get(0);
        assertThat(order.externalOrderId()).isEqualTo("701-9876543-1234567");
        assertThat(order.status()).isEqualTo("Shipped");
        assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("299.80"));
        assertThat(order.currency()).isEqualTo("BRL");
        assertThat(order.orderDate()).isEqualTo(Instant.parse("2026-11-17T15:30:00Z"));
        assertThat(order.buyerNickname()).isNull(); // Buyer PII restricted without RDT; not fabricated
        assertThat(order.items()).hasSize(1);

        ConnectorOrderItem item = order.items().get(0);
        assertThat(item.externalItemId()).isEqualTo("item-991");
        assertThat(item.sku()).isEqualTo("FONE-BT-AMZ-01");
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.unitPrice()).isEqualByComparingTo(new BigDecimal("149.90"));

        assertThat(batch.hasMore()).isTrue();
        assertThat(batch.nextCursor()).isEqualTo("next-order-cursor-xyz");

        mockServer.verify();
    }

    @Test
    @DisplayName("Resilience: handles 429 Too Many Requests throwing AMAZON_RATE_LIMIT_EXCEEDED")
    void shouldHandleRateLimitExceeded() {
        mockServer.expect(requestTo(SP_API_BASE_URL + "/listings/2021-08-01/items/A123?marketplaceIds=A2Q3Y263D00KWC&pageSize=20"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("x-amzn-RateLimit-Limit", "0.5")
                        .body("{\"message\":\"Rate exceeded\"}"));

        assertThatThrownBy(() -> connector.fetchListings("token", "A123", null, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Taxa limite da Amazon excedida");

        mockServer.verify();
    }
}
