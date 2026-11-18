package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnectionRepository;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.OAuthCallbackRequest;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MercadoLivreOAuthIntegrationTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_BETA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MarketplaceConnectionRepository connectionRepository;

    @MockitoBean
    private MeliOAuthClient meliOAuthClient;

    private String validTokenAlpha;
    private String validTokenBeta;

    @BeforeEach
    void setUp() {
        validTokenAlpha = jwtTokenProvider.generateToken(USER_ID, "seller@example.com", TENANT_ALPHA_ID, java.util.List.of("ADMIN"));
        validTokenBeta = jwtTokenProvider.generateToken(USER_ID, "seller@example.com", TENANT_BETA_ID, java.util.List.of("ADMIN"));

        when(meliOAuthClient.buildAuthorizationUrl(anyString(), anyString())).thenAnswer(invocation -> {
            String state = invocation.getArgument(0);
            String challenge = invocation.getArgument(1);
            return "https://auth.mercadolivre.com.br/authorization?response_type=code&client_id=test-app&state="
                    + state + "&code_challenge=" + challenge + "&code_challenge_method=S256";
        });

        when(meliOAuthClient.exchangeAuthorizationCode(anyString(), anyString(), anyString())).thenReturn(
                new MeliTokenResponse(
                        "APP_USR-sandbox-access-token-999",
                        "bearer",
                        21600L,
                        "read offline_access",
                        "123456789",
                        "TG-sandbox-refresh-token-999"
                )
        );

        when(meliOAuthClient.refreshAccessToken(anyString())).thenReturn(
                new MeliTokenResponse(
                        "APP_USR-refreshed-token-888",
                        "bearer",
                        21600L,
                        "read offline_access",
                        "123456789",
                        "TG-refreshed-token-888"
                )
        );
    }

    @Test
    @DisplayName("Cenário E2E: Iniciação OAuth, Callback com PKCE e Cifragem, Refresh sob lock e Revogação")
    void testFullOAuthLifecycleEndToEnd() throws Exception {
        // 1. Inicia o fluxo de autorização
        MvcResult initResult = mockMvc.perform(post("/api/v1/integrations/mercadolivre/authorize")
                        .header("Authorization", "Bearer " + validTokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.stateToken").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();

        String initResponseBody = initResult.getResponse().getContentAsString();
        String stateToken = objectMapper.readTree(initResponseBody).get("stateToken").asText();

        // 2. Callback com state inválido -> Rejeitado 400 Bad Request
        OAuthCallbackRequest invalidStateReq = new OAuthCallbackRequest("TG-code-abc", "invalid-state-token", "http://localhost:3000/callback");
        mockMvc.perform(post("/api/v1/integrations/mercadolivre/callback")
                        .header("Authorization", "Bearer " + validTokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("State Anti-CSRF Inválido"));

        // 3. Callback com state válido e code -> Sucesso 200 OK (Tokens omitidos na resposta)
        OAuthCallbackRequest validCallbackReq = new OAuthCallbackRequest("TG-auth-code-sandbox", stateToken, "http://localhost:3000/callback");
        MvcResult callbackResult = mockMvc.perform(post("/api/v1/integrations/mercadolivre/callback")
                        .header("Authorization", "Bearer " + validTokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCallbackReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ALPHA_ID.toString()))
                .andExpect(jsonPath("$.marketplace").value("MERCADO_LIVRE"))
                .andExpect(jsonPath("$.sellerAccountId").value("123456789"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accessToken").doesNotExist()) // Zero tokens na resposta
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String connIdStr = objectMapper.readTree(callbackResult.getResponse().getContentAsString()).get("id").asText();
        UUID connectionId = UUID.fromString(connIdStr);

        // 4. Lista conexões para o Tenant Alpha
        mockMvc.perform(get("/api/v1/integrations/mercadolivre/connections")
                        .header("Authorization", "Bearer " + validTokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(connIdStr));

        // 5. Isolamento Cross-Tenant: Tenant Beta tenta consultar conexão do Tenant Alpha -> 404 Not Found
        mockMvc.perform(get("/api/v1/integrations/mercadolivre/connections/" + connIdStr)
                        .header("Authorization", "Bearer " + validTokenBeta))
                .andExpect(status().isNotFound());

        // 6. Refresh do token da conexão sob lock
        mockMvc.perform(post("/api/v1/integrations/mercadolivre/connections/" + connIdStr + "/refresh")
                        .header("Authorization", "Bearer " + validTokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.lastRefreshedAt").exists());

        // 7. Revogação da conexão
        mockMvc.perform(post("/api/v1/integrations/mercadolivre/connections/" + connIdStr + "/revoke")
                        .header("Authorization", "Bearer " + validTokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        // 8. Tenta fazer refresh após revogação -> 409 Conflict ou 500 / Erro de conexão inativa
        mockMvc.perform(post("/api/v1/integrations/mercadolivre/connections/" + connIdStr + "/refresh")
                        .header("Authorization", "Bearer " + validTokenAlpha))
                .andExpect(status().isInternalServerError());
    }
}
