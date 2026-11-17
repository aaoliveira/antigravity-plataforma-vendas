package br.com.sellerintelligence.security;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.financial.domain.FinancialRuleSet;
import br.com.sellerintelligence.financial.domain.FinancialRuleSetRepository;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.market.domain.SavedSearch;
import br.com.sellerintelligence.market.domain.SavedSearchRepository;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class AdversarialSecurityIntegrationTest {

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
    private SavedSearchRepository savedSearchRepository;

    @Autowired
    private FinancialRuleSetRepository financialRuleSetRepository;

    private User userAlpha;
    private User userBeta;
    private User viewerAlpha;
    private User operatorAlpha;

    private Organization orgAlpha;
    private Organization orgBeta;

    private Company companyBeta;
    private Store storeBeta;
    private SavedSearch savedSearchBeta;

    @BeforeEach
    void setUp() {
        savedSearchRepository.deleteAll();
        financialRuleSetRepository.deleteAll();
        storeRepository.deleteAll();
        companyRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        // Tenant Alpha users
        userAlpha = userRepository.save(new User("sub-alpha-owner", "alpha-owner@tenant-a.com", "Owner Alpha"));
        viewerAlpha = userRepository.save(new User("sub-alpha-viewer", "alpha-viewer@tenant-a.com", "Viewer Alpha"));
        operatorAlpha = userRepository.save(new User("sub-alpha-operator", "alpha-operator@tenant-a.com", "Operator Alpha"));

        // Tenant Beta users
        userBeta = userRepository.save(new User("sub-beta-owner", "beta-owner@tenant-b.com", "Owner Beta"));

        // Organizations
        orgAlpha = organizationRepository.save(new Organization("Alpha Logistics Corp", "alpha-corp"));
        orgBeta = organizationRepository.save(new Organization("Beta Retail Ltd", "beta-ltd"));

        // Memberships Alpha
        membershipRepository.save(new Membership(orgAlpha, userAlpha, Role.OWNER, MembershipStatus.ACTIVE));
        membershipRepository.save(new Membership(orgAlpha, viewerAlpha, Role.VIEWER, MembershipStatus.ACTIVE));
        membershipRepository.save(new Membership(orgAlpha, operatorAlpha, Role.OPERATOR, MembershipStatus.ACTIVE));

        // Membership Beta
        membershipRepository.save(new Membership(orgBeta, userBeta, Role.OWNER, MembershipStatus.ACTIVE));

        // Beta resources
        companyBeta = companyRepository.save(new Company(
                orgBeta.getId(),
                "11.222.333/0001-81",
                "Beta Store LTDA",
                "Beta Legal Name",
                TaxRegime.LUCRO_PRESUMIDO
        ));

        storeBeta = storeRepository.save(new Store(
                orgBeta.getId(),
                companyBeta,
                Marketplace.MERCADO_LIVRE,
                "Beta ML Official Store"
        ));

        savedSearchBeta = savedSearchRepository.save(new SavedSearch(
                orgBeta.getId(),
                "Termômetro Digital",
                Marketplace.MERCADO_LIVRE,
                "termometro",
                "MLB1234",
                BigDecimal.ZERO,
                BigDecimal.valueOf(100),
                "NEW",
                "G-Tech",
                "Notas Beta"
        ));
    }

    @Test
    @DisplayName("Cross-tenant IDOR attack: User Alpha cannot access Company of Tenant Beta")
    void crossTenantIdorCompanyAccessBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/companies/" + companyBeta.getId())
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant IDOR attack: User Alpha spoofing X-Tenant-Id header is denied with 403")
    void crossTenantSpoofedHeaderDenied() throws Exception {
        mockMvc.perform(get("/api/v1/companies/" + companyBeta.getId())
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgBeta.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Cross-tenant IDOR attack: User Alpha cannot access Store of Tenant Beta")
    void crossTenantStoreAccessBlocked() throws Exception {
        mockMvc.perform(get("/api/v1/stores/" + storeBeta.getId())
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant IDOR attack: User Alpha cannot trigger sync on Tenant Beta store")
    void crossTenantSyncTriggerBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/sync/jobs")
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\": \"%s\", \"resourceType\": \"CATALOG\"}".formatted(storeBeta.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cross-tenant IDOR attack: User Alpha cannot delete Saved Search of Tenant Beta")
    void crossTenantSavedSearchBlocked() throws Exception {
        mockMvc.perform(delete("/api/v1/market/saved-searches/" + savedSearchBeta.getId())
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RBAC privilege escalation: VIEWER cannot create a new Company (403 Forbidden)")
    void viewerCannotCreateCompany() throws Exception {
        String payload = """
                {
                    "cnpj": "12.345.678/0001-90",
                    "tradeName": "Viewer Fake Company",
                    "legalName": "Viewer Fake Legal",
                    "taxRegime": "SIMPLES_NACIONAL"
                }
                """;

        mockMvc.perform(post("/api/v1/companies")
                        .with(jwt().jwt(builder -> builder.subject(viewerAlpha.getKeycloakSub())
                                .claim("email", viewerAlpha.getEmail())
                                .claim("name", viewerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC privilege escalation: VIEWER cannot create a new Store (403 Forbidden)")
    void viewerCannotCreateStore() throws Exception {
        String payload = """
                {
                    "companyId": "%s",
                    "marketplace": "SHOPEE",
                    "internalName": "Viewer Unauthorized Store"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/stores")
                        .with(jwt().jwt(builder -> builder.subject(viewerAlpha.getKeycloakSub())
                                .claim("email", viewerAlpha.getEmail())
                                .claim("name", viewerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC privilege escalation: VIEWER cannot send user invitations (403 Forbidden)")
    void viewerCannotSendInvitation() throws Exception {
        String payload = """
                {
                    "email": "hacker@intruder.com",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/v1/invitations")
                        .with(jwt().jwt(builder -> builder.subject(viewerAlpha.getKeycloakSub())
                                .claim("email", viewerAlpha.getEmail())
                                .claim("name", viewerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC privilege escalation: OPERATOR cannot access Financial Dashboard Overview (403 Forbidden)")
    void operatorCannotAccessFinancialSummary() throws Exception {
        mockMvc.perform(get("/api/v1/financial/dashboard/overview")
                        .with(jwt().jwt(builder -> builder.subject(operatorAlpha.getKeycloakSub())
                                .claim("email", operatorAlpha.getEmail())
                                .claim("name", operatorAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_FINANCIAL_ACCESS"));
    }

    @Test
    @DisplayName("RBAC authorization: OWNER can access Financial Dashboard Overview")
    void ownerCanAccessFinancialSummary() throws Exception {
        mockMvc.perform(get("/api/v1/financial/dashboard/overview")
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.provenance").exists());
    }
}
