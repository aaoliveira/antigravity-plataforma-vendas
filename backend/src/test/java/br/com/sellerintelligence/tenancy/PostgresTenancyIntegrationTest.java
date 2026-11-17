package br.com.sellerintelligence.tenancy;

import br.com.sellerintelligence.audit.domain.AuditEvent;
import br.com.sellerintelligence.audit.domain.AuditEventRepository;
import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.tenancy.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("dev")
@Import(TestSecurityConfig.class)
class PostgresTenancyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    @DisplayName("PostgreSQL schema migrations apply and enforce constraints")
    void verifyPostgresConstraintsAndFlyway() {
        assertThat(postgres.isRunning()).isTrue();

        User user = userRepository.save(new User("sub-pg-1", "user-pg@example.com", "User Postgres"));
        assertThat(user.getId()).isNotNull();

        Organization org = organizationRepository.save(new Organization("Test Org", "test-org-pg"));
        assertThat(org.getId()).isNotNull();

        Membership membership = membershipRepository.save(new Membership(org, user, Role.OWNER, MembershipStatus.ACTIVE));
        assertThat(membership.getId()).isNotNull();

        AuditEvent auditEvent = auditEventRepository.save(new AuditEvent(
                org.getId(),
                user.getId().toString(),
                "INTEGRATION_TEST",
                "Organization",
                org.getId().toString(),
                "Verified with real PostgreSQL container",
                AuditResult.SUCCESS
        ));
        assertThat(auditEvent.getId()).isNotNull();

        // Verify unique slug constraint in PostgreSQL
        Organization duplicateOrg = new Organization("Duplicate Slug Org", "test-org-pg");
        assertThatThrownBy(() -> organizationRepository.saveAndFlush(duplicateOrg))
                .isNotNull();
    }
}
