package com.sellerintelligence.modules.productmonitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.productmonitoring.model.AddMonitoredItemRequest;
import com.sellerintelligence.modules.productmonitoring.model.CreateMonitoringGroupRequest;
import com.sellerintelligence.modules.productmonitoring.model.TriggerSnapshotRequest;
import com.sellerintelligence.modules.productmonitoring.model.UpdateMonitoringGroupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductMonitoringControllerTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_BETA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String tokenAlpha;
    private String tokenBeta;

    @BeforeEach
    void setUp() {
        tokenAlpha = jwtTokenProvider.generateToken(USER_ID, "alpha-admin@sellerintelligence.com", TENANT_ALPHA_ID, List.of("ROLE_ADMIN"));
        tokenBeta = jwtTokenProvider.generateToken(USER_ID, "beta-admin@sellerintelligence.com", TENANT_BETA_ID, List.of("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Ciclo completo de CRUD de Grupo, Itens e Snapshots via Endpoints REST")
    void shouldExecuteFullMonitoringFlow() throws Exception {
        // 1. Criar Grupo
        CreateMonitoringGroupRequest createGroupReq = new CreateMonitoringGroupRequest(
                "Linha Galaxy S23",
                "Concorrentes diretos do Galaxy S23",
                "#3B82F6",
                30,
                90
        );

        MvcResult groupResult = mockMvc.perform(post("/api/v1/monitoring/groups")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGroupReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Linha Galaxy S23")))
                .andExpect(jsonPath("$.minIntervalMinutes", is(30)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn();

        Map<?, ?> groupMap = objectMapper.readValue(groupResult.getResponse().getContentAsString(), Map.class);
        String groupId = (String) groupMap.get("id");

        // 2. Listar Grupos do Tenant Alpha
        mockMvc.perform(get("/api/v1/monitoring/groups")
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[?(@.id == '" + groupId + "')].name", contains("Linha Galaxy S23")));

        // 3. Atualizar Grupo
        UpdateMonitoringGroupRequest updateReq = new UpdateMonitoringGroupRequest(
                "Linha Galaxy S23 Premium",
                "Descrição atualizada",
                "#10B981",
                45,
                120,
                null
        );

        mockMvc.perform(put("/api/v1/monitoring/groups/" + groupId)
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Linha Galaxy S23 Premium")))
                .andExpect(jsonPath("$.minIntervalMinutes", is(45)));

        // 4. Adicionar Item ao Grupo
        AddMonitoredItemRequest addItemReq = new AddMonitoredItemRequest(
                "MERCADO_LIVRE",
                "POLICY_MELI_OFFICIAL",
                "MLB-999888111",
                "Galaxy S23 128GB Preto Lacrado",
                new BigDecimal("3399.00"),
                "BRL",
                "https://mercadolivre.com.br/item/999888111",
                "Vendedor Líder",
                "NEW",
                45
        );

        MvcResult itemResult = mockMvc.perform(post("/api/v1/monitoring/groups/" + groupId + "/items")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.externalListingId", is("MLB-999888111")))
                .andExpect(jsonPath("$.initialPrice", is(3399.00)))
                .andExpect(jsonPath("$.firstSnapshotAt", nullValue())) // Histórico só começa no 1º snapshot após inclusão
                .andReturn();

        Map<?, ?> itemMap = objectMapper.readValue(itemResult.getResponse().getContentAsString(), Map.class);
        String itemId = (String) itemMap.get("id");

        // 5. Listar Itens do Grupo
        mockMvc.perform(get("/api/v1/monitoring/groups/" + groupId + "/items")
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(itemId)));

        // 6. Disparar Primeiro Snapshot Efetivo
        TriggerSnapshotRequest triggerReq = new TriggerSnapshotRequest(
                new BigDecimal("3299.00"),
                new BigDecimal("0.00"),
                15,
                "ACTIVE",
                "Snapshot manual de teste"
        );

        mockMvc.perform(post("/api/v1/monitoring/items/" + itemId + "/trigger-snapshot")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(triggerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.snapshot.price", is(3299.00)))
                .andExpect(jsonPath("$.snapshot.isEffectiveSnapshot", is(true)));

        // 7. Consultar Histórico de Snapshots
        mockMvc.perform(get("/api/v1/monitoring/items/" + itemId + "/snapshots")
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].price", is(3299.00)));

        // 8. Consultar Auditoria de Execuções de Jobs
        mockMvc.perform(get("/api/v1/monitoring/executions")
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].executionStatus", is("SUCCESS")));

        // 9. Remover Item do Grupo
        mockMvc.perform(delete("/api/v1/monitoring/groups/" + groupId + "/items/" + itemId)
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isNoContent());

        // 10. Remover Grupo
        mockMvc.perform(delete("/api/v1/monitoring/groups/" + groupId)
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Rejeição de Acesso sem Autenticação (401)")
    void shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/groups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rejeição de Item com Fonte Proibida (422 BusinessRuleException)")
    void shouldRejectForbiddenSource() throws Exception {
        // Cria grupo
        CreateMonitoringGroupRequest createGroupReq = new CreateMonitoringGroupRequest(
                "Grupo Teste", "Desc", "#3B82F6", 30, 90
        );
        MvcResult res = mockMvc.perform(post("/api/v1/monitoring/groups")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createGroupReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> gMap = objectMapper.readValue(res.getResponse().getContentAsString(), Map.class);
        String groupId = (String) gMap.get("id");

        // Tenta adicionar item com scraper proibido
        AddMonitoredItemRequest badItemReq = new AddMonitoredItemRequest(
                "MERCADO_LIVRE",
                "UNLICENSED_SCRAPER",
                "MLB-BAD-01",
                "Item Proibido",
                new BigDecimal("100.00"),
                "BRL",
                null, null, "NEW", 30
        );

        mockMvc.perform(post("/api/v1/monitoring/groups/" + groupId + "/items")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badItemReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title", is("Violação de Regra de Negócio")));
    }

    @Test
    @DisplayName("P013-BE: Snapshots Idempotentes e Detecção de Alterações via API REST")
    void shouldHandleIdempotentSnapshotAndChangeDetectionViaRest() throws Exception {
        // 1. Criar Grupo
        CreateMonitoringGroupRequest groupReq = new CreateMonitoringGroupRequest(
                "Grupo Idempotência", "Desc", "#3B82F6", 15, 90
        );
        MvcResult groupRes = mockMvc.perform(post("/api/v1/monitoring/groups")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String groupId = (String) objectMapper.readValue(groupRes.getResponse().getContentAsString(), Map.class).get("id");

        // 2. Criar Item
        AddMonitoredItemRequest itemReq = new AddMonitoredItemRequest(
                "MERCADO_LIVRE", "POLICY_MELI_OFFICIAL", "MLB-IDEM-01",
                "Item Idempotente", new BigDecimal("100.00"), "BRL",
                "https://mercadolivre.com.br/item/IDEM-01", "Seller", "NEW", 15
        );
        MvcResult itemRes = mockMvc.perform(post("/api/v1/monitoring/groups/" + groupId + "/items")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String itemId = (String) objectMapper.readValue(itemRes.getResponse().getContentAsString(), Map.class).get("id");

        // 3. Disparar Primeiro Snapshot com chave 'key-001'
        TriggerSnapshotRequest snap1Req = new TriggerSnapshotRequest(
                new BigDecimal("100.00"), BigDecimal.ZERO, 10, "ACTIVE",
                "Snapshot 1", "key-001", "https://mercadolivre.com.br/item/IDEM-01",
                "OFFICIAL_API", null, false
        );
        MvcResult snap1Res = mockMvc.perform(post("/api/v1/monitoring/items/" + itemId + "/trigger-snapshot")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(snap1Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.snapshot.changeType", is("INITIAL")))
                .andExpect(jsonPath("$.snapshot.hasPriceChange", is(false)))
                .andExpect(jsonPath("$.snapshot.idempotencyKey", is("key-001")))
                .andReturn();
        Map<?, ?> snap1Map = (Map<?, ?>) objectMapper.readValue(snap1Res.getResponse().getContentAsString(), Map.class).get("snapshot");
        String snap1Id = (String) snap1Map.get("id");

        // 4. Re-enviar o mesmo snapshot com 'key-001' (deve ser idempotente e retornar o mesmo ID)
        mockMvc.perform(post("/api/v1/monitoring/items/" + itemId + "/trigger-snapshot")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(snap1Req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.snapshot.id", is(snap1Id)))
                .andExpect(jsonPath("$.snapshot.idempotencyKey", is("key-001")));

        // 5. Gravar Segundo Snapshot com preço 125.00 e chave 'key-002' (Aumento de preço detectado)
        TriggerSnapshotRequest snap2Req = new TriggerSnapshotRequest(
                new BigDecimal("125.00"), BigDecimal.ZERO, 8, "ACTIVE",
                "Snapshot 2 com aumento", "key-002", "https://mercadolivre.com.br/item/IDEM-01",
                "OFFICIAL_API", null, false
        );
        mockMvc.perform(post("/api/v1/monitoring/items/" + itemId + "/snapshots")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(snap2Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.changeType", is("PRICE_INCREASE")))
                .andExpect(jsonPath("$.hasPriceChange", is(true)))
                .andExpect(jsonPath("$.priceChangeAmount", is(25.0000)))
                .andExpect(jsonPath("$.priceChangePercent", is(25.0000)))
                .andExpect(jsonPath("$.hasAvailabilityChange", is(true)))
                .andExpect(jsonPath("$.previousSnapshotId", is(snap1Id)));

        // 6. Consultar lista de snapshots históricos do item
        mockMvc.perform(get("/api/v1/monitoring/items/" + itemId + "/snapshots")
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].price", is(125.00)))
                .andExpect(jsonPath("$[0].changeType", is("PRICE_INCREASE")))
                .andExpect(jsonPath("$[1].price", is(100.00)))
                .andExpect(jsonPath("$[1].changeType", is("INITIAL")));
    }

    @Test
    @DisplayName("P013-BE: Fail-Closed e Auditoria quando Fonte Externa estiver Indisponível")
    void shouldFailClosedAndRecordExecutionWhenSourceIsUnavailable() throws Exception {
        // 1. Criar Grupo e Item
        CreateMonitoringGroupRequest groupReq = new CreateMonitoringGroupRequest(
                "Grupo Resiliência", "Desc", "#10B981", 15, 90
        );
        MvcResult groupRes = mockMvc.perform(post("/api/v1/monitoring/groups")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(groupReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String groupId = (String) objectMapper.readValue(groupRes.getResponse().getContentAsString(), Map.class).get("id");

        AddMonitoredItemRequest itemReq = new AddMonitoredItemRequest(
                "MERCADO_LIVRE", "POLICY_MELI_OFFICIAL", "MLB-RESIL-01",
                "Item Resiliente", new BigDecimal("200.00"), "BRL",
                "https://mercadolivre.com.br/item/RESIL-01", "Seller", "NEW", 15
        );
        MvcResult itemRes = mockMvc.perform(post("/api/v1/monitoring/groups/" + groupId + "/items")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String itemId = (String) objectMapper.readValue(itemRes.getResponse().getContentAsString(), Map.class).get("id");

        // 2. Disparar Snapshot simulando fonte indisponível
        TriggerSnapshotRequest failReq = new TriggerSnapshotRequest(
                new BigDecimal("210.00"), BigDecimal.ZERO, 5, "ACTIVE",
                "Tentativa com fonte indisponível", "fail-key-01", null, null, null, true
        );

        mockMvc.perform(post("/api/v1/monitoring/items/" + itemId + "/trigger-snapshot")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FAILED_SOURCE_UNAVAILABLE")))
                .andExpect(jsonPath("$.skipReason", containsString("FAILED_SOURCE_UNAVAILABLE")))
                .andExpect(jsonPath("$.snapshot", nullValue()));

        // 3. Nenhum snapshot deve ter sido inserido no histórico
        mockMvc.perform(get("/api/v1/monitoring/items/" + itemId + "/snapshots")
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 4. Execução com falha deve constar no log de execuções
        mockMvc.perform(get("/api/v1/monitoring/executions")
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].executionStatus", is("FAILED_SOURCE_UNAVAILABLE")));

        // 5. Chamada direta ao endpoint de gravação de snapshots deve responder HTTP 503
        mockMvc.perform(post("/api/v1/monitoring/items/" + itemId + "/snapshots")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title", is("Fonte de Dados Indisponível")));
    }
}
