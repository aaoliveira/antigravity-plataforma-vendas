package com.sellerintelligence.modules.pricing;

import com.sellerintelligence.modules.pricing.entity.FeeRuleRecord;
import com.sellerintelligence.modules.pricing.model.FeeResolutionRequest;
import com.sellerintelligence.modules.pricing.model.FeeResolutionResult;
import com.sellerintelligence.modules.pricing.model.FeeResolutionStatus;
import com.sellerintelligence.modules.pricing.model.FeeSourceType;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.FeeValueType;
import com.sellerintelligence.modules.pricing.repository.FeeRuleRepository;
import com.sellerintelligence.modules.pricing.service.FeeResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeResolutionServiceTest {

    @Mock
    private FeeRuleRepository feeRuleRepository;

    private FeeResolutionService feeResolutionService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        feeResolutionService = new FeeResolutionService(feeRuleRepository);
    }

    @Test
    @DisplayName("1. Deve resolver taxas completas (comissão + taxa fixa) para preço abaixo do teto de isenção de tarifa fixa")
    void shouldResolveCompleteFeesBelowPriceThreshold() {
        Instant now = Instant.now();

        // Regra 1: Comissão 13% para Mercado Livre Clássico
        FeeRuleRecord commissionRule = new FeeRuleRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "CLASSIC", null,
                FeeType.COMMISSION, FeeValueType.PERCENTAGE, new BigDecimal("13.0000"), null,
                "BRL", null, null,
                FeeSourceType.OFFICIAL_CONTRACT, "Tabela Meli 2026", null,
                now.minusSeconds(3600), null, 1, true, "system", now, now
        );

        // Regra 2: Tarifa fixa de R$ 6,00 para itens com preço até R$ 79,00
        FeeRuleRecord fixedFeeRule = new FeeRuleRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "CLASSIC", null,
                FeeType.FIXED_FEE, FeeValueType.FIXED_AMOUNT, null, new BigDecimal("6.0000"),
                "BRL", BigDecimal.ZERO, new BigDecimal("79.0000"),
                FeeSourceType.OFFICIAL_CONTRACT, "Tabela Meli 2026 - Tarifa Fixa", null,
                now.minusSeconds(3600), null, 1, true, "system", now, now
        );

        when(feeRuleRepository.findCandidateRules(eq(tenantId), eq("MERCADO_LIVRE"), eq("CLASSIC"), any(Instant.class)))
                .thenReturn(List.of(commissionRule, fixedFeeRule));

        // Preço de R$ 50,00 -> Comissão 13% = R$ 6,50 + Tarifa Fixa R$ 6,00 = Total R$ 12,50
        FeeResolutionRequest request = new FeeResolutionRequest(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                new BigDecimal("50.0000"),
                now
        );

        FeeResolutionResult result = feeResolutionService.resolveFees(tenantId, request);

        assertThat(result).isNotNull();
        assertThat(result.resolutionStatus()).isEqualTo(FeeResolutionStatus.COMPLETE);
        assertThat(result.hasMissingFees()).isFalse();
        assertThat(result.missingFeeTypes()).isEmpty();
        assertThat(result.resolvedFees()).hasSize(2);
        assertThat(result.totalFeeAmount()).isEqualByComparingTo("12.5000");
    }

    @Test
    @DisplayName("2. Teste de Fronteira de Faixa de Preço: Limites inclusivos (<= 79.00 aplica, > 79.00 não aplica)")
    void shouldRespectPriceTierThresholdBoundaries() {
        Instant now = Instant.now();

        FeeRuleRecord commissionRule = new FeeRuleRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "CLASSIC", null,
                FeeType.COMMISSION, FeeValueType.PERCENTAGE, new BigDecimal("12.0000"), null,
                "BRL", null, null,
                FeeSourceType.OFFICIAL_CONTRACT, "Tabela Meli", null,
                now.minusSeconds(3600), null, 1, true, "system", now, now
        );

        FeeRuleRecord fixedFeeRule = new FeeRuleRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "CLASSIC", null,
                FeeType.FIXED_FEE, FeeValueType.FIXED_AMOUNT, null, new BigDecimal("6.0000"),
                "BRL", BigDecimal.ZERO, new BigDecimal("79.0000"),
                FeeSourceType.OFFICIAL_CONTRACT, "Tabela Meli Tarifa Fixa", null,
                now.minusSeconds(3600), null, 1, true, "system", now, now
        );

        when(feeRuleRepository.findCandidateRules(eq(tenantId), eq("MERCADO_LIVRE"), eq("CLASSIC"), any(Instant.class)))
                .thenReturn(List.of(commissionRule, fixedFeeRule));

        // Caso A: Preço exato no limite R$ 79,00 -> Tarifa fixa APLICA
        FeeResolutionResult resultAtThreshold = feeResolutionService.resolveFees(
                tenantId,
                new FeeResolutionRequest("MERCADO_LIVRE", "CLASSIC", null, new BigDecimal("79.0000"), now)
        );
        assertThat(resultAtThreshold.resolvedFees()).hasSize(2); // comissão + fixa
        // Comissão 12% de 79 = 9.48 + 6.00 = 15.4800
        assertThat(resultAtThreshold.totalFeeAmount()).isEqualByComparingTo("15.4800");

        // Caso B: Preço ligeiramente acima R$ 79.01 -> Tarifa fixa NÃO APLICA
        FeeResolutionResult resultAboveThreshold = feeResolutionService.resolveFees(
                tenantId,
                new FeeResolutionRequest("MERCADO_LIVRE", "CLASSIC", null, new BigDecimal("79.0100"), now)
        );
        assertThat(resultAboveThreshold.resolvedFees()).hasSize(1); // apenas comissão
        // 12% de 79.01 = 9.4812
        assertThat(resultAboveThreshold.totalFeeAmount()).isEqualByComparingTo("9.4812");
    }

    @Test
    @DisplayName("3. MANDATO ANTI-ALUCINAÇÃO: Marketplace sem regra configurada NÃO inventa taxa zero e indica ausência explícita")
    void shouldExplicitlyFlagMissingFeesWhenNoRulesConfigured() {
        Instant now = Instant.now();

        // Nenhuma regra cadastrada para o canal AMAZON
        when(feeRuleRepository.findCandidateRules(eq(tenantId), eq("AMAZON"), eq("FBA"), any(Instant.class)))
                .thenReturn(List.of());

        FeeResolutionRequest request = new FeeResolutionRequest(
                "AMAZON",
                "FBA",
                null,
                new BigDecimal("150.0000"),
                now
        );

        FeeResolutionResult result = feeResolutionService.resolveFees(tenantId, request);

        assertThat(result).isNotNull();
        // Não assume taxa zero silenciosamente!
        assertThat(result.hasMissingFees()).isTrue();
        assertThat(result.resolutionStatus()).isEqualTo(FeeResolutionStatus.MISSING);
        assertThat(result.missingFeeTypes()).containsExactly(FeeType.COMMISSION);
        assertThat(result.totalFeeAmount()).isEqualByComparingTo("0.0000");
        assertThat(result.userActionRequired()).contains("Nenhuma regra de comissão validada encontrada para o canal AMAZON");
        assertThat(result.userActionRequired()).contains("A plataforma não assume taxa zero sem fonte oficial comprovada");
    }

    @Test
    @DisplayName("4. MANDATO ANTI-ALUCINAÇÃO: Taxa parcial (apenas taxa fixa, sem comissão) retorna status PARTIAL")
    void shouldReturnPartialWhenCommissionIsMissing() {
        Instant now = Instant.now();

        FeeRuleRecord fixedFeeOnly = new FeeRuleRecord(
                UUID.randomUUID(), tenantId, "SHOPEE", "STANDARD", null,
                FeeType.FIXED_FEE, FeeValueType.FIXED_AMOUNT, null, new BigDecimal("4.0000"),
                "BRL", null, null,
                FeeSourceType.USER_CONFIGURED, "Regra Shopee", null,
                now.minusSeconds(3600), null, 1, true, "system", now, now
        );

        when(feeRuleRepository.findCandidateRules(eq(tenantId), eq("SHOPEE"), eq("STANDARD"), any(Instant.class)))
                .thenReturn(List.of(fixedFeeOnly));

        FeeResolutionRequest request = new FeeResolutionRequest(
                "SHOPEE",
                "STANDARD",
                null,
                new BigDecimal("30.0000"),
                now
        );

        FeeResolutionResult result = feeResolutionService.resolveFees(tenantId, request);

        assertThat(result.hasMissingFees()).isTrue();
        assertThat(result.resolutionStatus()).isEqualTo(FeeResolutionStatus.PARTIAL);
        assertThat(result.missingFeeTypes()).containsExactly(FeeType.COMMISSION);
        assertThat(result.resolvedFees()).hasSize(1);
        assertThat(result.totalFeeAmount()).isEqualByComparingTo("4.0000");
    }

    @Test
    @DisplayName("5. Regra de categoria específica se sobrepõe à regra padrão (DEFAULT/null)")
    void shouldPreferSpecificCategoryRuleOverDefault() {
        Instant now = Instant.now();

        FeeRuleRecord defaultRule = new FeeRuleRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "CLASSIC", null,
                FeeType.COMMISSION, FeeValueType.PERCENTAGE, new BigDecimal("12.0000"), null,
                "BRL", null, null,
                FeeSourceType.OFFICIAL_CONTRACT, "Tabela Geral", null,
                now.minusSeconds(3600), null, 1, true, "system", now, now
        );

        FeeRuleRecord specificCategoryRule = new FeeRuleRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "CLASSIC", "MLB1055",
                FeeType.COMMISSION, FeeValueType.PERCENTAGE, new BigDecimal("14.0000"), null,
                "BRL", null, null,
                FeeSourceType.OFFICIAL_CONTRACT, "Tabela Celulares", null,
                now.minusSeconds(3600), null, 1, true, "system", now, now
        );

        when(feeRuleRepository.findCandidateRules(eq(tenantId), eq("MERCADO_LIVRE"), eq("CLASSIC"), any(Instant.class)))
                .thenReturn(List.of(defaultRule, specificCategoryRule));

        FeeResolutionRequest request = new FeeResolutionRequest(
                "MERCADO_LIVRE",
                "CLASSIC",
                "MLB1055", // Categoria informada
                new BigDecimal("100.0000"),
                now
        );

        FeeResolutionResult result = feeResolutionService.resolveFees(tenantId, request);

        assertThat(result.resolvedFees()).hasSize(1);
        // Aplica a comissão específica de 14%, e não a padrão de 12%
        assertThat(result.resolvedFees().get(0).percentageRate()).isEqualByComparingTo("14.0000");
        assertThat(result.totalFeeAmount()).isEqualByComparingTo("14.0000");
    }

    @Test
    @DisplayName("6. Teste de Precisão Decimal e Arredondamento HALF_UP documentado")
    void shouldCalculateFractionalCentsWithHalfUpRounding() {
        Instant now = Instant.now();

        // 13.3333% de comissão
        FeeRuleRecord fractionalRateRule = new FeeRuleRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "CLASSIC", null,
                FeeType.COMMISSION, FeeValueType.PERCENTAGE, new BigDecimal("13.3333"), null,
                "BRL", null, null,
                FeeSourceType.OFFICIAL_CONTRACT, "Tabela Fracionada", null,
                now.minusSeconds(3600), null, 1, true, "system", now, now
        );

        when(feeRuleRepository.findCandidateRules(eq(tenantId), eq("MERCADO_LIVRE"), eq("CLASSIC"), any(Instant.class)))
                .thenReturn(List.of(fractionalRateRule));

        // Preço R$ 19,99 -> 19.99 * 0.133333 = 2.66532667 -> 2.6653 (HALF_UP em 4 casas)
        FeeResolutionRequest request = new FeeResolutionRequest(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                new BigDecimal("19.9900"),
                now
        );

        FeeResolutionResult result = feeResolutionService.resolveFees(tenantId, request);

        assertThat(result.totalFeeAmount()).isEqualByComparingTo("2.6653");
    }
}
