package br.com.sellerintelligence.mcp.web;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.mcp.domain.McpApiKey;
import br.com.sellerintelligence.mcp.domain.McpApiKeyRepository;
import br.com.sellerintelligence.mcp.service.McpAuthService;
import br.com.sellerintelligence.mcp.web.dto.ConversationalChatRequest;
import br.com.sellerintelligence.mcp.web.dto.CreateApiKeyRequest;
import br.com.sellerintelligence.mcp.web.dto.McpToolCallRequest;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class McpControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private McpApiKeyRepository apiKeyRepository;

    @Autowired
    private McpAuthService authService;

    private Organization organization;
    private User user;
    private String userSub;

    @BeforeEach
    void setUp() {
        organization = new Organization("MCP Integration Org " + UUID.randomUUID(), "mcp-org-" + UUID.randomUUID());
        organizationRepository.save(organization);

        userSub = "user-mcp-" + UUID.randomUUID();
        user = new User(userSub, "mcp_seller_" + UUID.randomUUID() + "@test.com", "MCP Seller");
        userRepository.save(user);

        Membership membership = new Membership(organization, user, Role.ADMIN);
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);
    }

    @Test
    @DisplayName("GET /api/v1/mcp/tools should list all 5 approved read-only tools")
    void shouldListMcpTools() throws Exception {
        mockMvc.perform(get("/api/v1/mcp/tools")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", user.getEmail())))
                        .header("X-Tenant-Id", organization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].name", hasItems(
                        "search_market", "query_tenant_products", "query_sales_indicators",
                        "calculate_sku_margin", "query_reconciliation_summary"
                )))
                .andExpect(jsonPath("$[*].readOnly", everyItem(is(true))));
    }

    @Test
    @DisplayName("POST /api/v1/mcp/tools/call should execute calculate_sku_margin with deterministic response")
    void shouldExecuteMcpToolSuccessfully() throws Exception {
        McpToolCallRequest request = new McpToolCallRequest(
                "calculate_sku_margin",
                Map.of("sku", "SKU-TEST-01", "salePrice", 100.00)
        );

        mockMvc.perform(post("/api/v1/mcp/tools/call")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", user.getEmail())))
                        .header("X-Tenant-Id", organization.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.toolName", is("calculate_sku_margin")))
                .andExpect(jsonPath("$.data.sku", is("SKU-TEST-01")))
                .andExpect(jsonPath("$.provenance.classification", is("ESTIMATED")));
    }

    @Test
    @DisplayName("POST /api/v1/mcp/tools/call with mutable operation should be BLOCKED_MUTABLE_DENIED")
    void shouldBlockMutableToolExecution() throws Exception {
        McpToolCallRequest request = new McpToolCallRequest(
                "update_price",
                Map.of("sku", "SKU-01", "price", 50.00)
        );

        mockMvc.perform(post("/api/v1/mcp/tools/call")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", user.getEmail())))
                        .header("X-Tenant-Id", organization.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is("BLOCKED_MUTABLE_DENIED")))
                .andExpect(jsonPath("$.errorMessage", containsString("Operação mutável 'update_price' é estritamente proibida")));
    }

    @Test
    @DisplayName("POST /api/v1/mcp/tools/call with foreign tenant_id should be BLOCKED_CROSS_TENANT")
    void shouldBlockCrossTenantCall() throws Exception {
        UUID foreignTenantId = UUID.randomUUID();
        McpToolCallRequest request = new McpToolCallRequest(
                "query_tenant_products",
                Map.of("tenant_id", foreignTenantId.toString())
        );

        mockMvc.perform(post("/api/v1/mcp/tools/call")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", user.getEmail())))
                        .header("X-Tenant-Id", organization.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is("BLOCKED_CROSS_TENANT")))
                .andExpect(jsonPath("$.errorMessage", containsString("Tentativa de acesso cross-tenant detectada")));
    }

    @Test
    @DisplayName("POST /api/v1/mcp/api-keys and GET /api/v1/mcp/api-keys should manage API keys")
    void shouldManageMcpApiKeys() throws Exception {
        CreateApiKeyRequest request = new CreateApiKeyRequest("Cursor Client", 30L);

        mockMvc.perform(post("/api/v1/mcp/api-keys")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", user.getEmail())))
                        .header("X-Tenant-Id", organization.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Cursor Client")))
                .andExpect(jsonPath("$.plainTextApiKey", startsWith("sip_mcp_")))
                .andExpect(jsonPath("$.permissions", is("READ_ONLY")));

        mockMvc.perform(get("/api/v1/mcp/api-keys")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", user.getEmail())))
                        .header("X-Tenant-Id", organization.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST /api/v1/ai/conversational/chat should execute assistant query with tool execution")
    void shouldExecuteConversationalChat() throws Exception {
        ConversationalChatRequest request = new ConversationalChatRequest(
                null,
                "Qual é o faturamento e vendas dos meus pedidos recentes?",
                "MERCADO_LIVRE",
                null
        );

        mockMvc.perform(post("/api/v1/ai/conversational/chat")
                        .with(jwt().jwt(j -> j.subject(userSub).claim("email", user.getEmail())))
                        .header("X-Tenant-Id", organization.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("Indicadores de Vendas")))
                .andExpect(jsonPath("$.toolExecutions", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.provenance.source", containsString("Assistente Contextual")));
    }
}
