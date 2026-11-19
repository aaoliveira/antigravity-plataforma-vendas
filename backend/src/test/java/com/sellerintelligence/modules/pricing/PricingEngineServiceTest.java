package com.sellerintelligence.modules.pricing;

import com.sellerintelligence.modules.pricing.model.DeductionCategory;
import com.sellerintelligence.modules.pricing.model.FeeResolutionRequest;
import com.sellerintelligence.modules.pricing.model.FeeResolutionResult;
import com.sellerintelligence.modules.pricing.model.FeeResolutionStatus;
import com.sellerintelligence.modules.pricing.model.FeeSourceType;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.FeeValueType;
import com.sellerintelligence.modules.pricing.model.PricingCalculationRequest;
import com.sellerintelligence.modules.pricing.model.PricingCalculationResult;
import com.sellerintelligence.modules.pricing.model.ProductCostDto;
import com.sellerintelligence.modules.pricing.model.ProductMarginSimulationRequest;
import com.sellerintelligence.modules.pricing.model.ResolvedFeeItem;
import com.sellerintelligence.modules.pricing.service.FeeResolutionService;
import com.sellerintelligence.modules.pricing.service.PricingEngineService;
import com.sellerintelligence.modules.pricing.service.ProductCostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingEngineServiceTest {

    @Mock
    private ProductCostService productCostService;

    @Mock
    private FeeResolutionService feeResolutionService;

    private PricingEngineService pricingEngineService;

    @BeforeEach
    void setUp() {
        pricingEngineService = new PricingEngineService(productCostService, feeResolutionService);
    }

    @Test
    @DisplayName("1. Cenário de Referência: Cálculo completo de margem, deduções e ponto de equilíbrio com precisão BigDecimal")
    void shouldCalculateReferenceScenarioAccurately() {
        // Cenário de Referência:
        // Preço Venda: R$ 120,00
        // Custo Aquisição (CMV): R$ 45,00
        // Embalagem: R$ 3,50
        // Custo Adicional: R$ 1,50
        // Total Custos Diretos (CD): R$ 50,00
        // Comissão: 16.00% (R$ 19,20)
        // Tarifa Fixa: R$ 6,00
        // Imposto: 8.00% (R$ 9,60)
        // Outras Despesas Variáveis: 2.00% (R$ 2,40)
        // Total Deduções Variáveis (DV%): 26.00% (R$ 31,20)
        // Total Deduções Fixas (DF): R$ 6,00
        // Total Deduções: 50 + 6 + 31.20 = R$ 87,20
        // Margem de Contribuição em Valor: 120 - 87.20 = R$ 32,80
        // Margem de Contribuição Percentual: 32.80 / 120 = 27.33%
        // Ponto de Equilíbrio: (50 + 6) / (1 - 0.26) = 56 / 0.74 = R$ 75,68
        // Preço-Alvo (20% margem): 56 / (1 - 0.46) = 56 / 0.54 = R$ 103,70

        PricingCalculationRequest request = new PricingCalculationRequest(
                new BigDecimal("120.00"),
                BigDecimal.ZERO,
                new BigDecimal("45.00"),
                new BigDecimal("3.50"),
                new BigDecimal("1.50"),
                BigDecimal.ZERO, // frete subsidiado
                new BigDecimal("16.00"), // comissão %
                new BigDecimal("6.00"), // tarifa fixa
                BigDecimal.ZERO, // taxa logística
                new BigDecimal("8.00"), // imposto %
                new BigDecimal("2.00"), // outras variáveis %
                BigDecimal.ZERO, // outras fixas
                new BigDecimal("5.00"), // margem mínima %
                new BigDecimal("20.00"), // margem alvo %
                null,
                "BRL"
        );

        PricingCalculationResult result = pricingEngineService.calculateMargins(request);

        assertThat(result.grossSellingPrice()).isEqualByComparingTo("120.00");
        assertThat(result.netSellingPrice()).isEqualByComparingTo("120.00");
        assertThat(result.totalDirectCosts()).isEqualByComparingTo("50.00");
        assertThat(result.totalFixedDeductions()).isEqualByComparingTo("6.00");
        assertThat(result.totalVariableDeductions()).isEqualByComparingTo("31.20");
        assertThat(result.totalDeductions()).isEqualByComparingTo("87.20");

        // Margem de contribuição exata
        assertThat(result.contributionMarginAmount()).isEqualByComparingTo("32.80");
        assertThat(result.contributionMarginPercent()).isEqualByComparingTo("27.33");

        // Ponto de equilíbrio
        assertThat(result.isBreakEvenAttainable()).isTrue();
        assertThat(result.breakEvenPrice()).isEqualByComparingTo("75.68");

        // Preço Mínimo (5% margem): 56 / (1 - 0.31) = 56 / 0.69 = R$ 81,16
        assertThat(result.isMinPriceAttainable()).isTrue();
        assertThat(result.minPrice()).isEqualByComparingTo("81.16");

        // Preço-Alvo (20% margem)
        assertThat(result.isTargetPriceAttainable()).isTrue();
        assertThat(result.targetPrice()).isEqualByComparingTo("103.70");

        // Metadados
        assertThat(result.formulaVersion()).isEqualTo("v1.0.0");
        assertThat(result.currency()).isEqualTo("BRL");
        assertThat(result.warnings()).isEmpty();

        // Detalhamento de parcelas
        assertThat(result.deductionItems()).hasSize(7);
        assertThat(result.deductionItems())
                .extracting("category")
                .contains(
                        DeductionCategory.ACQUISITION_COST,
                        DeductionCategory.PACKAGING_COST,
                        DeductionCategory.ADDITIONAL_DIRECT_COST,
                        DeductionCategory.MARKETPLACE_COMMISSION,
                        DeductionCategory.MARKETPLACE_FIXED_FEE,
                        DeductionCategory.TAX,
                        DeductionCategory.OTHER_VARIABLE_EXPENSE
                );
    }

    @Test
    @DisplayName("2. CRITÉRIO DE ACEITE: Margem de Contribuição não pode ser rotulada como Lucro Líquido")
    void shouldNeverLabelContributionMarginAsNetProfit() {
        PricingCalculationRequest request = new PricingCalculationRequest(
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("40.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("15.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null, null, null, "BRL"
        );

        PricingCalculationResult result = pricingEngineService.calculateMargins(request);

        // A terminologia formal deve ser estritamente Contribution Margin
        assertThat(result.contributionMarginAmount()).isNotNull();
        assertThat(result.contributionMarginPercent()).isNotNull();

        // As premissas devem alertar expressamente que despesas fixas estruturais não foram deduzidas
        assertThat(result.assumptions())
                .anyMatch(a -> a.contains("não representa Lucro Líquido"));
    }

    @Test
    @DisplayName("3. CRITÉRIO DE ACEITE: Divisão por Zero tratada fail-safe quando receita líquida <= 0")
    void shouldHandleZeroOrNegativeNetRevenueWithoutArithmeticException() {
        // Preço R$ 100 com desconto R$ 100 -> Receita Líquida = R$ 0,00
        PricingCalculationRequest request = new PricingCalculationRequest(
                new BigDecimal("100.00"),
                new BigDecimal("100.00"), // Desconto integral
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("16.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null, null, null, "BRL"
        );

        PricingCalculationResult result = pricingEngineService.calculateMargins(request);

        assertThat(result.netSellingPrice()).isEqualByComparingTo("0.00");
        assertThat(result.contributionMarginAmount()).isEqualByComparingTo("-30.00");
        assertThat(result.contributionMarginPercent()).isEqualByComparingTo("0.00"); // Tratamento fail-safe
        assertThat(result.warnings())
                .anyMatch(w -> w.contains("Receita líquida de venda menor ou igual a zero"));
    }

    @Test
    @DisplayName("4. CRITÉRIO DE ACEITE: Tratamento de Ponto de Equilíbrio inatingível quando deduções variáveis >= 100%")
    void shouldHandleAttainabilityWhenVariableDeductionsEqualOrExceed100Percent() {
        // Comissão 60% + Imposto 35% + Outras despesas 10% = 105% (>= 100%)
        PricingCalculationRequest request = new PricingCalculationRequest(
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("20.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("60.00"),
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                new BigDecimal("35.00"),
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null, null, null, "BRL"
        );

        PricingCalculationResult result = pricingEngineService.calculateMargins(request);

        assertThat(result.isBreakEvenAttainable()).isFalse();
        assertThat(result.breakEvenPrice()).isNull();
        assertThat(result.warnings())
                .anyMatch(w -> w.contains("Ponto de equilíbrio inatingível"));
    }

    @Test
    @DisplayName("5. Preço-Alvo por Valor Monetário Fixo (Target Margin Amount)")
    void shouldCalculateTargetPriceByFixedMarginAmount() {
        // Custos Diretos: R$ 40,00
        // Deduções Fixas: R$ 5,00
        // Deduções Variáveis: 20%
        // Margem Alvo Monetária: R$ 25,00
        // P_target = (40 + 5 + 25) / (1 - 0.20) = 70 / 0.80 = R$ 87,50
        PricingCalculationRequest request = new PricingCalculationRequest(
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("40.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("15.00"),
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null, // targetMarginPercent nulo
                new BigDecimal("25.00"), // targetMarginAmount fixo
                "BRL"
        );

        PricingCalculationResult result = pricingEngineService.calculateMargins(request);

        assertThat(result.isTargetPriceAttainable()).isTrue();
        assertThat(result.targetPrice()).isEqualByComparingTo("87.50");
    }

    @Test
    @DisplayName("6. CRITÉRIO DE ACEITE: Simulação Integrada gera aviso ostensivo quando taxas obrigatórias estão ausentes")
    void shouldGenerateWarningWhenIntegratingWithMissingMarketplaceFees() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductCostDto mockCost = new ProductCostDto(
                UUID.randomUUID(),
                tenantId,
                productId,
                null,
                null,
                "BRL",
                "UN",
                new BigDecimal("50.00"),
                new BigDecimal("3.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("53.00"),
                new BigDecimal("8.00"),
                new BigDecimal("2.00"),
                Instant.now().minusSeconds(86400),
                null,
                1,
                true,
                "Admin",
                "Lote teste",
                Instant.now(),
                Instant.now()
        );

        when(productCostService.getEffectiveCost(eq(tenantId), eq(productId), any(), any(), any()))
                .thenReturn(Optional.of(mockCost));

        // Marketplace Shopee sem taxas cadastradas -> missing COMMISSION
        FeeResolutionResult missingFeeResult = new FeeResolutionResult(
                "SHOPEE",
                "standard",
                "Eletrônicos",
                new BigDecimal("100.00"),
                "BRL",
                List.of(),
                BigDecimal.ZERO,
                true, // hasMissingFees
                List.of(FeeType.COMMISSION),
                FeeResolutionStatus.MISSING,
                "Nenhuma regra cadastrada"
        );

        when(feeResolutionService.resolveFees(eq(tenantId), any(FeeResolutionRequest.class)))
                .thenReturn(missingFeeResult);

        ProductMarginSimulationRequest simRequest = new ProductMarginSimulationRequest(
                productId,
                null,
                null,
                "SHOPEE",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                "standard",
                "Eletrônicos",
                Instant.now(),
                null, null, null
        );

        PricingCalculationResult result = pricingEngineService.calculateWithEffectiveFees(tenantId, simRequest);

        // Deve calcular com as taxas conhecidas (sem inventar comissão) mas sinalizar aviso ostensivo
        assertThat(result.warnings())
                .anyMatch(w -> w.contains("SHOPEE possui taxas obrigatórias ausentes ([COMMISSION])")
                        && w.contains("não inventou percentuais"));
    }

    @Test
    @DisplayName("7. Rejeição com falha fail-closed quando produto não possui custos cadastrados")
    void shouldThrowExceptionWhenProductHasNoRegisteredCosts() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(productCostService.getEffectiveCost(eq(tenantId), eq(productId), any(), any(), any()))
                .thenReturn(Optional.empty());

        ProductMarginSimulationRequest simRequest = new ProductMarginSimulationRequest(
                productId,
                null,
                null,
                "MERCADO_LIVRE",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                "gold_pro",
                null,
                Instant.now(),
                null, null, null
        );

        assertThatThrownBy(() -> pricingEngineService.calculateWithEffectiveFees(tenantId, simRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nenhum custo cadastrado para o produto");
    }

    @Test
    @DisplayName("8. Sinalização de uso de regra manual não oficial")
    void shouldWarnWhenManualFeeIsApplied() {
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ProductCostDto mockCost = new ProductCostDto(
                UUID.randomUUID(),
                tenantId,
                productId,
                null,
                null,
                "BRL",
                "UN",
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Instant.now().minusSeconds(86400),
                null,
                1,
                true,
                "Admin",
                "Lote teste",
                Instant.now(),
                Instant.now()
        );

        when(productCostService.getEffectiveCost(eq(tenantId), eq(productId), any(), any(), any()))
                .thenReturn(Optional.of(mockCost));

        FeeResolutionResult manualFeeResult = new FeeResolutionResult(
                "MERCADO_LIVRE",
                "gold_pro",
                null,
                new BigDecimal("100.00"),
                "BRL",
                List.of(new ResolvedFeeItem(
                        UUID.randomUUID(),
                        FeeType.COMMISSION,
                        FeeValueType.PERCENTAGE,
                        new BigDecimal("15.00"),
                        null,
                        new BigDecimal("15.00"),
                        "BRL",
                        FeeSourceType.USER_CONFIGURED,
                        "Anotação em reunião",
                        1
                )),
                new BigDecimal("15.00"),
                false,
                List.of(),
                FeeResolutionStatus.COMPLETE,
                null
        );

        when(feeResolutionService.resolveFees(eq(tenantId), any(FeeResolutionRequest.class)))
                .thenReturn(manualFeeResult);

        ProductMarginSimulationRequest simRequest = new ProductMarginSimulationRequest(
                productId,
                null,
                null,
                "MERCADO_LIVRE",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                "gold_pro",
                null,
                Instant.now(),
                null, null, null
        );

        PricingCalculationResult result = pricingEngineService.calculateWithEffectiveFees(tenantId, simRequest);

        assertThat(result.warnings())
                .anyMatch(w -> w.contains("Uma ou mais taxas aplicadas utilizam regras manuais não oficiais"));
    }
}
