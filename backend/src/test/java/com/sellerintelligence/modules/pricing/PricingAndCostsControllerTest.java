package com.sellerintelligence.modules.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.pricing.model.FeeResolutionRequest;
import com.sellerintelligence.modules.pricing.model.FeeSourceType;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.FeeValueType;
import com.sellerintelligence.modules.pricing.model.RegisterFeeRuleCommand;
import com.sellerintelligence.modules.pricing.model.RegisterProductCostCommand;
import com.sellerintelligence.modules.pricing.model.PricingCalculationRequest;
import com.sellerintelligence.modules.pricing.model.ProductMarginSimulationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PricingAndCostsControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

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
                USER_ID, "pricing-admin@sellerintelligence.com", TENANT_ID, List.of("ROLE_ADMIN")
        );
    }

    @Test
    @DisplayName("1. Fluxo REST completo de Custos: Cadastro, Atualização de Versão, Consulta Vigente e Histórico Preservado")
    void shouldExecuteFullProductCostFlow() throws Exception {
        UUID productId = UUID.randomUUID();

        // 1. Cadastrar Custo Inicial v1
        RegisterProductCostCommand v1Cmd = new RegisterProductCostCommand(
                productId,
                null,
                null,
                "BRL",
                "UN",
                new BigDecimal("120.0000"), // aquisição
                new BigDecimal("5.0000"),   // embalagem
                new BigDecimal("2.5000"),   // adicional
                new BigDecimal("10.0000"),  // frete subsidiado
                new BigDecimal("8.0000"),   // imposto %
                new BigDecimal("3.0000"),   // despesas %
                Instant.now().minusSeconds(86400),
                "Lote de lançamento"
        );

        mockMvc.perform(post("/api/v1/pricing/costs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v1Cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.acquisitionCost").value(120.0))
                .andExpect(jsonPath("$.totalDirectUnitCost").value(137.5)); // 120 + 5 + 2.5 + 10

        // 2. Consultar Custo Vigente -> deve retornar versão 1
        mockMvc.perform(get("/api/v1/pricing/costs/effective")
                        .header("Authorization", "Bearer " + token)
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.acquisitionCost").value(120.0));

        // 3. Atualizar Custo para nova versão v2
        RegisterProductCostCommand v2Cmd = new RegisterProductCostCommand(
                productId,
                null,
                null,
                "BRL",
                "UN",
                new BigDecimal("130.0000"), // reajuste
                new BigDecimal("5.0000"),
                new BigDecimal("2.5000"),
                new BigDecimal("10.0000"),
                new BigDecimal("8.0000"),
                new BigDecimal("3.0000"),
                Instant.now(),
                "Reajuste inflacionário"
        );

        mockMvc.perform(post("/api/v1/pricing/costs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v2Cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.acquisitionCost").value(130.0))
                .andExpect(jsonPath("$.totalDirectUnitCost").value(147.5));

        // 4. Consultar Custo Vigente -> agora retorna versão 2
        mockMvc.perform(get("/api/v1/pricing/costs/effective")
                        .header("Authorization", "Bearer " + token)
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.acquisitionCost").value(130.0));

        // 5. Histórico Preservado: deve retornar as 2 versões cadastradas sem perdas
        mockMvc.perform(get("/api/v1/pricing/costs/history")
                        .header("Authorization", "Bearer " + token)
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].version").value(2))
                .andExpect(jsonPath("$[1].version").value(1));
    }

    @Test
    @DisplayName("2. Deve retornar 404 ao consultar custo efetivo de produto sem custos cadastrados (ausência explícita)")
    void shouldReturn404WhenNoCostRegistered() throws Exception {
        UUID uncostedProductId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/pricing/costs/effective")
                        .header("Authorization", "Bearer " + token)
                        .param("productId", uncostedProductId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("3. Fluxo REST de Regras de Tarifas: Cadastro com fonte, listagem e resolução determinística")
    void shouldExecuteFeeRuleRegistrationAndResolution() throws Exception {
        // 1. Cadastrar regra de comissão de 13% no Mercado Livre
        RegisterFeeRuleCommand commissionCmd = new RegisterFeeRuleCommand(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                FeeType.COMMISSION,
                FeeValueType.PERCENTAGE,
                new BigDecimal("13.0000"),
                null,
                "BRL",
                null,
                null,
                FeeSourceType.OFFICIAL_CONTRACT,
                "Tabela Oficial Mercado Livre 2026 - Clássico",
                "Comissão padrão da categoria",
                Instant.now().minusSeconds(7200)
        );

        mockMvc.perform(post("/api/v1/pricing/fee-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commissionCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.marketplace").value("MERCADO_LIVRE"))
                .andExpect(jsonPath("$.percentageRate").value(13.0));

        // 2. Cadastrar regra de tarifa fixa de R$ 6,00 para itens <= R$ 79,00
        RegisterFeeRuleCommand fixedCmd = new RegisterFeeRuleCommand(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                FeeType.FIXED_FEE,
                FeeValueType.FIXED_AMOUNT,
                null,
                new BigDecimal("6.0000"),
                "BRL",
                BigDecimal.ZERO,
                new BigDecimal("79.0000"),
                FeeSourceType.OFFICIAL_CONTRACT,
                "Tabela Oficial Mercado Livre 2026 - Tarifa Fixa",
                null,
                Instant.now().minusSeconds(7200)
        );

        mockMvc.perform(post("/api/v1/pricing/fee-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fixedCmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fixedAmount").value(6.0));

        // 3. Listar Regras do Canal
        mockMvc.perform(get("/api/v1/pricing/fee-rules")
                        .header("Authorization", "Bearer " + token)
                        .param("marketplace", "MERCADO_LIVRE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));

        // 4. Resolver Taxas para Preço R$ 50,00 (13% de 50 = 6.50 + 6.00 fixo = R$ 12.50)
        FeeResolutionRequest resolveReq = new FeeResolutionRequest(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                new BigDecimal("50.0000"),
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/pricing/fee-rules/resolve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutionStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.hasMissingFees").value(false))
                .andExpect(jsonPath("$.totalFeeAmount").value(12.5))
                .andExpect(jsonPath("$.resolvedFees", hasSize(2)));
    }

    @Test
    @DisplayName("4. MANDATO ANTI-ALUCINAÇÃO: Resolução para canal não configurado sinaliza explicitamente ausência de taxas")
    void shouldReturnMissingStatusForUnconfiguredMarketplace() throws Exception {
        FeeResolutionRequest unconfiguredReq = new FeeResolutionRequest(
                "MAGALU",
                "PADRAO",
                null,
                new BigDecimal("100.0000"),
                Instant.now()
        );

        mockMvc.perform(post("/api/v1/pricing/fee-rules/resolve")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unconfiguredReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolutionStatus").value("MISSING"))
                .andExpect(jsonPath("$.hasMissingFees").value(true))
                .andExpect(jsonPath("$.missingFeeTypes[0]").value("COMMISSION"))
                .andExpect(jsonPath("$.userActionRequired").value(containsString("Nenhuma regra de comissão validada")));
    }

    @Test
    @DisplayName("5. Endpoint REST: Cálculo determinístico de margem com parâmetros explícitos")
    void shouldCalculateMarginsViaRestEndpoint() throws Exception {
        PricingCalculationRequest request = new PricingCalculationRequest(
                new BigDecimal("120.00"),
                BigDecimal.ZERO,
                new BigDecimal("50.00"),
                new BigDecimal("4.00"),
                new BigDecimal("2.00"),
                BigDecimal.ZERO,
                new BigDecimal("16.00"),
                new BigDecimal("6.00"),
                BigDecimal.ZERO,
                new BigDecimal("8.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("5.00"),
                new BigDecimal("20.00"),
                null,
                "BRL"
        );

        mockMvc.perform(post("/api/v1/pricing/margins/calculate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossSellingPrice").value(120.0))
                .andExpect(jsonPath("$.totalDirectCosts").value(56.0))
                .andExpect(jsonPath("$.formulaVersion").value("v1.0.0"))
                .andExpect(jsonPath("$.isBreakEvenAttainable").value(true))
                .andExpect(jsonPath("$.breakEvenPrice").isNumber())
                .andExpect(jsonPath("$.contributionMarginAmount").isNumber())
                .andExpect(jsonPath("$.deductionItems", hasSize(greaterThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("6. Endpoint REST: Simulação combinada de produto com custo cadastrado e regras ativas de taxas")
    void shouldSimulateProductMarginViaRestEndpoint() throws Exception {
        UUID productId = UUID.randomUUID();

        // 1. Cadastra custo do produto
        RegisterProductCostCommand costCmd = new RegisterProductCostCommand(
                productId,
                null,
                null,
                "BRL",
                "UN",
                new BigDecimal("60.00"),
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("7.00"),
                BigDecimal.ZERO,
                Instant.now().minusSeconds(3600),
                "Custo para simulação"
        );

        mockMvc.perform(post("/api/v1/pricing/costs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costCmd)))
                .andExpect(status().isCreated());

        // 2. Cadastra regra de taxa no canal MERCADO_LIVRE
        RegisterFeeRuleCommand ruleCmd = new RegisterFeeRuleCommand(
                "MERCADO_LIVRE",
                "gold_pro",
                null,
                FeeType.COMMISSION,
                FeeValueType.PERCENTAGE,
                new BigDecimal("15.00"),
                null,
                "BRL",
                null,
                null,
                FeeSourceType.OFFICIAL_CONTRACT,
                "https://mercadolivre.com.br/tarifas",
                "Comissão padrão",
                Instant.now().minusSeconds(86400)
        );

        mockMvc.perform(post("/api/v1/pricing/fee-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleCmd)))
                .andExpect(status().isCreated());

        // 3. Simula margem do produto
        ProductMarginSimulationRequest simRequest = new ProductMarginSimulationRequest(
                productId,
                null,
                null,
                "MERCADO_LIVRE",
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                "gold_pro",
                null,
                Instant.now(),
                new BigDecimal("5.00"),
                new BigDecimal("15.00"),
                null
        );

        mockMvc.perform(post("/api/v1/pricing/margins/simulate-product")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(simRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossSellingPrice").value(150.0))
                .andExpect(jsonPath("$.totalDirectCosts").value(65.0)) // 60 + 5
                .andExpect(jsonPath("$.isBreakEvenAttainable").value(true))
                .andExpect(jsonPath("$.contributionMarginAmount").isNumber());
    }
}
