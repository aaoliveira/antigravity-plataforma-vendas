package br.com.sellerintelligence.connector.web;

import br.com.sellerintelligence.audit.domain.AuditEventRepository;
import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.connector.crypto.TokenCryptoService;
import br.com.sellerintelligence.connector.model.MarketplaceCredential;
import br.com.sellerintelligence.connector.model.MarketplaceCredentialRepository;
import br.com.sellerintelligence.connector.model.OAuthState;
import br.com.sellerintelligence.connector.model.OAuthStateRepository;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.*;
import br.com.sellerintelligence.tenancy.store.ConnectionStatus;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class MarketplaceOAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OAuthStateRepository oauthStateRepository;

    @Autowired
    private MarketplaceCredentialRepository credentialRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private TokenCryptoService tokenCryptoService;

    private User ownerAlpha;
    private User ownerBeta;
    private Organization orgAlpha;
    private Organization orgBeta;
    private Company companyAlpha;
    private Store storeAlpha;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        credentialRepository.deleteAll();
        oauthStateRepository.deleteAll();
        storeRepository.deleteAll();
        companyRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        ownerAlpha = userRepository.save(new User("sub-alpha", "alpha@corp.com", "Owner Alpha"));
        ownerBeta = userRepository.save(new User("sub-beta", "beta@corp.com", "Owner Beta"));

        orgAlpha = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        orgBeta = organizationRepository.save(new Organization("Beta Corp", "beta-corp"));

        membershipRepository.save(new Membership(orgAlpha, ownerAlpha, Role.OWNER, MembershipStatus.ACTIVE));
        membershipRepository.save(new Membership(orgBeta, ownerBeta, Role.OWNER, MembershipStatus.ACTIVE));

        companyAlpha = companyRepository.save(new Company(
                orgAlpha.getId(), "06.990.590/0001-23", "Alpha Trading", "Alpha Trading Ltda", TaxRegime.SIMPLES_NACIONAL));

        storeAlpha = storeRepository.save(new Store(
                orgAlpha.getId(), companyAlpha, Marketplace.OUTRO, "Alpha Mock Store"));
    }

    @Test
    @DisplayName("Capabilities endpoint returns matrix for registered marketplaces")
    void getCapabilitiesMatrix() throws Exception {
        mockMvc.perform(get("/api/v1/marketplaces/capabilities")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Initiate OAuth flow returns valid state and authorization URL, auditing initiation")
    void initiateOAuthFlowSuccess() throws Exception {
        String payload = String.format("""
                {
                    "storeId": "%s",
                    "redirectUri": "https://app.sellerintelligence.com.br/oauth/callback"
                }
                """, storeAlpha.getId());

        mockMvc.perform(post("/api/v1/marketplaces/connect/authorize")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").isNotEmpty())
                .andExpect(jsonPath("$.state").isNotEmpty());

        var states = oauthStateRepository.findAll();
        assertThat(states).hasSize(1);
        assertThat(states.getFirst().getStoreId()).isEqualTo(storeAlpha.getId());

        var audits = auditEventRepository.findByActionAndResult("OAUTH_INITIATED", AuditResult.SUCCESS);
        assertThat(audits).isNotEmpty();
    }

    @Test
    @DisplayName("Full OAuth flow: state verification, token exchange, encrypted persistence and disconnect")
    void completeOAuthFlowAndDisconnect() throws Exception {
        // 1. Create active state
        String stateToken = "valid-test-state-token-" + UUID.randomUUID();
        OAuthState oauthState = new OAuthState(
                stateToken,
                orgAlpha.getId(),
                storeAlpha.getId(),
                ownerAlpha.getId(),
                Marketplace.OUTRO,
                "https://app.sellerintelligence.com.br/oauth/callback",
                OffsetDateTime.now().plusMinutes(15)
        );
        oauthStateRepository.save(oauthState);

        // 2. Callback with valid state and code
        String callbackPayload = String.format("""
                {
                    "state": "%s",
                    "code": "auth-code-123"
                }
                """, stateToken);

        mockMvc.perform(post("/api/v1/marketplaces/connect/callback")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.externalAccountId").value("fake-ext-account-123"));

        // 3. Verify state is now marked used
        OAuthState usedState = oauthStateRepository.findByState(stateToken).orElseThrow();
        assertThat(usedState.isUsed()).isTrue();

        // 4. Verify tokens in DB are strictly encrypted (never plaintext)
        MarketplaceCredential cred = credentialRepository.findByStoreId(storeAlpha.getId()).orElseThrow();
        assertThat(cred.getEncryptedAccessToken()).isNotEqualTo("fake-access-token-auth-code-123");
        assertThat(tokenCryptoService.decrypt(cred.getEncryptedAccessToken())).isEqualTo("fake-access-token-auth-code-123");
        assertThat(cred.getEnvironment()).isEqualTo("TEST");

        // 5. Reusing the state fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/marketplaces/connect/callback")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OAUTH_STATE_USED"));

        // 6. Disconnect store
        mockMvc.perform(post("/api/v1/marketplaces/connect/" + storeAlpha.getId() + "/disconnect")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionStatus").value("DISCONNECTED"));

        Store disconnectedStore = storeRepository.findById(storeAlpha.getId()).orElseThrow();
        assertThat(disconnectedStore.getConnectionStatus()).isEqualTo(ConnectionStatus.DISCONNECTED);

        var disconnectAudits = auditEventRepository.findByActionAndResult("STORE_DISCONNECTED", AuditResult.SUCCESS);
        assertThat(disconnectAudits).isNotEmpty();
    }

    @Test
    @DisplayName("Cross-tenant callback attempt is strictly denied with 403 and audited")
    void crossTenantCallbackDenied() throws Exception {
        // State belongs to Org Beta
        String stateToken = "beta-state-token-" + UUID.randomUUID();
        OAuthState oauthState = new OAuthState(
                stateToken,
                orgBeta.getId(),
                storeAlpha.getId(),
                ownerBeta.getId(),
                Marketplace.OUTRO,
                "https://app.sellerintelligence.com.br/oauth/callback",
                OffsetDateTime.now().plusMinutes(15)
        );
        oauthStateRepository.save(oauthState);

        String callbackPayload = String.format("""
                {
                    "state": "%s",
                    "code": "auth-code-456"
                }
                """, stateToken);

        // User Alpha tries to complete callback using Org Alpha tenant context
        mockMvc.perform(post("/api/v1/marketplaces/connect/callback")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));

        var denials = auditEventRepository.findByActionAndResult("ACCESS_DENIED", AuditResult.DENIED);
        assertThat(denials).isNotEmpty();
    }
}
