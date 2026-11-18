package com.sellerintelligence.modules.marketplaceconnection.shopee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnection;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnectionRepository;
import com.sellerintelligence.modules.marketplaceconnection.dto.ConnectionSummaryResponse;
import com.sellerintelligence.modules.marketplaceconnection.shopee.dto.ShopeeCallbackRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopeeOpenPlatformIntegrationTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_BETA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final long SHOP_ID = 987654321L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MarketplaceConnectionRepository connectionRepository;

    @Autowired
    private ShopeeOAuthProperties properties;

    @MockitoBean
    private ShopeeOpenPlatformClient shopeeClient;

    private String validTokenAlpha;
    private String validTokenBeta;

    @BeforeEach
    void setUp() {
        validTokenAlpha = jwtTokenProvider.generateToken(USER_ID, "seller@example.com", TENANT_ALPHA_ID, List.of("ADMIN"));
        validTokenBeta = jwtTokenProvider.generateToken(USER_ID, "seller@example.com", TENANT_BETA_ID, List.of("ADMIN"));

        when(shopeeClient.buildAuthorizationUrl(anyLong(), anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            long partnerId = invocation.getArgument(0);
            return "https://partner.shopeemobile.com/api/v2/shop/auth_partner?partner_id=" + partnerId + "&timestamp=12345&sign=test-sign";
        });

        when(shopeeClient.getAccessToken(anyString(), anyLong(), anyLong(), anyString(), anyString())).thenReturn(
                new ShopeeTokenResponse(
                        "shopee-access-token-integration-111",
                        "shopee-refresh-token-integration-111",
                        14400L,
                        "req-1",
                        null,
                        "success",
                        SHOP_ID
                )
        );

        when(shopeeClient.refreshAccessToken(anyString(), anyLong(), anyLong(), anyString(), anyString())).thenReturn(
                new ShopeeTokenResponse(
                        "shopee-access-token-refreshed-222",
                        "shopee-refresh-token-new-222",
                        14400L,
                        "req-2",
                        null,
                        "success",
                        SHOP_ID
                )
        );
    }

    @Test
    @DisplayName("Cenário E2E: Iniciação Shopee, Callback com HMAC, Isolamento Cross-Tenant, Refresh sob lock, Revogação e Webhook")
    void testFullShopeeLifecycleEndToEnd() throws Exception {
        // 1. Iniciação do fluxo de autorização
        mockMvc.perform(post("/api/v1/integrations/shopee/authorize")
                        .header("Authorization", "Bearer " + validTokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("auth_partner")));

        // 2. Callback de autorização com code e shop_id
        ShopeeCallbackRequest callbackRequest = new ShopeeCallbackRequest("auth_code_integration", SHOP_ID, null);

        MvcResult callbackResult = mockMvc.perform(post("/api/v1/integrations/shopee/callback")
                        .header("Authorization", "Bearer " + validTokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketplace").value("SHOPEE"))
                .andExpect(jsonPath("$.sellerAccountId").value(String.valueOf(SHOP_ID)))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        ConnectionSummaryResponse summary = objectMapper.readValue(
                callbackResult.getResponse().getContentAsString(),
                ConnectionSummaryResponse.class
        );
        UUID connectionId = summary.id();

        // 3. Verificação no banco: tokens salvos de forma cifrada (zero texto puro)
        MarketplaceConnection persisted = connectionRepository.findById(connectionId).orElseThrow();
        assertThat(persisted.getEncryptedAccessToken()).isNotBlank();
        assertThat(persisted.getEncryptedRefreshToken()).isNotBlank();
        assertThat(persisted.getEncryptedAccessToken()).doesNotContain("shopee-access-token-integration-111");

        // 4. Isolamento Cross-Tenant: Tenant Beta não deve conseguir ver nem operar na conexão do Tenant Alpha
        mockMvc.perform(get("/api/v1/integrations/shopee/connections/" + connectionId)
                        .header("Authorization", "Bearer " + validTokenBeta))
                .andExpect(status().isNotFound());

        // 5. Renovação de token sob lock exclusivo
        mockMvc.perform(post("/api/v1/integrations/shopee/connections/" + connectionId + "/refresh")
                        .header("Authorization", "Bearer " + validTokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 6. Teste de Webhook público: assinatura inválida é rejeitada com 401
        String webhookBody = String.format("{\"code\":2,\"shop_id\":%d,\"timestamp\":%d,\"data\":{}}",
                SHOP_ID, Instant.now().getEpochSecond());

        mockMvc.perform(post("/api/v1/integrations/shopee/webhook")
                        .header("Authorization", "forged_invalid_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isUnauthorized());

        // 7. Teste de Webhook público com assinatura válida: revogação automática (push code 2)
        String requestUrl = "http://localhost/api/v1/integrations/shopee/webhook";
        String baseString = ShopeeSignatureUtil.buildWebhookBaseString(requestUrl, webhookBody);
        String validSignature = ShopeeSignatureUtil.calculateHmacSha256(baseString, properties.getPartnerKey());

        mockMvc.perform(post("/api/v1/integrations/shopee/webhook")
                        .header("Authorization", validSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Conexão agora deve constar como REVOKED no banco e com tokens anulados
        MarketplaceConnection revoked = connectionRepository.findById(connectionId).orElseThrow();
        assertThat(revoked.getStatus()).isEqualTo("REVOKED");
        assertThat(revoked.getEncryptedAccessToken()).isNull();
        assertThat(revoked.getEncryptedRefreshToken()).isNull();
    }
}
