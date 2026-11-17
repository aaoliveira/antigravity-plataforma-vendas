package br.com.sellerintelligence.security;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.infrastructure.logging.CorrelationIdFilter;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    private User user;
    private Organization org;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("sub-obs", "obs@seller.com", "Observability User"));
        org = organizationRepository.save(new Organization("Obs Org", "obs-org"));
        membershipRepository.save(new Membership(org, user, Role.OWNER, MembershipStatus.ACTIVE));
    }

    @Test
    @DisplayName("Observability: Inbound X-Correlation-Id is preserved in response header")
    void inboundCorrelationIdPreserved() throws Exception {
        String testTraceId = "trace-custom-" + UUID.randomUUID();

        mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, testTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, testTraceId));
    }

    @Test
    @DisplayName("Observability: Generated correlation ID is returned when header is absent")
    void generatedCorrelationIdReturned() throws Exception {
        var result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .andReturn();

        String generatedId = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotBlank();
        assertThat(UUID.fromString(generatedId)).isNotNull();
    }

    @Test
    @DisplayName("Observability: ProblemDetail error responses include traceId")
    void problemDetailIncludesTraceId() throws Exception {
        String testTraceId = "trace-err-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/organizations/me")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, testTraceId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.traceId").value(testTraceId));
    }
}
