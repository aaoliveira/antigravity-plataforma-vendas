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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class RbacIntegrationTest {

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

    private User ownerUser;
    private User adminUser;
    private User viewerUser;
    private Organization org;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        ownerUser = userRepository.save(new User("sub-owner", "owner@company.com", "Owner User"));
        adminUser = userRepository.save(new User("sub-admin", "admin@company.com", "Admin User"));
        viewerUser = userRepository.save(new User("sub-viewer", "viewer@company.com", "Viewer User"));

        org = organizationRepository.save(new Organization("Acme Corp", "acme-corp"));

        membershipRepository.save(new Membership(org, ownerUser, Role.OWNER, MembershipStatus.ACTIVE));
        membershipRepository.save(new Membership(org, adminUser, Role.ADMIN, MembershipStatus.ACTIVE));
        membershipRepository.save(new Membership(org, viewerUser, Role.VIEWER, MembershipStatus.ACTIVE));
    }

    @Test
    @DisplayName("OWNER can invite new member and event is audited")
    void ownerCanInviteMember() throws Exception {
        String payload = """
                {
                    "email": "analyst@company.com",
                    "name": "Analyst User",
                    "role": "ANALYST"
                }
                """;

        mockMvc.perform(post("/api/v1/organizations/" + org.getId() + "/members")
                        .with(jwt().jwt(builder -> builder.subject(ownerUser.getKeycloakSub())
                                .claim("email", ownerUser.getEmail())
                                .claim("name", ownerUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("analyst@company.com"))
                .andExpect(jsonPath("$.role").value("ANALYST"));

        var auditEvents = auditEventRepository.findByActionAndResult("MEMBER_INVITED", AuditResult.SUCCESS);
        assertThat(auditEvents).isNotEmpty();
    }

    @Test
    @DisplayName("ADMIN cannot assign OWNER role (returns 403 and audits denial)")
    void adminCannotAssignOwnerRole() throws Exception {
        String payload = """
                {
                    "email": "newowner@company.com",
                    "name": "New Owner",
                    "role": "OWNER"
                }
                """;

        mockMvc.perform(post("/api/v1/organizations/" + org.getId() + "/members")
                        .with(jwt().jwt(builder -> builder.subject(adminUser.getKeycloakSub())
                                .claim("email", adminUser.getEmail())
                                .claim("name", adminUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        var denials = auditEventRepository.findByActionAndResult("ACCESS_DENIED", AuditResult.DENIED);
        assertThat(denials).isNotEmpty();
    }

    @Test
    @DisplayName("VIEWER cannot invite members (returns 403)")
    void viewerCannotInviteMember() throws Exception {
        String payload = """
                {
                    "email": "other@company.com",
                    "name": "Other User",
                    "role": "OPERATOR"
                }
                """;

        mockMvc.perform(post("/api/v1/organizations/" + org.getId() + "/members")
                        .with(jwt().jwt(builder -> builder.subject(viewerUser.getKeycloakSub())
                                .claim("email", viewerUser.getEmail())
                                .claim("name", viewerUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OWNER can change member role and event is audited")
    void ownerCanChangeMemberRole() throws Exception {
        String payload = """
                {
                    "role": "FINANCE"
                }
                """;

        mockMvc.perform(patch("/api/v1/organizations/" + org.getId() + "/members/" + viewerUser.getId() + "/role")
                        .with(jwt().jwt(builder -> builder.subject(ownerUser.getKeycloakSub())
                                .claim("email", ownerUser.getEmail())
                                .claim("name", ownerUser.getName())))
                        .header("X-Tenant-Id", org.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("FINANCE"));

        var audits = auditEventRepository.findByActionAndResult("MEMBER_ROLE_CHANGED", AuditResult.SUCCESS);
        assertThat(audits).isNotEmpty();
    }
}
