package com.sellerintelligence.modules.identityaccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.BruteForceProtectionService;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.audit.AuditEvent;
import com.sellerintelligence.modules.audit.AuditEventRepository;
import com.sellerintelligence.modules.identityaccess.dto.LoginRequest;
import com.sellerintelligence.modules.identityaccess.dto.SwitchTenantRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationAndTenancyIntegrationTest {

    private static final String TENANT_ALPHA_ID = "11111111-1111-1111-1111-111111111111";
    private static final String TENANT_BETA_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private BruteForceProtectionService bruteForceProtectionService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        bruteForceProtectionService.reset();
    }

    @Test
    @DisplayName("Cenário 1: Autenticação com credenciais válidas e derivação do tenant ativo")
    void testSuccessfulLoginAndDerivedTenant() throws Exception {
        LoginRequest loginRequest = new LoginRequest("seller@example.com", "Password@123", null);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.activeTenantId").value(TENANT_ALPHA_ID))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_OWNER"))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
        JwtTokenProvider.Claims claims = jwtTokenProvider.validateAndParseToken(token);
        assertThat(claims.tenantId()).isEqualTo(UUID.fromString(TENANT_ALPHA_ID));

        List<AuditEvent> auditEvents = auditEventRepository.findByEventType("LOGIN_SUCCESS");
        assertThat(auditEvents).isNotEmpty();
    }

    @Test
    @DisplayName("Cenário 2: Autenticação com senha incorreta deve retornar 401 e auditar falha")
    void testFailedLoginWrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest("seller@example.com", "SenhaIncorreta@123", null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciais Inválidas"))
                .andExpect(jsonPath("$.status").value(401));

        List<AuditEvent> failedEvents = auditEventRepository.findByEventType("LOGIN_FAILED");
        assertThat(failedEvents).isNotEmpty();
    }

    @Test
    @DisplayName("Cenário 3: Autenticação com usuário inexistente deve retornar 401")
    void testFailedLoginUserNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest("inexistente@example.com", "Password@123", null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Cenário 4: Bloqueio por brute force após 5 falhas consecutivas retornando HTTP 429")
    void testBruteForceLockout() throws Exception {
        LoginRequest badRequest = new LoginRequest("seller@example.com", "SenhaErrada", null);

        // Dispara 5 tentativas consecutivas com falha
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // A 6ª tentativa deve ser categoricamente bloqueada com HTTP 429 Too Many Requests
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Limite de Tentativas Excedido"))
                .andExpect(jsonPath("$.status").value(429));

        List<AuditEvent> blockedEvents = auditEventRepository.findByEventType("BRUTE_FORCE_BLOCKED");
        assertThat(blockedEvents).isNotEmpty();
    }

    @Test
    @DisplayName("Cenário 5: Acesso anônimo a endpoint tenant-scoped deve ser negado por padrão (Fail-Closed)")
    void testTenantScopedEndpointWithoutAuthenticationFails() throws Exception {
        mockMvc.perform(get("/api/v1/tenancy/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Autenticação Requerida ou Inválida"));
    }

    @Test
    @DisplayName("Cenário 6: Acesso a endpoint tenant-scoped com token JWT expirado deve retornar 401")
    void testTenantScopedEndpointWithExpiredTokenFails() throws Exception {
        UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID tenantId = UUID.fromString(TENANT_ALPHA_ID);
        String expiredToken = jwtTokenProvider.generateToken(userId, "seller@example.com", tenantId, List.of("ROLE_OWNER"), -10);

        mockMvc.perform(get("/api/v1/tenancy/current")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Token JWT expirado")));
    }

    @Test
    @DisplayName("Cenário 7: Acesso a endpoint tenant-scoped com token válido retorna dados do tenant derivado")
    void testTenantScopedEndpointWithValidTokenSucceeds() throws Exception {
        String token = obtainValidToken("seller@example.com", "Password@123");

        mockMvc.perform(get("/api/v1/tenancy/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ALPHA_ID))
                .andExpect(jsonPath("$.name").value("Alpha Seller Org"))
                .andExpect(jsonPath("$.slug").value("alpha-seller"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Cenário 8: Usuário sem tenant/membership deve ter acesso negado (403 Forbidden)")
    void testTenantScopedEndpointUserWithoutMembershipFails() throws Exception {
        String orphanToken = obtainValidToken("orphan@example.com", "Password@123");

        mockMvc.perform(get("/api/v1/tenancy/current")
                        .header("Authorization", "Bearer " + orphanToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Contexto de Tenant Ausente"));
    }

    @Test
    @DisplayName("Cenário 9: Tentativa de spoofing cross-tenant via header arbitrário X-Tenant-Id deve ser bloqueada (403) e auditada")
    void testTenantSpoofingAttemptRejectedAndAudited() throws Exception {
        String token = obtainValidToken("seller@example.com", "Password@123");

        // O usuário do Tenant Alpha tenta acessar enviando X-Tenant-Id do Tenant Beta
        mockMvc.perform(get("/api/v1/tenancy/current")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", TENANT_BETA_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Violação de Isolamento Multi-Tenant"));

        List<AuditEvent> spoofingEvents = auditEventRepository.findByEventType("TENANT_SPOOFING_ATTEMPT");
        assertThat(spoofingEvents).isNotEmpty();
        assertThat(spoofingEvents)
                .anyMatch(event -> event.getTenantId() != null && event.getTenantId().toString().equals(TENANT_BETA_ID));
    }

    @Test
    @DisplayName("Cenário 10: Tentativa de spoofing com header X-Tenant-Id malformado deve retornar 400 Bad Request")
    void testMalformedTenantHeaderRejected() throws Exception {
        String token = obtainValidToken("seller@example.com", "Password@123");

        mockMvc.perform(get("/api/v1/tenancy/current")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "not-a-valid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Violação de Isolamento Multi-Tenant"));
    }

    @Test
    @DisplayName("Cenário 11: Parâmetro de consulta arbitrário (?tenantId=...) não altera o contexto do tenant")
    void testTenantCannotBeAlteredByQueryParameter() throws Exception {
        String token = obtainValidToken("seller@example.com", "Password@123");

        mockMvc.perform(get("/api/v1/tenancy/current?tenantId=" + TENANT_BETA_ID)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // O tenant retornado permanece estritamente o Tenant Alpha derivado da credencial/token
                .andExpect(jsonPath("$.tenantId").value(TENANT_ALPHA_ID))
                .andExpect(jsonPath("$.name").value("Alpha Seller Org"));
    }

    @Test
    @DisplayName("Cenário 12: Tentativa de troca de tenant para organização sem membership deve ser rejeitada (403)")
    void testSwitchTenantUnauthorizedRejected() throws Exception {
        String token = obtainValidToken("seller@example.com", "Password@123");
        SwitchTenantRequest request = new SwitchTenantRequest(UUID.fromString(TENANT_BETA_ID));

        mockMvc.perform(post("/api/v1/auth/switch-tenant")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Acesso Negado ao Tenant"));
    }

    @Test
    @DisplayName("Cenário 13: Endpoint /api/v1/auth/me retorna os dados do perfil e memberships ativas")
    void testGetMeEndpoint() throws Exception {
        String token = obtainValidToken("seller@example.com", "Password@123");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("seller@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alpha Seller Admin"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.memberships[0].tenantId").value(TENANT_ALPHA_ID))
                .andExpect(jsonPath("$.memberships[0].tenantName").value("Alpha Seller Org"))
                .andExpect(jsonPath("$.memberships[0].role").value("OWNER"));
    }

    private String obtainValidToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password, null);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }
}
