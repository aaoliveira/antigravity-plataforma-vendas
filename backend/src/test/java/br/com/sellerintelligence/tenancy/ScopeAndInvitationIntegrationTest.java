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
import br.com.sellerintelligence.tenancy.invitation.InvitationStatus;
import br.com.sellerintelligence.tenancy.invitation.UserInvitation;
import br.com.sellerintelligence.tenancy.invitation.UserInvitationRepository;
import br.com.sellerintelligence.tenancy.scope.MembershipScopeRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ScopeAndInvitationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private MembershipScopeRepository membershipScopeRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserInvitationRepository invitationRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private User owner;
    private User inviteeUser;
    private Organization org;
    private Company company;
    private Store storeA;
    private Store storeB;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        membershipScopeRepository.deleteAll();
        invitationRepository.deleteAll();
        storeRepository.deleteAll();
        companyRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(new User("sub-owner", "owner@corp.com", "Owner User"));
        inviteeUser = userRepository.save(new User("sub-invitee", "invitee@corp.com", "Invitee User"));

        org = organizationRepository.save(new Organization("Holding Corp", "holding-corp"));
        membershipRepository.save(new Membership(org, owner, Role.OWNER, MembershipStatus.ACTIVE));

        company = companyRepository.save(new Company(
                org.getId(), "06.990.590/0001-23", "Holding Filial 1", "Holding Filial 1 Ltda", TaxRegime.SIMPLES_NACIONAL));

        storeA = storeRepository.save(new Store(org.getId(), company, Marketplace.MERCADO_LIVRE, "Loja Alpha ML"));
        storeB = storeRepository.save(new Store(org.getId(), company, Marketplace.SHOPEE, "Loja Beta Shopee"));
    }

    @Test
    @DisplayName("Owner can create scoped invitation, invitee accepts, and access is restricted to Store A")
    void invitationWithScopeLifecycle() throws Exception {
        // 1. Owner creates invitation restricted to Store A
        String createInvPayload = String.format("""
                {
                    "email": "invitee@corp.com",
                    "role": "ANALYST",
                    "allowedStoreIds": ["%s"]
                }
                """, storeA.getId());

        String invResponse = mockMvc.perform(post("/api/v1/invitations")
                        .with(jwt().jwt(builder -> builder.subject(owner.getKeycloakSub())
                                .claim("email", owner.getEmail())
                                .claim("name", owner.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createInvPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        UserInvitation inv = invitationRepository.findAll().getFirst();
        assertThat(inv.getToken()).isNotBlank();

        // 2. Invitee accepts invitation using token
        String acceptPayload = String.format("""
                {
                    "token": "%s"
                }
                """, inv.getToken());

        mockMvc.perform(post("/api/v1/invitations/accept")
                        .with(jwt().jwt(builder -> builder.subject(inviteeUser.getKeycloakSub())
                                .claim("email", inviteeUser.getEmail())
                                .claim("name", inviteeUser.getName())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ANALYST"));

        UserInvitation updatedInv = invitationRepository.findById(inv.getId()).orElseThrow();
        assertThat(updatedInv.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);

        // 3. Re-using token must fail
        mockMvc.perform(post("/api/v1/invitations/accept")
                        .with(jwt().jwt(builder -> builder.subject(inviteeUser.getKeycloakSub())
                                .claim("email", inviteeUser.getEmail())
                                .claim("name", inviteeUser.getName())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptPayload))
                .andExpect(status().isBadRequest());

        // 4. Scoped Invitee CAN query Store A
        mockMvc.perform(get("/api/v1/stores/" + storeA.getId())
                        .with(jwt().jwt(builder -> builder.subject(inviteeUser.getKeycloakSub())
                                .claim("email", inviteeUser.getEmail())
                                .claim("name", inviteeUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalName").value("Loja Alpha ML"));

        // 5. Scoped Invitee CANNOT query Store B (403 Forbidden and audit ACCESS_DENIED)
        mockMvc.perform(get("/api/v1/stores/" + storeB.getId())
                        .with(jwt().jwt(builder -> builder.subject(inviteeUser.getKeycloakSub())
                                .claim("email", inviteeUser.getEmail())
                                .claim("name", inviteeUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("TENANT_ACCESS_DENIED"));

        var denials = auditEventRepository.findByActionAndResult("ACCESS_DENIED", AuditResult.DENIED);
        assertThat(denials).isNotEmpty();
    }

    @Test
    @DisplayName("Expired invitation token cannot be accepted")
    void expiredTokenCannotBeAccepted() throws Exception {
        UserInvitation expiredInv = new UserInvitation(
                org.getId(),
                "late@corp.com",
                Role.VIEWER,
                "expired-token-12345",
                null,
                null,
                OffsetDateTime.now().minusDays(1), // already expired
                owner.getId()
        );
        invitationRepository.save(expiredInv);

        String payload = """
                {
                    "token": "expired-token-12345"
                }
                """;

        mockMvc.perform(post("/api/v1/invitations/accept")
                        .with(jwt().jwt(builder -> builder.subject(inviteeUser.getKeycloakSub())
                                .claim("email", inviteeUser.getEmail())
                                .claim("name", inviteeUser.getName())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Revoked invitation token cannot be accepted")
    void revokedTokenCannotBeAccepted() throws Exception {
        UserInvitation inv = new UserInvitation(
                org.getId(),
                "revoked@corp.com",
                Role.VIEWER,
                "revoked-token-12345",
                null,
                null,
                OffsetDateTime.now().plusDays(5),
                owner.getId()
        );
        inv.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(inv);

        String payload = """
                {
                    "token": "revoked-token-12345"
                }
                """;

        mockMvc.perform(post("/api/v1/invitations/accept")
                        .with(jwt().jwt(builder -> builder.subject(inviteeUser.getKeycloakSub())
                                .claim("email", inviteeUser.getEmail())
                                .claim("name", inviteeUser.getName())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
