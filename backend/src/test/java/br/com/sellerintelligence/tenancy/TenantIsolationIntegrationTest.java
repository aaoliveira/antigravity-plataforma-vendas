package br.com.sellerintelligence.tenancy;

import br.com.sellerintelligence.audit.domain.AuditEventRepository;
import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.tenancy.domain.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class TenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private User userAlpha;
    private User userBeta;
    private Organization orgAlpha;
    private Organization orgBeta;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        userAlpha = userRepository.save(new User("sub-alpha-101", "alpha@tenant-a.com", "User Alpha"));
        userBeta = userRepository.save(new User("sub-beta-202", "beta@tenant-b.com", "User Beta"));

        orgAlpha = organizationRepository.save(new Organization("Alpha Corp", "alpha-corp"));
        orgBeta = organizationRepository.save(new Organization("Beta LLC", "beta-llc"));

        membershipRepository.save(new Membership(orgAlpha, userAlpha, Role.OWNER, MembershipStatus.ACTIVE));
        membershipRepository.save(new Membership(orgBeta, userBeta, Role.OWNER, MembershipStatus.ACTIVE));
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint must return 401 Unauthorized")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Public health probe must be accessible without authentication")
    void healthProbeIsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("User Alpha can access their own organization Alpha")
    void userAlphaCanAccessOrgAlpha() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + orgAlpha.getId())
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgAlpha.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orgAlpha.getId().toString()))
                .andExpect(jsonPath("$.slug").value("alpha-corp"))
                .andExpect(jsonPath("$.userRole").value("OWNER"));
    }

    @Test
    @DisplayName("User Alpha cannot access organization Beta (cross-tenant denied with 403)")
    void userAlphaCannotAccessOrgBeta() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + orgBeta.getId())
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgBeta.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        var deniedEvents = auditEventRepository.findByActionAndResult("ACCESS_DENIED", AuditResult.DENIED);
        assertThat(deniedEvents).isNotEmpty();
        assertThat(deniedEvents.getFirst().getTenantId()).isEqualTo(orgBeta.getId());
    }

    @Test
    @DisplayName("User Alpha cannot list members of organization Beta")
    void userAlphaCannotListMembersOfOrgBeta() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/" + orgBeta.getId() + "/members")
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgBeta.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("User Alpha cannot modify member role in organization Beta")
    void userAlphaCannotUpdateMemberInOrgBeta() throws Exception {
        String payload = """
                {
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(patch("/api/v1/organizations/" + orgBeta.getId() + "/members/" + userBeta.getId() + "/role")
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .header("X-Tenant-Id", orgBeta.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Creating organization assigns current user as OWNER and persists audit record")
    void createOrganizationAssignsOwnerAndAudits() throws Exception {
        String payload = """
                {
                    "name": "Gamma Ventures",
                    "slug": "gamma-ventures"
                }
                """;

        mockMvc.perform(post("/api/v1/organizations")
                        .with(jwt().jwt(builder -> builder.subject(userAlpha.getKeycloakSub())
                                .claim("email", userAlpha.getEmail())
                                .claim("name", userAlpha.getName())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("gamma-ventures"))
                .andExpect(jsonPath("$.userRole").value("OWNER"));

        Organization gamma = organizationRepository.findBySlug("gamma-ventures").orElseThrow();
        assertThat(membershipRepository.existsByOrganizationIdAndUserIdAndStatus(
                gamma.getId(), userAlpha.getId(), MembershipStatus.ACTIVE)).isTrue();

        var successAudits = auditEventRepository.findByActionAndResult("ORGANIZATION_CREATED", AuditResult.SUCCESS);
        assertThat(successAudits).isNotEmpty();
    }
}
