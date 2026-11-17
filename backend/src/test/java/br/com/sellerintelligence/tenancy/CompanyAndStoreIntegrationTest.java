package br.com.sellerintelligence.tenancy;

import br.com.sellerintelligence.audit.domain.AuditEventRepository;
import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.domain.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CompanyAndStoreIntegrationTest {

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
    private AuditEventRepository auditEventRepository;

    private User ownerAlpha;
    private User ownerBeta;
    private Organization orgAlpha;
    private Organization orgBeta;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        storeRepository.deleteAll();
        companyRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        ownerAlpha = userRepository.save(new User("sub-alpha", "owner@alpha.com", "Owner Alpha"));
        ownerBeta = userRepository.save(new User("sub-beta", "owner@beta.com", "Owner Beta"));

        orgAlpha = organizationRepository.save(new Organization("Alpha Group", "alpha-group"));
        orgBeta = organizationRepository.save(new Organization("Beta Group", "beta-group"));

        membershipRepository.save(new Membership(orgAlpha, ownerAlpha, Role.OWNER, MembershipStatus.ACTIVE));
        membershipRepository.save(new Membership(orgBeta, ownerBeta, Role.OWNER, MembershipStatus.ACTIVE));
    }

    @Test
    @DisplayName("Owner can create company with valid CNPJ and informative tax regime")
    void createCompanySuccess() throws Exception {
        String payload = """
                {
                    "cnpj": "06.990.590/0001-23",
                    "tradeName": "Google Brasil Internet Ltda",
                    "legalName": "Google Brasil Internet Ltda",
                    "taxRegime": "LUCRO_REAL"
                }
                """;

        mockMvc.perform(post("/api/v1/companies")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value("06.990.590/0001-23"))
                .andExpect(jsonPath("$.tradeName").value("Google Brasil Internet Ltda"))
                .andExpect(jsonPath("$.taxRegime").value("LUCRO_REAL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        var audits = auditEventRepository.findByActionAndResult("COMPANY_CREATED", AuditResult.SUCCESS);
        assertThat(audits).isNotEmpty();
    }

    @Test
    @DisplayName("Malformed or invalid CNPJ must be rejected with 400 Bad Request")
    void rejectInvalidCnpj() throws Exception {
        String payload = """
                {
                    "cnpj": "00.000.000/0001-99",
                    "tradeName": "Empresa Ficticia",
                    "taxRegime": "SIMPLES_NACIONAL"
                }
                """;

        mockMvc.perform(post("/api/v1/companies")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Owner can create store associated with company in same tenant")
    void createStoreSuccess() throws Exception {
        Company compAlpha = companyRepository.save(new Company(
                orgAlpha.getId(), "06.990.590/0001-23", "Alpha Comercio", "Alpha Comercio Ltda", TaxRegime.SIMPLES_NACIONAL));

        String payload = String.format("""
                {
                    "companyId": "%s",
                    "marketplace": "MERCADO_LIVRE",
                    "internalName": "Loja Principal ML",
                    "metadata": "{\\"tier\\":\\"platinum\\"}"
                }
                """, compAlpha.getId());

        mockMvc.perform(post("/api/v1/stores")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.internalName").value("Loja Principal ML"))
                .andExpect(jsonPath("$.marketplace").value("MERCADO_LIVRE"))
                .andExpect(jsonPath("$.companyId").value(compAlpha.getId().toString()))
                .andExpect(jsonPath("$.connectionStatus").value("DISCONNECTED"));

        var audits = auditEventRepository.findByActionAndResult("STORE_CREATED", AuditResult.SUCCESS);
        assertThat(audits).isNotEmpty();
    }

    @Test
    @DisplayName("Attempt to link store to company from another tenant is strictly blocked with 403 and audited")
    void crossTenantStoreCompanyLinkBlocked() throws Exception {
        Company compBeta = companyRepository.save(new Company(
                orgBeta.getId(), "33.000.167/0001-01", "Beta Comercio", "Beta Comercio S.A.", TaxRegime.LUCRO_PRESUMIDO));

        String payload = String.format("""
                {
                    "companyId": "%s",
                    "marketplace": "SHOPEE",
                    "internalName": "Shopee Store Illegal Link"
                }
                """, compBeta.getId());

        mockMvc.perform(post("/api/v1/stores")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));

        var denials = auditEventRepository.findByActionAndResult("ACCESS_DENIED", AuditResult.DENIED);
        assertThat(denials).isNotEmpty();
    }

    @Test
    @DisplayName("Owner can update company and store and toggle their status")
    void updateAndToggleStatus() throws Exception {
        Company comp = companyRepository.save(new Company(
                orgAlpha.getId(), "06.990.590/0001-23", "Alpha Old Name", "Alpha Old Razao", TaxRegime.SIMPLES_NACIONAL));
        Store store = storeRepository.save(new Store(orgAlpha.getId(), comp, Marketplace.MERCADO_LIVRE, "Store Old Name"));

        // Update company
        String updateCompPayload = """
                {
                    "tradeName": "Alpha New Trade Name",
                    "taxRegime": "LUCRO_PRESUMIDO"
                }
                """;
        mockMvc.perform(put("/api/v1/companies/" + comp.getId())
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateCompPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeName").value("Alpha New Trade Name"))
                .andExpect(jsonPath("$.taxRegime").value("LUCRO_PRESUMIDO"));

        // Toggle company status
        mockMvc.perform(patch("/api/v1/companies/" + comp.getId() + "/status?status=INACTIVE")
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // Update store
        String updateStorePayload = """
                {
                    "internalName": "Store New Name",
                    "externalAccountId": "MLB-987654321",
                    "connectionStatus": "CONNECTED"
                }
                """;
        mockMvc.perform(put("/api/v1/stores/" + store.getId())
                        .with(jwt().jwt(builder -> builder.subject(ownerAlpha.getKeycloakSub())
                                .claim("email", ownerAlpha.getEmail())
                                .claim("name", ownerAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateStorePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalName").value("Store New Name"))
                .andExpect(jsonPath("$.externalAccountId").value("MLB-987654321"))
                .andExpect(jsonPath("$.connectionStatus").value("CONNECTED"));
    }
}
