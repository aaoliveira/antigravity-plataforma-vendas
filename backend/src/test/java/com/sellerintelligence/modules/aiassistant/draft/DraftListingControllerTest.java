package com.sellerintelligence.modules.aiassistant.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftApproveRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftEditRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftGenerationRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftRejectRequest;
import com.sellerintelligence.modules.aiassistant.draft.model.DraftTone;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("DraftListingController - Testes de Integração REST e Contratos de Rascunhos de Anúncios (P020-BE)")
class DraftListingControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtTokenProvider.generateToken(
                USER_ID, "ai-drafts@sellerintelligence.com", TENANT_ID, List.of("ROLE_ADMIN")
        );
    }

    @Test
    @DisplayName("Requisição não autenticada deve ser rejeitada com 401 Unauthorized (Fail-Closed)")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/ai/drafts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve consultar o catálogo de regras oficiais e limites por canal homologado")
    void shouldListMarketplaceRules() throws Exception {
        mockMvc.perform(get("/api/v1/ai/drafts/rules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.marketplace == 'MERCADO_LIVRE')].maxTitleLength").value(60))
                .andExpect(jsonPath("$[?(@.marketplace == 'SHOPEE')].maxTitleLength").value(120));
    }

    @Test
    @DisplayName("Deve gerar rascunho com sucesso com status DRAFT e sem publicação externa")
    void shouldGenerateDraftSuccessfully() throws Exception {
        DraftGenerationRequest request = new DraftGenerationRequest(
                null,
                null,
                "MERCADO_LIVRE",
                DraftTone.INFORMATIVE_OBJECTIVE,
                "Tramontina",
                "Solar",
                "Panela",
                List.of("Aço Inox"),
                Map.of("diâmetro", "20cm"),
                "90 dias legal",
                null,
                List.of("Durável"),
                List.of("cozinha", "fogão"),
                null,
                null
        );

        mockMvc.perform(post("/api/v1/ai/drafts/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.targetMarketplace", is("MERCADO_LIVRE")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.currentVersionNumber", is(1)))
                .andExpect(jsonPath("$.publicationStatus", is("NOT_PUBLISHED_DRAFT_ONLY")))
                .andExpect(jsonPath("$.externalPublishBlocked", is(true)))
                .andExpect(jsonPath("$.currentVersion.generatedTitle", notNullValue()))
                .andExpect(jsonPath("$.currentVersion.characterLimitValid", is(true)))
                .andExpect(jsonPath("$.currentVersion.userEdited", is(false)));
    }

    @Test
    @DisplayName("Deve rejeitar geração quando marketplace não possui regras validadas")
    void shouldRejectGenerationWithUnvalidatedMarketplace() throws Exception {
        DraftGenerationRequest request = new DraftGenerationRequest(
                null,
                null,
                "TIKTOK_SHOP",
                DraftTone.INFORMATIVE_OBJECTIVE,
                "Marca",
                "Modelo",
                "Categoria",
                null,
                null,
                null,
                null,
                null,
                List.of("kw"),
                null,
                null
        );

        mockMvc.perform(post("/api/v1/ai/drafts/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", containsString("Limites de Caracteres Violados")))
                .andExpect(jsonPath("$.detail", containsString("não possui regras oficiais")));
    }

    @Test
    @DisplayName("Deve executar ciclo completo: gerar -> editar -> aprovar sem publicação externa")
    void shouldExecuteFullDraftLifecycle() throws Exception {
        // 1. Gerar
        DraftGenerationRequest genReq = new DraftGenerationRequest(
                null,
                null,
                "MERCADO_LIVRE",
                DraftTone.COMMERCIAL_POLITE,
                "Bosch",
                "GSR 120",
                "Parafusadeira",
                List.of("Aço e Borracha"),
                Map.of("voltagem", "12V"),
                "12 meses legal",
                null,
                null,
                List.of("bateria"),
                null,
                null
        );

        MvcResult genResult = mockMvc.perform(post("/api/v1/ai/drafts/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String genJson = genResult.getResponse().getContentAsString();
        String draftId = objectMapper.readTree(genJson).get("id").asText();

        // 2. Editar título e descrição
        DraftEditRequest editReq = new DraftEditRequest(
                "Parafusadeira Bosch GSR 120 12V Bateria Original",
                "Descrição completa com todos os atributos confirmados da Bosch.",
                "Ajuste comercial para Mercado Livre"
        );

        mockMvc.perform(put("/api/v1/ai/drafts/" + draftId + "/edit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersionNumber", is(2)))
                .andExpect(jsonPath("$.status", is("REVIEWED")))
                .andExpect(jsonPath("$.currentVersion.userEdited", is(true)))
                .andExpect(jsonPath("$.currentVersion.effectiveTitle", is(editReq.reviewedTitle())));

        // 3. Consultar histórico
        mockMvc.perform(get("/api/v1/ai/drafts/" + draftId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionHistory", hasSize(2)));

        // 4. Aprovar rascunho
        DraftApproveRequest approveReq = new DraftApproveRequest("Homologado internamente");
        mockMvc.perform(post("/api/v1/ai/drafts/" + draftId + "/approve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.publicationStatus", is("NOT_PUBLISHED_DRAFT_ONLY")))
                .andExpect(jsonPath("$.externalPublishBlocked", is(true)));
    }

    @Test
    @DisplayName("Deve bloquear edição com claim não comprovada retornando HTTP 422 Unprocessable Entity")
    void shouldRejectEditWithUnprovenClaims() throws Exception {
        DraftGenerationRequest genReq = new DraftGenerationRequest(
                null,
                null,
                "MERCADO_LIVRE",
                DraftTone.INFORMATIVE_OBJECTIVE,
                "Genérico",
                "Mod1",
                "Cabo",
                null,
                null,
                null,
                null,
                null,
                List.of("usb"),
                null,
                null
        );

        MvcResult genResult = mockMvc.perform(post("/api/v1/ai/drafts/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String draftId = objectMapper.readTree(genResult.getResponse().getContentAsString()).get("id").asText();

        DraftEditRequest badEdit = new DraftEditRequest(
                "Cabo Certificado pelo Inmetro",
                "Cura calvície e tem garantia vitalícia.",
                "Tentativa de injeção de claims"
        );

        mockMvc.perform(put("/api/v1/ai/drafts/" + draftId + "/edit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badEdit)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title", containsString("Claims Não Comprovadas")))
                .andExpect(jsonPath("$.violations", notNullValue()));
    }
}
