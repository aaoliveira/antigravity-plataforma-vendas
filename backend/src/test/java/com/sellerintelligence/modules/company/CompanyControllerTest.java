package com.sellerintelligence.modules.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.company.model.CreateCompanyCommand;
import com.sellerintelligence.modules.company.model.LinkAccountCommand;
import com.sellerintelligence.modules.company.model.UpdateCompanyCommand;
import com.sellerintelligence.modules.company.repository.CompanyAccountLinkRepository;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyAccountLinkRepository linkRepository;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtTokenProvider.generateToken(
                USER_ID, "company-admin@sellerintelligence.com", TENANT_ID, List.of("ROLE_ADMIN")
        );
    }

    @Test
    @DisplayName("1. Fluxo REST completo de Empresa: Cadastro com CNPJ válido, consulta por ID, listagem e atualização")
    void shouldExecuteCompanyCrudFlow() throws Exception {
        CreateCompanyCommand createCmd = new CreateCompanyCommand(
                "Alfa Tech Soluções LTDA",
                "Alfa Tech",
                "33.000.167/0001-01",
                "123.456.789",
                "SIMPLES_NACIONAL",
                "Empresa Matriz"
        );

        // 1. Criar Empresa
        MvcResult result = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.corporateName").value("Alfa Tech Soluções LTDA"))
                .andExpect(jsonPath("$.tradeName").value("Alfa Tech"))
                .andExpect(jsonPath("$.cnpj").value("33000167000101"))
                .andExpect(jsonPath("$.formattedCnpj").value("33.000.167/0001-01"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        String companyId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // 2. Consultar por ID
        mockMvc.perform(get("/api/v1/companies/" + companyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId))
                .andExpect(jsonPath("$.corporateName").value("Alfa Tech Soluções LTDA"));

        // 3. Listar Empresas
        mockMvc.perform(get("/api/v1/companies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())));

        // 4. Atualizar Dados Cadastrais
        UpdateCompanyCommand updateCmd = new UpdateCompanyCommand(
                "Alfa Tech Soluções e Comércio LTDA",
                "Alfa Tech Global",
                "987.654.321",
                "LUCRO_PRESUMIDO",
                true,
                "Observação atualizada"
        );

        mockMvc.perform(put("/api/v1/companies/" + companyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corporateName").value("Alfa Tech Soluções e Comércio LTDA"))
                .andExpect(jsonPath("$.tradeName").value("Alfa Tech Global"))
                .andExpect(jsonPath("$.taxRegime").value("LUCRO_PRESUMIDO"));

        // 5. Desativar Empresa
        mockMvc.perform(delete("/api/v1/companies/" + companyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/companies/" + companyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("2. Deve rejeitar cadastro de empresa com CNPJ sintaticamente inválido (400 Bad Request)")
    void shouldRejectCompanyWithInvalidCnpj() throws Exception {
        CreateCompanyCommand createCmd = new CreateCompanyCommand(
                "Empresa Inválida",
                null,
                "11.111.111/1111-11", // Dígitos repetidos
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCmd)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3. Deve rejeitar cadastro com CNPJ duplicado no mesmo tenant (409 Conflict)")
    void shouldRejectDuplicateCnpjInSameTenant() throws Exception {
        CreateCompanyCommand cmd1 = new CreateCompanyCommand(
                "Primeira Empresa",
                null,
                "60.701.190/0001-04",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd1)))
                .andExpect(status().isCreated());

        CreateCompanyCommand cmd2 = new CreateCompanyCommand(
                "Segunda Empresa com Mesmo CNPJ",
                null,
                "60701190000104", // Mesmo CNPJ sem máscara
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cmd2)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("4. Fluxo REST de Vínculo de Contas de Marketplace: Vínculo, bloqueio ambíguo e histórico")
    void shouldExecuteAccountLinkingFlow() throws Exception {
        // 1. Criar Empresa Alpha
        CreateCompanyCommand compCmd1 = new CreateCompanyCommand(
                "Empresa Alpha Log",
                null,
                "00.000.000/0001-91",
                null,
                null,
                null
        );
        MvcResult compResult1 = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compCmd1)))
                .andExpect(status().isCreated())
                .andReturn();
        String companyId1 = objectMapper.readTree(compResult1.getResponse().getContentAsString()).get("id").asText();

        // 2. Criar Empresa Beta
        CreateCompanyCommand compCmd2 = new CreateCompanyCommand(
                "Empresa Beta Varejo",
                null,
                "07.526.557/0001-00",
                null,
                null,
                null
        );
        MvcResult compResult2 = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compCmd2)))
                .andExpect(status().isCreated())
                .andReturn();
        String companyId2 = objectMapper.readTree(compResult2.getResponse().getContentAsString()).get("id").asText();

        // 3. Cadastrar Conta de Marketplace (SellerAccount) no tenant
        SellerAccount sellerAccount = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                TENANT_ID,
                "MERCADO_LIVRE",
                "MLB_OFFICIAL_100",
                "Loja Matriz Mercado Livre",
                "ACTIVE"
        ));

        // 4. Vincular conta à Empresa Alpha (Sucesso)
        LinkAccountCommand linkCmd = new LinkAccountCommand(
                sellerAccount.getId(),
                "Vínculo inicial operacional",
                false
        );

        MvcResult linkRes = mockMvc.perform(post("/api/v1/companies/" + companyId1 + "/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(linkCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sellerAccountId").value(sellerAccount.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(companyId1))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.isActive").value(true))
                .andReturn();

        String linkId = objectMapper.readTree(linkRes.getResponse().getContentAsString()).get("id").asText();

        // 5. Tentar vincular a mesma conta à Empresa Beta sem replaceActiveLink (Bloqueio de Ambiguidade)
        LinkAccountCommand ambiguousCmd = new LinkAccountCommand(
                sellerAccount.getId(),
                "Tentativa ambígua",
                false
        );

        mockMvc.perform(post("/api/v1/companies/" + companyId2 + "/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ambiguousCmd)))
                .andExpect(status().isConflict());

        // 6. Transferir conta para Empresa Beta com replaceActiveLink = true
        LinkAccountCommand transferCmd = new LinkAccountCommand(
                sellerAccount.getId(),
                "Transferência autorizada para Beta",
                true
        );

        mockMvc.perform(post("/api/v1/companies/" + companyId2 + "/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyId").value(companyId2))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.isActive").value(true));

        // 7. Consultar histórico da conta (deve conter 2 registros: v1 encerrada e v2 ativa)
        mockMvc.perform(get("/api/v1/companies/accounts/" + sellerAccount.getId() + "/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[0].companyId").value(companyId2))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[1].version").value(1))
                .andExpect(jsonPath("$[1].companyId").value(companyId1))
                .andExpect(jsonPath("$[1].isActive").value(false));

        // 8. Desvincular explicitamente a conta
        mockMvc.perform(delete("/api/v1/companies/links/" + linkId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
