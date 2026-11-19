package com.sellerintelligence.modules.pricing;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.modules.pricing.entity.FeeRuleRecord;
import com.sellerintelligence.modules.pricing.model.FeeRuleDto;
import com.sellerintelligence.modules.pricing.model.FeeSourceType;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.FeeValueType;
import com.sellerintelligence.modules.pricing.model.RegisterFeeRuleCommand;
import com.sellerintelligence.modules.pricing.repository.FeeRuleRepository;
import com.sellerintelligence.modules.pricing.service.FeeRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeRuleServiceTest {

    @Mock
    private FeeRuleRepository feeRuleRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private FeeRuleService feeRuleService;

    private final UUID tenantId = UUID.randomUUID();
    private final String userEmail = "admin@store.com";

    @BeforeEach
    void setUp() {
        feeRuleService = new FeeRuleService(feeRuleRepository, auditPublisher);
    }

    @Test
    @DisplayName("1. Deve registrar regra de comissão com fonte contratual válida e auditoria")
    void shouldRegisterValidFeeRule() {
        RegisterFeeRuleCommand cmd = new RegisterFeeRuleCommand(
                "MERCADO_LIVRE",
                "CLASSIC",
                "MLB1055",
                FeeType.COMMISSION,
                FeeValueType.PERCENTAGE,
                new BigDecimal("13.0000"),
                null,
                "BRL",
                null,
                null,
                FeeSourceType.OFFICIAL_CONTRACT,
                "Tabela Oficial Mercado Livre 2026 - Anexo Clássico",
                "Comissão padrão da categoria Celulares",
                Instant.parse("2026-01-01T00:00:00Z")
        );

        when(feeRuleRepository.findCurrentOpenRule(
                tenantId, "MERCADO_LIVRE", "CLASSIC", "MLB1055", FeeType.COMMISSION
        )).thenReturn(Optional.empty());

        when(feeRuleRepository.save(any(FeeRuleRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        FeeRuleDto dto = feeRuleService.registerRule(tenantId, userEmail, cmd);

        assertThat(dto).isNotNull();
        assertThat(dto.version()).isEqualTo(1);
        assertThat(dto.percentageRate()).isEqualByComparingTo("13.0000");
        assertThat(dto.sourceReference()).isEqualTo("Tabela Oficial Mercado Livre 2026 - Anexo Clássico");
        assertThat(dto.effectiveTo()).isNull();

        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditPublisher).publish(
                eventTypeCaptor.capture(),
                any(),
                eq(userEmail),
                eq(tenantId),
                any(),
                any(),
                any(),
                any()
        );
        assertThat(eventTypeCaptor.getValue()).isEqualTo("FEE_RULE_CREATED");
    }

    @Test
    @DisplayName("2. MANDATO ANTI-ALUCINAÇÃO: Não inventar taxas e rejeitar regras sem sourceReference comprovado")
    void shouldRejectFeeRuleWithoutSourceReference() {
        RegisterFeeRuleCommand cmd = new RegisterFeeRuleCommand(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                FeeType.COMMISSION,
                FeeValueType.PERCENTAGE,
                new BigDecimal("12.0000"),
                null,
                "BRL",
                null,
                null,
                FeeSourceType.OFFICIAL_CONTRACT,
                "   ", // Vazio! Proibido inventar taxas sem fonte
                null,
                Instant.now()
        );

        assertThatThrownBy(() -> feeRuleService.registerRule(tenantId, userEmail, cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceReference é obrigatório");
    }

    @Test
    @DisplayName("3. Deve rejeitar regra com valores negativos")
    void shouldRejectNegativeRates() {
        RegisterFeeRuleCommand cmd = new RegisterFeeRuleCommand(
                "SHOPEE",
                "STANDARD",
                null,
                FeeType.COMMISSION,
                FeeValueType.PERCENTAGE,
                new BigDecimal("-5.0000"),
                null,
                "BRL",
                null,
                null,
                FeeSourceType.USER_CONFIGURED,
                "Contrato Shopee 2026",
                null,
                Instant.now()
        );

        assertThatThrownBy(() -> feeRuleService.registerRule(tenantId, userEmail, cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser negativo");
    }

    @Test
    @DisplayName("4. Teste de Fronteira: Rejeitar minPriceThreshold maior que maxPriceThreshold")
    void shouldRejectMinThresholdGreaterThanMaxThreshold() {
        RegisterFeeRuleCommand cmd = new RegisterFeeRuleCommand(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                FeeType.FIXED_FEE,
                FeeValueType.FIXED_AMOUNT,
                null,
                new BigDecimal("6.0000"),
                "BRL",
                new BigDecimal("100.0000"), // min > max
                new BigDecimal("50.0000"),
                FeeSourceType.OFFICIAL_CONTRACT,
                "Termos Meli 2026",
                null,
                Instant.now()
        );

        assertThatThrownBy(() -> feeRuleService.registerRule(tenantId, userEmail, cmd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minPriceThreshold não pode ser maior que maxPriceThreshold");
    }

    @Test
    @DisplayName("5. Atualização de regra encerra vigência da anterior e gera versão 2")
    void shouldClosePreviousRuleValidityOnUpdate() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-07-01T00:00:00Z");

        FeeRuleRecord previous = new FeeRuleRecord(
                UUID.randomUUID(), tenantId, "MERCADO_LIVRE", "CLASSIC", null,
                FeeType.FIXED_FEE, FeeValueType.FIXED_AMOUNT, null, new BigDecimal("6.0000"),
                "BRL", BigDecimal.ZERO, new BigDecimal("79.0000"),
                FeeSourceType.OFFICIAL_CONTRACT, "Tabela Antiga 2025", null,
                t1, null, 1, true, userEmail, t1, t1
        );

        when(feeRuleRepository.findCurrentOpenRule(
                tenantId, "MERCADO_LIVRE", "CLASSIC", null, FeeType.FIXED_FEE
        )).thenReturn(Optional.of(previous));

        when(feeRuleRepository.save(any(FeeRuleRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterFeeRuleCommand updateCmd = new RegisterFeeRuleCommand(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                FeeType.FIXED_FEE,
                FeeValueType.FIXED_AMOUNT,
                null,
                new BigDecimal("6.5000"), // reajuste da tarifa fixa
                "BRL",
                BigDecimal.ZERO,
                new BigDecimal("79.0000"),
                FeeSourceType.OFFICIAL_CONTRACT,
                "Tabela Nova 2026",
                null,
                t2
        );

        FeeRuleDto updatedDto = feeRuleService.registerRule(tenantId, userEmail, updateCmd);

        assertThat(previous.getEffectiveTo()).isEqualTo(t2);
        assertThat(updatedDto.version()).isEqualTo(2);
        assertThat(updatedDto.fixedAmount()).isEqualByComparingTo("6.5000");

        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditPublisher).publish(
                eventTypeCaptor.capture(),
                any(),
                eq(userEmail),
                eq(tenantId),
                any(),
                any(),
                any(),
                any()
        );
        assertThat(eventTypeCaptor.getValue()).isEqualTo("FEE_RULE_UPDATED");
    }
}
