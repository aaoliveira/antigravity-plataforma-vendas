package com.sellerintelligence.modules.financialanalytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.company.entity.CompanyAccountLinkRecord;
import com.sellerintelligence.modules.company.repository.CompanyAccountLinkRepository;
import com.sellerintelligence.modules.financialanalytics.entity.OrderFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.entity.OrderItemFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.model.FinancialFactSummaryDto;
import com.sellerintelligence.modules.financialanalytics.model.FinancialValueProvenance;
import com.sellerintelligence.modules.financialanalytics.model.OrderFinancialFactDto;
import com.sellerintelligence.modules.financialanalytics.model.ReconcileSettlementCommand;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import com.sellerintelligence.modules.financialanalytics.repository.OrderItemFinancialFactRepository;
import com.sellerintelligence.modules.financialanalytics.service.OrderFinancialFactService;
import com.sellerintelligence.modules.orderingestion.Order;
import com.sellerintelligence.modules.orderingestion.OrderItem;
import com.sellerintelligence.modules.orderingestion.OrderItemRepository;
import com.sellerintelligence.modules.orderingestion.OrderRepository;
import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
import com.sellerintelligence.modules.pricing.entity.ProductCostRecord;
import com.sellerintelligence.modules.pricing.model.FeeResolutionRequest;
import com.sellerintelligence.modules.pricing.model.FeeResolutionResult;
import com.sellerintelligence.modules.pricing.model.FeeResolutionStatus;
import com.sellerintelligence.modules.pricing.model.FeeSourceType;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.FeeValueType;
import com.sellerintelligence.modules.pricing.model.ResolvedFeeItem;
import com.sellerintelligence.modules.pricing.repository.ProductCostRepository;
import com.sellerintelligence.modules.pricing.service.FeeResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
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
class OrderFinancialFactServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CompanyAccountLinkRepository companyAccountLinkRepository;

    @Mock
    private ProductCostRepository productCostRepository;

    @Mock
    private FeeResolutionService feeResolutionService;

    @Mock
    private OrderFinancialFactRepository orderFinancialFactRepository;

    @Mock
    private OrderItemFinancialFactRepository orderItemFinancialFactRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrderFinancialFactService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID sellerAccountId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OrderFinancialFactService(
                orderRepository,
                orderItemRepository,
                companyAccountLinkRepository,
                productCostRepository,
                feeResolutionService,
                orderFinancialFactRepository,
                orderItemFinancialFactRepository,
                objectMapper
        );
    }

    private Order createSampleOrder(BigDecimal shippingCost, OrderStatus status) {
        return new Order(
                orderId,
                tenantId,
                sellerAccountId,
                "MERCADO_LIVRE",
                "MLB-999888777",
                status,
                new BigDecimal("300.0000"),
                shippingCost,
                new BigDecimal("300.0000").add(shippingCost),
                "BRL",
                "Comprador Exemplo",
                "123.456.789-00",
                "BR123456789",
                null,
                null,
                null,
                "{}",
                "v1",
                "MERCADO_LIVRE_API",
                Instant.now(),
                Instant.now()
        );
    }

    private OrderItem createSampleItem(UUID itemId, UUID productId, String sku, BigDecimal unitPrice, int qty) {
        return new OrderItem(
                itemId,
                tenantId,
                orderId,
                productId,
                null,
                "MLB12345",
                "ITEM123",
                sku,
                "Produto de Teste " + sku,
                unitPrice,
                qty,
                unitPrice.multiply(BigDecimal.valueOf(qty))
        );
    }

    private ProductCostRecord createSampleCost(UUID productId, BigDecimal acqCost, BigDecimal packCost, BigDecimal taxRate) {
        Instant now = Instant.now();
        return new ProductCostRecord(
                UUID.randomUUID(),
                tenantId,
                productId,
                null,
                companyId,
                "BRL",
                "UN",
                acqCost,
                packCost,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                taxRate,
                BigDecimal.ZERO,
                now.minusSeconds(86400),
                null,
                1,
                true,
                "admin@sellerintelligence.com",
                "Custo teste",
                now.minusSeconds(86400),
                now.minusSeconds(86400)
        );
    }

    private FeeResolutionResult createSampleFeeResult(BigDecimal price, BigDecimal commissionAmount, BigDecimal fixedFeeAmount) {
        ResolvedFeeItem commission = new ResolvedFeeItem(
                UUID.randomUUID(),
                FeeType.COMMISSION,
                FeeValueType.PERCENTAGE,
                new BigDecimal("14.0000"),
                null,
                commissionAmount,
                "BRL",
                FeeSourceType.OFFICIAL_CONTRACT,
                "https://developers.mercadolibre.com.br",
                1
        );
        ResolvedFeeItem fixed = new ResolvedFeeItem(
                UUID.randomUUID(),
                FeeType.FIXED_FEE,
                FeeValueType.FIXED_AMOUNT,
                null,
                fixedFeeAmount,
                fixedFeeAmount,
                "BRL",
                FeeSourceType.OFFICIAL_CONTRACT,
                "https://developers.mercadolibre.com.br",
                1
        );
        return new FeeResolutionResult(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                price,
                "BRL",
                List.of(commission, fixed),
                commissionAmount.add(fixedFeeAmount),
                false,
                Collections.emptyList(),
                FeeResolutionStatus.COMPLETE,
                null
        );
    }

    @Test
    @DisplayName("1. Deve apurar fatos com múltiplos itens e garantir soma dos itens rigorosamente reconciliável com o pedido")
    void shouldComputeOrderFactsWithMultipleItemsAndVerifyExactReconciliability() {
        Order order = createSampleOrder(new BigDecimal("25.0000"), OrderStatus.PAID);
        UUID item1Id = UUID.randomUUID();
        UUID item2Id = UUID.randomUUID();
        UUID prod1Id = UUID.randomUUID();
        UUID prod2Id = UUID.randomUUID();

        OrderItem item1 = createSampleItem(item1Id, prod1Id, "SKU-A", new BigDecimal("100.0000"), 1);
        OrderItem item2 = createSampleItem(item2Id, prod2Id, "SKU-B", new BigDecimal("100.0000"), 2); // total 200.00

        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(List.of(item1, item2));

        Instant now = Instant.now();
        CompanyAccountLinkRecord link = new CompanyAccountLinkRecord(
                UUID.randomUUID(), tenantId, companyId, sellerAccountId, now, null, true, 1, null, "admin", now, now
        );
        when(companyAccountLinkRepository.findActiveBySellerAccount(tenantId, sellerAccountId)).thenReturn(Optional.of(link));

        // Custos de produtos
        ProductCostRecord cost1 = createSampleCost(prod1Id, new BigDecimal("30.0000"), new BigDecimal("2.0000"), new BigDecimal("6.0000"));
        ProductCostRecord cost2 = createSampleCost(prod2Id, new BigDecimal("40.0000"), new BigDecimal("1.5000"), new BigDecimal("6.0000"));
        when(productCostRepository.findCandidateCosts(eq(tenantId), eq(prod1Id), any(), eq(companyId), any())).thenReturn(List.of(cost1));
        when(productCostRepository.findCandidateCosts(eq(tenantId), eq(prod2Id), any(), eq(companyId), any())).thenReturn(List.of(cost2));

        // Regras de taxa
        FeeResolutionResult feeResult1 = createSampleFeeResult(new BigDecimal("100.0000"), new BigDecimal("14.0000"), new BigDecimal("6.0000"));
        FeeResolutionResult feeResult2 = createSampleFeeResult(new BigDecimal("100.0000"), new BigDecimal("14.0000"), new BigDecimal("6.0000"));
        when(feeResolutionService.resolveFees(eq(tenantId), any())).thenReturn(feeResult1, feeResult2);

        when(orderFinancialFactRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(Optional.empty());
        when(orderFinancialFactRepository.save(any(OrderFinancialFactRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemFinancialFactRepository.findByTenantIdAndOrderItemId(any(), any())).thenReturn(Optional.empty());
        when(orderItemFinancialFactRepository.save(any(OrderItemFinancialFactRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        // Executa apuração
        OrderFinancialFactDto result = service.computeOrReprocessOrderFacts(tenantId, orderId);

        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.grossRevenue()).isEqualByComparingTo("300.0000");
        assertThat(result.shippingCost()).isEqualByComparingTo("25.0000");
        assertThat(result.items()).hasSize(2);

        // Verificação da reconciliabilidade da soma dos itens contra o consolidado do pedido
        BigDecimal sumItemsGrossRevenue = result.items().stream()
                .map(i -> i.grossRevenue()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumItemsShipping = result.items().stream()
                .map(i -> i.shippingCost()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumItemsTotalDeductions = result.items().stream()
                .map(i -> i.totalDeductions()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumItemsContributionMargin = result.items().stream()
                .map(i -> i.contributionMarginAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sumItemsGrossRevenue).isEqualByComparingTo(result.grossRevenue());
        assertThat(sumItemsShipping).isEqualByComparingTo(result.shippingCost());
        assertThat(sumItemsTotalDeductions).isEqualByComparingTo(result.totalDeductions());
        assertThat(sumItemsContributionMargin).isEqualByComparingTo(result.contributionMarginAmount());

        // Verificação anti-alucinação
        assertThat(result.antiHallucinationDisclaimer()).isNotBlank();
        assertThat(result.antiHallucinationDisclaimer()).contains("Margem de Contribuição");
        assertThat(result.antiHallucinationDisclaimer()).contains("proibido rotular este valor como Lucro Líquido");

        // Status e proveniência
        assertThat(result.reconciliationStatus()).isEqualTo(ReconciliationStatus.ESTIMATED);
        assertThat(result.feeProvenance()).isEqualTo(FinancialValueProvenance.ESTIMATED);
        assertThat(result.costProvenance()).isEqualTo(FinancialValueProvenance.USER_INFORMED);
        assertThat(result.hasMissingData()).isFalse();
    }

    @Test
    @DisplayName("2. Deve ser idempotente no reprocessamento do pedido sem duplicar registros")
    void shouldBeIdempotentOnReprocessing() {
        Order order = createSampleOrder(BigDecimal.ZERO, OrderStatus.PAID);
        UUID itemId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        OrderItem item = createSampleItem(itemId, prodId, "SKU-C", new BigDecimal("150.0000"), 1);

        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(List.of(item));
        when(companyAccountLinkRepository.findActiveBySellerAccount(tenantId, sellerAccountId)).thenReturn(Optional.empty());
        when(productCostRepository.findCandidateCosts(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(feeResolutionService.resolveFees(any(), any())).thenReturn(
                new FeeResolutionResult("MERCADO_LIVRE", "CLASSIC", null, new BigDecimal("150.0000"), "BRL",
                        Collections.emptyList(), BigDecimal.ZERO, true, List.of(FeeType.COMMISSION),
                        FeeResolutionStatus.MISSING, "Cadastre taxas")
        );

        OrderFinancialFactRecord existingRecord = new OrderFinancialFactRecord(
                UUID.randomUUID(), tenantId, orderId, sellerAccountId, null, "MERCADO_LIVRE", "MLB-1", "PAID",
                Instant.now(), new BigDecimal("100.0000"), BigDecimal.ZERO, new BigDecimal("100.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("100.0000"), new BigDecimal("100.0000"), "BRL",
                ReconciliationStatus.ESTIMATED, FinancialValueProvenance.ESTIMATED, FinancialValueProvenance.ESTIMATED,
                FinancialValueProvenance.USER_INFORMED, FinancialValueProvenance.USER_INFORMED, false, null, "v1.0.0", "{}"
        );
        when(orderFinancialFactRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(Optional.of(existingRecord));
        when(orderFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderItemFinancialFactRecord existingItemRecord = new OrderItemFinancialFactRecord(
                UUID.randomUUID(), tenantId, existingRecord.getId(), orderId, itemId, prodId, null, "SKU-C",
                "Item", 1, new BigDecimal("100.0000"), new BigDecimal("100.0000"), BigDecimal.ZERO,
                new BigDecimal("100.0000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100.0000"),
                new BigDecimal("100.0000"), FinancialValueProvenance.USER_INFORMED, false, null
        );
        when(orderItemFinancialFactRepository.findByTenantIdAndOrderItemId(tenantId, itemId)).thenReturn(Optional.of(existingItemRecord));
        when(orderItemFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderFinancialFactDto result = service.computeOrReprocessOrderFacts(tenantId, orderId);

        assertThat(result).isNotNull();
        assertThat(result.grossRevenue()).isEqualByComparingTo("150.0000");

        // Verifica que o save foi chamado para atualizar os registros existentes
        verify(orderFinancialFactRepository).save(existingRecord);
        verify(orderItemFinancialFactRepository).save(existingItemRecord);
    }

    @Test
    @DisplayName("3. Deve sinalizar dados ausentes quando custo de produto não estiver cadastrado sem inventar valores")
    void shouldFlagMissingDataWhenProductCostNotFound() {
        Order order = createSampleOrder(BigDecimal.ZERO, OrderStatus.PAID);
        UUID itemId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        OrderItem item = createSampleItem(itemId, prodId, "SKU-WITHOUT-COST", new BigDecimal("80.0000"), 1);

        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(List.of(item));
        when(companyAccountLinkRepository.findActiveBySellerAccount(tenantId, sellerAccountId)).thenReturn(Optional.empty());

        // Custo ausente
        when(productCostRepository.findCandidateCosts(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

        // Taxa presente
        FeeResolutionResult feeResult = createSampleFeeResult(new BigDecimal("80.0000"), new BigDecimal("11.2000"), new BigDecimal("6.0000"));
        when(feeResolutionService.resolveFees(any(), any())).thenReturn(feeResult);

        when(orderFinancialFactRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(Optional.empty());
        when(orderFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemFinancialFactRepository.findByTenantIdAndOrderItemId(any(), any())).thenReturn(Optional.empty());
        when(orderItemFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderFinancialFactDto result = service.computeOrReprocessOrderFacts(tenantId, orderId);

        assertThat(result.hasMissingData()).isTrue();
        assertThat(result.costProvenance()).isEqualTo(FinancialValueProvenance.MISSING);
        assertThat(result.missingDataReasons()).contains("PRODUCT_COST_MISSING");
        assertThat(result.productCost()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("4. Deve tratar cancelamentos e devoluções deterministamente zerando a margem de contribuição")
    void shouldHandleCancelledOrdersDeterministically() {
        Order order = createSampleOrder(new BigDecimal("15.0000"), OrderStatus.CANCELLED);
        UUID itemId = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        OrderItem item = createSampleItem(itemId, prodId, "SKU-CANCEL", new BigDecimal("120.0000"), 1);

        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(List.of(item));
        when(companyAccountLinkRepository.findActiveBySellerAccount(tenantId, sellerAccountId)).thenReturn(Optional.empty());
        when(productCostRepository.findCandidateCosts(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(feeResolutionService.resolveFees(any(), any())).thenReturn(
                new FeeResolutionResult("MERCADO_LIVRE", "CLASSIC", null, new BigDecimal("120.0000"), "BRL",
                        Collections.emptyList(), BigDecimal.ZERO, false, Collections.emptyList(),
                        FeeResolutionStatus.COMPLETE, null)
        );

        when(orderFinancialFactRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(Optional.empty());
        when(orderFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemFinancialFactRepository.findByTenantIdAndOrderItemId(any(), any())).thenReturn(Optional.empty());
        when(orderItemFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderFinancialFactDto result = service.computeOrReprocessOrderFacts(tenantId, orderId);

        assertThat(result.orderStatus()).isEqualTo("CANCELLED");
        assertThat(result.cancellationAmount()).isEqualByComparingTo("120.0000");
        assertThat(result.contributionMarginRatio()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("5. Deve reconciliar com extrato externo marcando RECONCILED quando valores baterem dentro da tolerância")
    void shouldReconcileWithExternalSettlementWhenExactMatch() {
        OrderFinancialFactRecord fact = new OrderFinancialFactRecord(
                UUID.randomUUID(), tenantId, orderId, sellerAccountId, companyId, "MERCADO_LIVRE", "MLB-EXT", "PAID",
                Instant.now(), new BigDecimal("200.0000"), BigDecimal.ZERO, new BigDecimal("200.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("28.0000"), new BigDecimal("6.0000"),
                new BigDecimal("15.0000"), new BigDecimal("50.0000"), new BigDecimal("4.0000"), BigDecimal.ZERO,
                new BigDecimal("12.0000"), BigDecimal.ZERO, new BigDecimal("115.0000"), new BigDecimal("85.0000"),
                new BigDecimal("42.5000"), "BRL", ReconciliationStatus.ESTIMATED, FinancialValueProvenance.ESTIMATED,
                FinancialValueProvenance.ESTIMATED, FinancialValueProvenance.USER_INFORMED, FinancialValueProvenance.USER_INFORMED,
                false, null, "v1.0.0", "{}"
        );

        OrderItemFinancialFactRecord itemFact = new OrderItemFinancialFactRecord(
                UUID.randomUUID(), tenantId, fact.getId(), orderId, UUID.randomUUID(), UUID.randomUUID(), null,
                "SKU-1", "Item 1", 1, new BigDecimal("200.0000"), new BigDecimal("200.0000"), BigDecimal.ZERO,
                new BigDecimal("200.0000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("28.0000"),
                new BigDecimal("6.0000"), new BigDecimal("15.0000"), new BigDecimal("50.0000"), new BigDecimal("50.0000"),
                new BigDecimal("4.0000"), BigDecimal.ZERO, new BigDecimal("6.0000"), new BigDecimal("12.0000"),
                BigDecimal.ZERO, new BigDecimal("115.0000"), new BigDecimal("85.0000"), new BigDecimal("42.5000"),
                FinancialValueProvenance.USER_INFORMED, false, null
        );

        when(orderFinancialFactRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(Optional.of(fact));
        when(orderItemFinancialFactRepository.findAllByTenantIdAndOrderFactIdOrderByCreatedAtAsc(tenantId, fact.getId()))
                .thenReturn(List.of(itemFact));
        when(orderFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReconcileSettlementCommand cmd = new ReconcileSettlementCommand(
                "EXT-SETTLE-001",
                Instant.now(),
                new BigDecimal("28.0000"), // exatamente igual
                new BigDecimal("6.0000"),
                new BigDecimal("15.0000"),
                new BigDecimal("151.0000"),
                "Liquidação recebida com sucesso"
        );

        OrderFinancialFactDto result = service.reconcileWithExternalSettlement(tenantId, orderId, cmd);

        assertThat(result.reconciliationStatus()).isEqualTo(ReconciliationStatus.RECONCILED);
        assertThat(result.feeProvenance()).isEqualTo(FinancialValueProvenance.CONFIRMED);
        assertThat(result.shippingProvenance()).isEqualTo(FinancialValueProvenance.CONFIRMED);
        assertThat(result.settlementExternalId()).isEqualTo("EXT-SETTLE-001");
    }

    @Test
    @DisplayName("6. Deve sinalizar status DIVERGENT quando tarifa real diferir da estimada além da tolerância")
    void shouldReconcileWithExternalSettlementWhenDivergent() {
        OrderFinancialFactRecord fact = new OrderFinancialFactRecord(
                UUID.randomUUID(), tenantId, orderId, sellerAccountId, companyId, "MERCADO_LIVRE", "MLB-EXT", "PAID",
                Instant.now(), new BigDecimal("200.0000"), BigDecimal.ZERO, new BigDecimal("200.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("28.0000"), new BigDecimal("6.0000"),
                new BigDecimal("15.0000"), new BigDecimal("50.0000"), new BigDecimal("4.0000"), BigDecimal.ZERO,
                new BigDecimal("12.0000"), BigDecimal.ZERO, new BigDecimal("115.0000"), new BigDecimal("85.0000"),
                new BigDecimal("42.5000"), "BRL", ReconciliationStatus.ESTIMATED, FinancialValueProvenance.ESTIMATED,
                FinancialValueProvenance.ESTIMATED, FinancialValueProvenance.USER_INFORMED, FinancialValueProvenance.USER_INFORMED,
                false, null, "v1.0.0", "{}"
        );

        OrderItemFinancialFactRecord itemFact = new OrderItemFinancialFactRecord(
                UUID.randomUUID(), tenantId, fact.getId(), orderId, UUID.randomUUID(), UUID.randomUUID(), null,
                "SKU-1", "Item 1", 1, new BigDecimal("200.0000"), new BigDecimal("200.0000"), BigDecimal.ZERO,
                new BigDecimal("200.0000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("28.0000"),
                new BigDecimal("6.0000"), new BigDecimal("15.0000"), new BigDecimal("50.0000"), new BigDecimal("50.0000"),
                new BigDecimal("4.0000"), BigDecimal.ZERO, new BigDecimal("6.0000"), new BigDecimal("12.0000"),
                BigDecimal.ZERO, new BigDecimal("115.0000"), new BigDecimal("85.0000"), new BigDecimal("42.5000"),
                FinancialValueProvenance.USER_INFORMED, false, null
        );

        when(orderFinancialFactRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(Optional.of(fact));
        when(orderItemFinancialFactRepository.findAllByTenantIdAndOrderFactIdOrderByCreatedAtAsc(tenantId, fact.getId()))
                .thenReturn(List.of(itemFact));
        when(orderFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemFinancialFactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Diferença de R$ 5,00 na taxa e R$ 4,00 no frete
        ReconcileSettlementCommand cmd = new ReconcileSettlementCommand(
                "EXT-SETTLE-DIVERGENT",
                Instant.now(),
                new BigDecimal("33.0000"), // estimada era 28
                new BigDecimal("6.0000"),
                new BigDecimal("19.0000"), // estimada era 15
                new BigDecimal("142.0000"),
                "Cobrança a maior detectada no extrato"
        );

        OrderFinancialFactDto result = service.reconcileWithExternalSettlement(tenantId, orderId, cmd);

        assertThat(result.reconciliationStatus()).isEqualTo(ReconciliationStatus.DIVERGENT);
        assertThat(result.feeProvenance()).isEqualTo(FinancialValueProvenance.CONFIRMED);
        assertThat(result.shippingProvenance()).isEqualTo(FinancialValueProvenance.CONFIRMED);
        assertThat(result.marketplaceFee()).isEqualByComparingTo("33.0000");
        assertThat(result.shippingCost()).isEqualByComparingTo("19.0000");
        assertThat(result.contributionMarginAmount()).isEqualByComparingTo("76.0000"); // 85 - 5 - 4
    }

    @Test
    @DisplayName("7. Deve calcular sumarização consolidada com médias e disclaimer anti-alucinação")
    void shouldCalculateFinancialSummaryConsistently() {
        OrderFinancialFactRecord fact1 = new OrderFinancialFactRecord(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), sellerAccountId, companyId, "MERCADO_LIVRE", "MLB-1", "PAID",
                Instant.now(), new BigDecimal("100.0000"), BigDecimal.ZERO, new BigDecimal("100.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("14.0000"), new BigDecimal("6.0000"),
                BigDecimal.ZERO, new BigDecimal("30.0000"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("6.0000"), BigDecimal.ZERO, new BigDecimal("56.0000"), new BigDecimal("44.0000"),
                new BigDecimal("44.0000"), "BRL", ReconciliationStatus.RECONCILED, FinancialValueProvenance.CONFIRMED,
                FinancialValueProvenance.CONFIRMED, FinancialValueProvenance.USER_INFORMED, FinancialValueProvenance.USER_INFORMED,
                false, null, "v1.0.0", "{}"
        );

        OrderFinancialFactRecord fact2 = new OrderFinancialFactRecord(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), sellerAccountId, companyId, "SHOPEE", "SHOPEE-1", "PAID",
                Instant.now(), new BigDecimal("200.0000"), BigDecimal.ZERO, new BigDecimal("200.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("28.0000"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("80.0000"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("12.0000"), BigDecimal.ZERO, new BigDecimal("120.0000"), new BigDecimal("80.0000"),
                new BigDecimal("40.0000"), "BRL", ReconciliationStatus.ESTIMATED, FinancialValueProvenance.ESTIMATED,
                FinancialValueProvenance.ESTIMATED, FinancialValueProvenance.USER_INFORMED, FinancialValueProvenance.USER_INFORMED,
                true, "[\"PRODUCT_COST_MISSING\"]", "v1.0.0", "{}"
        );

        when(orderFinancialFactRepository.findForSummary(eq(tenantId), any(), any(), any(), any()))
                .thenReturn(List.of(fact1, fact2));

        FinancialFactSummaryDto summary = service.getFinancialSummary(tenantId, companyId, null, null, null);

        assertThat(summary.totalOrders()).isEqualTo(2);
        assertThat(summary.totalGrossRevenue()).isEqualByComparingTo("300.0000");
        assertThat(summary.totalNetRevenue()).isEqualByComparingTo("300.0000");
        assertThat(summary.totalContributionMarginAmount()).isEqualByComparingTo("124.0000"); // 44 + 80
        assertThat(summary.reconciledCount()).isEqualTo(1);
        assertThat(summary.estimatedCount()).isEqualTo(1);
        assertThat(summary.ordersWithMissingDataCount()).isEqualTo(1);
        assertThat(summary.antiHallucinationDisclaimer()).contains("Margem de Contribuição");
    }
}
