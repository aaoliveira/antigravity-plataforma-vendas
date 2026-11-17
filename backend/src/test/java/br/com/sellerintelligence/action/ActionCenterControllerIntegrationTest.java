package br.com.sellerintelligence.action;

import br.com.sellerintelligence.action.domain.*;
import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class ActionCenterControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private ActionAlertRuleRepository ruleRepository;

    @Autowired
    private ActionAlertRepository alertRepository;

    @Autowired
    private ActionTaskRepository taskRepository;

    private Organization testOrg;
    private User testUser;
    private String userSub;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        alertRepository.deleteAll();
        ruleRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        testOrg = organizationRepository.save(new Organization("Action Org", "action-org-" + UUID.randomUUID()));
        userSub = "action-user-" + UUID.randomUUID();
        testUser = userRepository.save(new User(userSub, "action@seller.com", "Action User"));
        membershipRepository.save(new Membership(testOrg, testUser, Role.OWNER, MembershipStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should execute complete Action Center flow: rule creation, simulation, inbox, task conversion and audit")
    void shouldExecuteActionCenterLifecycleFlow() throws Exception {
        // 1. Create Alert Rule
        String createRuleJson = """
                {
                    "name": "Margem Crítica < 10%",
                    "ruleType": "MARGIN_THRESHOLD",
                    "scopeType": "ORGANIZATION",
                    "thresholdValue": 10.00,
                    "thresholdOperator": "LESS_THAN",
                    "timeWindowMinutes": 60,
                    "cooldownMinutes": 120,
                    "severity": "CRITICAL"
                }
                """;

        String ruleResponse = mockMvc.perform(post("/api/v1/action/rules")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", "action@seller.com")))
                        .header("X-Tenant-Id", testOrg.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRuleJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Margem Crítica < 10%"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andReturn().getResponse().getContentAsString();

        String ruleId = com.jayway.jsonpath.JsonPath.read(ruleResponse, "$.id");

        // 2. Simulate Rule (triggering an alert)
        String simulateJson = """
                {
                    "observedValue": 6.50,
                    "targetContext": "Pedido MLB-998877",
                    "persistAlert": true
                }
                """;

        String simResponse = mockMvc.perform(post("/api/v1/action/rules/" + ruleId + "/simulate")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", "action@seller.com")))
                        .header("X-Tenant-Id", testOrg.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simulateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triggered").value(true))
                .andExpect(jsonPath("$.generatedAlertId").exists())
                .andReturn().getResponse().getContentAsString();

        String alertId = com.jayway.jsonpath.JsonPath.read(simResponse, "$.generatedAlertId");

        // 3. List Alerts from Inbox
        mockMvc.perform(get("/api/v1/action/alerts")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", "action@seller.com")))
                        .header("X-Tenant-Id", testOrg.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alertId))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));

        // 4. Convert Alert into Operational Task
        String createTaskFromAlertJson = String.format("""
                {
                    "priority": "HIGH",
                    "assignedToUserId": "%s",
                    "customTitle": "Revisar custo do pedido MLB-998877",
                    "customNotes": "Verificar frete e comissão cobrada"
                }
                """, testUser.getId());

        String taskResponse = mockMvc.perform(post("/api/v1/action/tasks/from-alert/" + alertId)
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", "action@seller.com")))
                        .header("X-Tenant-Id", testOrg.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTaskFromAlertJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.alertId").value(alertId))
                .andExpect(jsonPath("$.title").value("Revisar custo do pedido MLB-998877"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn().getResponse().getContentAsString();

        String taskId = com.jayway.jsonpath.JsonPath.read(taskResponse, "$.id");

        // 5. Update Task Status to IN_PROGRESS
        mockMvc.perform(patch("/api/v1/action/tasks/" + taskId + "/status")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", "action@seller.com")))
                        .header("X-Tenant-Id", testOrg.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "IN_PROGRESS",
                                    "notes": "Analisando comprovante fiscal"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 6. Complete Task (status = DONE)
        mockMvc.perform(patch("/api/v1/action/tasks/" + taskId + "/status")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", "action@seller.com")))
                        .header("X-Tenant-Id", testOrg.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "DONE",
                                    "notes": "Custos reajustados e margem recomposta"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").exists());

        // 7. Verify Task Audit Trail
        mockMvc.perform(get("/api/v1/action/tasks/" + taskId + "/audits")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", "action@seller.com")))
                        .header("X-Tenant-Id", testOrg.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3)) // CREATED -> STATUS_CHANGED -> COMPLETED
                .andExpect(jsonPath("$[0].actionType").value("CREATED"))
                .andExpect(jsonPath("$[1].actionType").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[2].actionType").value("COMPLETED"));

        // 8. Configure Notification Preference (Opt-in)
        mockMvc.perform(post("/api/v1/action/notifications/preferences")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", "action@seller.com")))
                        .header("X-Tenant-Id", testOrg.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "channel": "EMAIL",
                                    "optedIn": true,
                                    "targetAddress": "ops@seller.com",
                                    "frequencyLimitPerHour": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.optedIn").value(true));
    }
}
