package com.sellerintelligence.modules.pricing;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.modules.pricing.entity.ProductCostRecord;
import com.sellerintelligence.modules.pricing.model.ProductCostDto;
import com.sellerintelligence.modules.pricing.model.RegisterProductCostCommand;
import com.sellerintelligence.modules.pricing.repository.ProductCostRepository;
import com.sellerintelligence.modules.pricing.service.ProductCostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class ProductCostServiceTest {

    @Mock
    private ProductCostRepository productCostRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private ProductCostService productCostService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final String userEmail = "seller@store.com";

    @BeforeEach
    void setUp() {
        productCostService = new ProductCostService(productCostRepository, auditPublisher);
    }

    @Test
    @DisplayName("1. Deve registrar custo inicial (versão 1) com vigência aberta e calcular total direto unitário")
    void shouldRegisterInitialCost() {
        Instant effectiveFrom = Instant.parse("2026-01-01T00:00:00Z");
        RegisterProductCostCommand cmd = new RegisterProductCostCommand(
                productId,
                variantId,
                companyId,
                "BRL",
                "UN",
                new BigDecimal("50.0000"), // aquisição
                new BigDecimal("3.5000"),  // embalagem
                new BigDecimal("1.5000"),  // adicionais
                new BigDecimal("5.0000"),  // frete subsidiado
                new BigDecimal("6.0000"),  // imposto %
                new BigDecimal("2.0000"),  // outras despesas %
                effectiveFrom,
                "Lote inicial de compra 2026"
        );

        when(productCostRepository.findCurrentOpenRecord(tenantId, productId, variantId, companyId))
                .thenReturn(Optional.empty());

        when(productCostRepository.save(any(ProductCostRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductCostDto result = productCostService.registerCost(tenantId, userEmail, cmd);

        assertThat(result).isNotNull();
        assertThat(result.version()).isEqualTo(1);
        assertThat(result.effectiveFrom()).isEqualTo(effectiveFrom);
        assertThat(result.effectiveTo()).isNull();
        assertThat(result.isActive()).isTrue();
        // 50.0000 + 3.5000 + 1.5000 + 5.0000 = 60.0000
        assertThat(result.totalDirectUnitCost()).isEqualByComparingTo("60.0000");

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
        assertThat(eventTypeCaptor.getValue()).isEqualTo("PRODUCT_COST_CREATED");
    }

    @Test
    @DisplayName("2. Deve encerrar a vigência do custo anterior e criar nova versão preservando o histórico")
    void shouldClosePreviousValidityAndUpdateToVersion2() {
        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-06-01T00:00:00Z");

        ProductCostRecord previousRecord = new ProductCostRecord(
                UUID.randomUUID(),
                tenantId,
                productId,
                variantId,
                companyId,
                "BRL",
                "UN",
                new BigDecimal("50.0000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                t1,
                null,
                1,
                true,
                userEmail,
                "Custo antigo",
                t1,
                t1
        );

        when(productCostRepository.findCurrentOpenRecord(tenantId, productId, variantId, companyId))
                .thenReturn(Optional.of(previousRecord));

        when(productCostRepository.save(any(ProductCostRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterProductCostCommand updateCmd = new RegisterProductCostCommand(
                productId,
                variantId,
                companyId,
                "BRL",
                "UN",
                new BigDecimal("55.0000"), // reajuste
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                t2,
                "Reajuste de fornecedor"
        );

        ProductCostDto updatedDto = productCostService.registerCost(tenantId, userEmail, updateCmd);

        // Registro anterior teve sua vigência encerrada no exato momento t2
        assertThat(previousRecord.getEffectiveTo()).isEqualTo(t2);

        // Novo registro é a versão 2 com início em t2 e vigência aberta
        assertThat(updatedDto.version()).isEqualTo(2);
        assertThat(updatedDto.effectiveFrom()).isEqualTo(t2);
        assertThat(updatedDto.effectiveTo()).isNull();
        assertThat(updatedDto.acquisitionCost()).isEqualByComparingTo("55.0000");

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
        assertThat(eventTypeCaptor.getValue()).isEqualTo("PRODUCT_COST_UPDATED");
    }

    @Test
    @DisplayName("3. Deve recuperar custo vigente respeitando a hierarquia de especificidade (Variante+Empresa > Variante > Produto)")
    void shouldResolveEffectiveCostBySpecificityCascade() {
        Instant now = Instant.now();

        ProductCostRecord exactMatch = new ProductCostRecord(
                UUID.randomUUID(), tenantId, productId, variantId, companyId,
                "BRL", "UN", new BigDecimal("45.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                now.minus(10, ChronoUnit.DAYS), null, 1, true, userEmail, "Exato", now, now
        );

        when(productCostRepository.findCandidateCosts(tenantId, productId, variantId, companyId, now))
                .thenReturn(List.of(exactMatch));

        Optional<ProductCostDto> result = productCostService.getEffectiveCost(
                tenantId, productId, variantId, companyId, now
        );

        assertThat(result).isPresent();
        assertThat(result.get().acquisitionCost()).isEqualByComparingTo("45.0000");
    }

    @Test
    @DisplayName("4. Teste de Fronteira Temporal: effective_from é inclusivo e effective_to é exclusivo")
    void shouldRespectTemporalBoundariesStrictly() {
        Instant tFrom = Instant.parse("2026-03-01T00:00:00Z");
        Instant tTo = Instant.parse("2026-06-01T00:00:00Z");

        ProductCostRecord record = new ProductCostRecord(
                UUID.randomUUID(), tenantId, productId, null, null,
                "BRL", "UN", new BigDecimal("100.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                tFrom, tTo, 1, true, userEmail, "Teste fronteira", tFrom, tFrom
        );

        // 1. No exato instante effective_from -> VIGENTE
        when(productCostRepository.findCandidateCosts(tenantId, productId, null, null, tFrom))
                .thenReturn(List.of(record));
        Optional<ProductCostDto> onFrom = productCostService.getEffectiveCost(tenantId, productId, null, null, tFrom);
        assertThat(onFrom).isPresent();

        // 2. No instante effective_to -> NÃO VIGENTE (fim é exclusivo)
        when(productCostRepository.findCandidateCosts(tenantId, productId, null, null, tTo))
                .thenReturn(List.of());
        Optional<ProductCostDto> onTo = productCostService.getEffectiveCost(tenantId, productId, null, null, tTo);
        assertThat(onTo).isEmpty();

        // 3. Antes de effective_from -> NÃO VIGENTE
        Instant before = tFrom.minus(1, ChronoUnit.SECONDS);
        when(productCostRepository.findCandidateCosts(tenantId, productId, null, null, before))
                .thenReturn(List.of());
        Optional<ProductCostDto> onBefore = productCostService.getEffectiveCost(tenantId, productId, null, null, before);
        assertThat(onBefore).isEmpty();
    }

    @Test
    @DisplayName("5. Deve retornar Optional.empty() quando não houver custo configurado (ausência explícita)")
    void shouldReturnEmptyWhenNoCostConfigured() {
        Instant now = Instant.now();
        when(productCostRepository.findCandidateCosts(tenantId, productId, null, null, now))
                .thenReturn(List.of());

        Optional<ProductCostDto> result = productCostService.getEffectiveCost(tenantId, productId, null, null, now);
        assertThat(result).isEmpty();
    }
}
