package br.com.sellerintelligence.security;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.infrastructure.security.RateLimitFilter;
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
import org.junit.jupiter.api.AfterEach;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class OwaspSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitFilter rateLimitFilter;

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

    private User ownerUser;
    private Organization org;
    private Store store;

    @BeforeEach
    void setUp() {
        storeRepository.deleteAll();
        companyRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = userRepository.save(new User("sub-owasp-owner", "owasp@seller.com", "Owasp Tester"));
        org = organizationRepository.save(new Organization("Security Test Org", "security-test-org"));
        membershipRepository.save(new Membership(org, ownerUser, Role.OWNER, MembershipStatus.ACTIVE));

        Company company = companyRepository.save(new Company(
                org.getId(),
                "06.990.590/0001-23",
                "Security Company",
                "Security Legal",
                TaxRegime.SIMPLES_NACIONAL
        ));

        store = storeRepository.save(new Store(
                org.getId(),
                company,
                Marketplace.MERCADO_LIVRE,
                "Security Store"
        ));

        rateLimitFilter.resetCounts();
        rateLimitFilter.setEnabled(false);
    }

    @AfterEach
    void tearDown() {
        rateLimitFilter.resetCounts();
        rateLimitFilter.setEnabled(false);
    }

    @Test
    @DisplayName("OWASP: HTTP Security Headers are properly enforced")
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    @DisplayName("OWASP Injection: SQL Injection attempts in query parameters are treated as literal strings")
    void sqlInjectionAttemptTreatedLiterally() throws Exception {
        String sqlPayload = """
                {
                    "query": "' OR '1'='1' UNION SELECT null, null, null --",
                    "marketplace": "MERCADO_LIVRE"
                }
                """;

        mockMvc.perform(post("/api/v1/market/search")
                        .with(jwt().jwt(builder -> builder.subject(ownerUser.getKeycloakSub())
                                .claim("email", ownerUser.getEmail())
                                .claim("name", ownerUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sqlPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("OWASP XSS: Script tags in company names are stored safely and encoded in JSON")
    void xssPayloadStoredSafely() throws Exception {
        String xssName = "<script>alert('XSS_INJECTION')</script>";
        String payload = """
                {
                    "cnpj": "33.000.167/0001-01",
                    "tradeName": "%s",
                    "legalName": "Safe Legal Name",
                    "taxRegime": "SIMPLES_NACIONAL"
                }
                """.formatted(xssName);

        mockMvc.perform(post("/api/v1/companies")
                        .with(jwt().jwt(builder -> builder.subject(ownerUser.getKeycloakSub())
                                .claim("email", ownerUser.getEmail())
                                .claim("name", ownerUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tradeName").value(xssName));

        // Verify stored in DB as raw text without code execution
        var companies = companyRepository.findByTenantId(org.getId());
        assertThat(companies.stream().anyMatch(c -> c.getTradeName().equals(xssName))).isTrue();
    }

    @Test
    @DisplayName("OWASP SSRF: Prohibited cloud metadata IP (169.254.169.254) in redirect URI is rejected")
    void ssrfCloudMetadataIpRejected() throws Exception {
        String payload = """
                {
                    "storeId": "%s",
                    "redirectUri": "http://169.254.169.254/latest/meta-data/"
                }
                """.formatted(store.getId());

        mockMvc.perform(post("/api/v1/marketplaces/connect/authorize")
                        .with(jwt().jwt(builder -> builder.subject(ownerUser.getKeycloakSub())
                                .claim("email", ownerUser.getEmail())
                                .claim("name", ownerUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROHIBITED_REDIRECT_HOST"));
    }

    @Test
    @DisplayName("OWASP SSRF: Prohibited private class A network IP (10.0.0.1) in redirect URI is rejected")
    void ssrfPrivateNetworkIpRejected() throws Exception {
        String payload = """
                {
                    "storeId": "%s",
                    "redirectUri": "http://10.0.0.1/admin"
                }
                """.formatted(store.getId());

        mockMvc.perform(post("/api/v1/marketplaces/connect/authorize")
                        .with(jwt().jwt(builder -> builder.subject(ownerUser.getKeycloakSub())
                                .claim("email", ownerUser.getEmail())
                                .claim("name", ownerUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROHIBITED_REDIRECT_HOST"));
    }

    @Test
    @DisplayName("OWASP Rate Limit: Exceeding request quota triggers HTTP 429 Too Many Requests")
    void rateLimitEnforcesTooManyRequests() throws Exception {
        rateLimitFilter.setEnabled(true);
        rateLimitFilter.setMaxRequestsPerMinute(3);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/companies")
                            .with(jwt().jwt(builder -> builder.subject(ownerUser.getKeycloakSub())
                                    .claim("email", ownerUser.getEmail())
                                    .claim("name", ownerUser.getName())))
                            .header("X-Tenant-Id", org.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-RateLimit-Limit"))
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        // 4th request must be rate limited
        mockMvc.perform(get("/api/v1/companies")
                        .with(jwt().jwt(builder -> builder.subject(ownerUser.getKeycloakSub())
                                .claim("email", ownerUser.getEmail())
                                .claim("name", ownerUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("OWASP Information Disclosure: Health actuator does not leak secrets or credentials")
    void healthActuatorDoesNotLeakSecrets() throws Exception {
        var result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("password");
        assertThat(body).doesNotContain("secret");
        assertThat(body).doesNotContain("aesKey");
    }
}
