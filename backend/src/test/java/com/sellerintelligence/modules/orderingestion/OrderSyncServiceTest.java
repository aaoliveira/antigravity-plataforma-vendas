package com.sellerintelligence.modules.orderingestion;

import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.marketplaceadapters.fake.FakeMarketplaceAdapter;
import com.sellerintelligence.modules.orderingestion.model.*;
import com.sellerintelligence.modules.orderingestion.service.OrderSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderSyncServiceTest {

    @Autowired
    private OrderSyncService orderSyncService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderSyncCheckpointRepository checkpointRepository;

    @Autowired
    private OrderOutboxEventRepository outboxEventRepository;

    @Autowired
    private OrderDlqRecordRepository dlqRecordRepository;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

    private UUID tenantId;
    private SellerAccount sellerAccount;
    private FakeMarketplaceAdapter fakeAdapter;

    @BeforeEach
    void setUp() {
        tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        sellerAccount = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                tenantId,
                "MERCADO_LIVRE",
                "ORDERS_TEST_SELLER_01",
                "Loja Oficial Pedidos Teste",
                "ACTIVE"
        ));

        fakeAdapter = FakeMarketplaceAdapter.createDefault();
    }

    @Test
    @DisplayName("1. Sincroniza pedidos e itens com idempotência e emissão de outbox event")
    void shouldSyncOrdersAndItemsWithIdempotencyAndOutbox() {
        RawOrderItem item1 = new RawOrderItem(
                "ITEM-001",
                "MLB-99901",
                "SKU-HEADSET",
                "Headset Gamer 7.1",
                new BigDecimal("299.90"),
                1,
                new BigDecimal("299.90")
        );

        RawOrder order1 = new RawOrder(
                "ORD-ML-1001",
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.PAID,
                new BigDecimal("299.90"),
                new BigDecimal("20.00"),
                new BigDecimal("319.90"),
                "BRL",
                "João Comprador",
                "***.123.456-**",
                "BR123456789XP",
                null,
                null,
                null,
                Instant.now().minusSeconds(3600),
                "{\"id\":\"ORD-ML-1001\",\"status\":\"paid\"}",
                "v1",
                "MERCADO_LIVRE_ORDERS_API",
                List.of(item1)
        );

        // Primeira sincronização
        OrderSyncResult result1 = orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(order1), null, false, 1)
        );

        assertThat(result1.ordersCreated()).isEqualTo(1);
        assertThat(result1.ordersUpdated()).isEqualTo(0);
        assertThat(result1.dlqCount()).isEqualTo(0);

        List<Order> orders = orderRepository.findAllByTenantId(tenantId);
        assertThat(orders).hasSize(1);
        Order persisted = orders.get(0);
        assertThat(persisted.getExternalOrderId()).isEqualTo("ORD-ML-1001");
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(persisted.getTotalAmount()).isEqualByComparingTo("319.90");

        List<OrderItem> items = orderItemRepository.findByTenantIdAndOrderId(tenantId, persisted.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getTitle()).isEqualTo("Headset Gamer 7.1");

        List<OrderOutboxEvent> outboxEvents = outboxEventRepository.findByTenantIdAndAggregateId(tenantId, persisted.getId());
        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getEventType()).isEqualTo("ORDER_SYNCED");

        // Segunda sincronização da mesma página (IDEMPOTÊNCIA)
        OrderSyncResult result2 = orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(order1), null, false, 1)
        );

        assertThat(result2.ordersCreated()).isEqualTo(0);
        assertThat(result2.ordersUpdated()).isEqualTo(1);

        // O total de pedidos e itens deve permanecer estritamente 1
        assertThat(orderRepository.findAllByTenantId(tenantId)).hasSize(1);
        assertThat(orderItemRepository.findByTenantIdAndOrderId(tenantId, persisted.getId())).hasSize(1);
    }

    @Test
    @DisplayName("2. Reconcilia transição de status do pedido (PAID -> SHIPPED) e gera outbox event")
    void shouldReconcileOrderStatusChange() {
        RawOrder initialOrder = new RawOrder(
                "ORD-ML-2001",
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.PAID,
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                new BigDecimal("150.00"),
                "BRL",
                "Maria Compradora",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "{\"status\":\"paid\"}",
                "v1",
                "MERCADO_LIVRE",
                List.of()
        );

        orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(initialOrder), null, false, 1)
        );

        // Atualização posterior com status SHIPPED e código de rastreamento
        RawOrder updatedOrder = new RawOrder(
                "ORD-ML-2001",
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.SHIPPED,
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                new BigDecimal("150.00"),
                "BRL",
                "Maria Compradora",
                null,
                "TRACK-BR-999",
                null,
                null,
                null,
                Instant.now(),
                "{\"status\":\"shipped\"}",
                "v1",
                "MERCADO_LIVRE",
                List.of()
        );

        OrderSyncResult result = orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(updatedOrder), null, false, 1)
        );

        assertThat(result.ordersUpdated()).isEqualTo(1);

        Order reconciled = orderRepository
                .findByTenantIdAndMarketplaceAndExternalOrderId(tenantId, "MERCADO_LIVRE", "ORD-ML-2001")
                .orElseThrow();
        assertThat(reconciled.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(reconciled.getTrackingNumber()).isEqualTo("TRACK-BR-999");

        List<OrderOutboxEvent> events = outboxEventRepository.findByTenantIdAndAggregateId(tenantId, reconciled.getId());
        assertThat(events).extracting(OrderOutboxEvent::getEventType)
                .contains("ORDER_SYNCED", "ORDER_STATUS_CHANGED");
    }

    @Test
    @DisplayName("3. Trata cancelamento de pedido com motivo e emite ORDER_CANCELLED")
    void shouldHandleOrderCancellation() {
        RawOrder paidOrder = new RawOrder(
                "ORD-ML-3001",
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.PAID,
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                new BigDecimal("200.00"),
                "BRL",
                "Carlos Comprador",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "{}",
                "v1",
                "MERCADO_LIVRE",
                List.of()
        );

        orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(paidOrder), null, false, 1)
        );

        Instant cancelledAt = Instant.now();
        RawOrder cancelledOrder = new RawOrder(
                "ORD-ML-3001",
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.CANCELLED,
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                new BigDecimal("200.00"),
                "BRL",
                "Carlos Comprador",
                null,
                null,
                "Comprador desistiu antes do envio",
                cancelledAt,
                null,
                Instant.now(),
                "{}",
                "v1",
                "MERCADO_LIVRE",
                List.of()
        );

        orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(cancelledOrder), null, false, 1)
        );

        Order cancelled = orderRepository
                .findByTenantIdAndMarketplaceAndExternalOrderId(tenantId, "MERCADO_LIVRE", "ORD-ML-3001")
                .orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("Comprador desistiu antes do envio");
        assertThat(cancelled.getCancelledAt()).isNotNull();

        List<OrderOutboxEvent> events = outboxEventRepository.findByTenantIdAndAggregateId(tenantId, cancelled.getId());
        assertThat(events).extracting(OrderOutboxEvent::getEventType).contains("ORDER_CANCELLED");
    }

    @Test
    @DisplayName("4. Trata devolução de pedido com data e emite ORDER_RETURNED")
    void shouldHandleOrderReturn() {
        RawOrder deliveredOrder = new RawOrder(
                "ORD-ML-4001",
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.DELIVERED,
                new BigDecimal("89.90"),
                BigDecimal.ZERO,
                new BigDecimal("89.90"),
                "BRL",
                "Ana Compradora",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "{}",
                "v1",
                "MERCADO_LIVRE",
                List.of()
        );

        orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(deliveredOrder), null, false, 1)
        );

        Instant returnedAt = Instant.now();
        RawOrder returnedOrder = new RawOrder(
                "ORD-ML-4001",
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.RETURNED,
                new BigDecimal("89.90"),
                BigDecimal.ZERO,
                new BigDecimal("89.90"),
                "BRL",
                "Ana Compradora",
                null,
                null,
                null,
                null,
                returnedAt,
                Instant.now(),
                "{}",
                "v1",
                "MERCADO_LIVRE",
                List.of()
        );

        orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(returnedOrder), null, false, 1)
        );

        Order returned = orderRepository
                .findByTenantIdAndMarketplaceAndExternalOrderId(tenantId, "MERCADO_LIVRE", "ORD-ML-4001")
                .orElseThrow();
        assertThat(returned.getStatus()).isEqualTo(OrderStatus.RETURNED);
        assertThat(returned.getReturnedAt()).isNotNull();

        List<OrderOutboxEvent> events = outboxEventRepository.findByTenantIdAndAggregateId(tenantId, returned.getId());
        assertThat(events).extracting(OrderOutboxEvent::getEventType).contains("ORDER_RETURNED");
    }

    @Test
    @DisplayName("5. Atualiza checkpoint de sincronização com cursor e marca COMPLETED")
    void shouldTrackCheckpointsWithCursor() {
        RawOrder order = new RawOrder(
                "ORD-ML-5001",
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.PAID,
                new BigDecimal("50.00"),
                BigDecimal.ZERO,
                new BigDecimal("50.00"),
                "BRL",
                "Cliente Teste",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "{}",
                "v1",
                "MERCADO_LIVRE",
                List.of()
        );

        OrderSyncResult result = orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(order), "cursor-next-page", false, 1)
        );

        assertThat(result.status()).isEqualTo("COMPLETED");

        OrderSyncCheckpoint cp = checkpointRepository
                .findByTenantIdAndSellerAccountIdAndMarketplace(tenantId, sellerAccount.getId(), "MERCADO_LIVRE")
                .orElseThrow();
        assertThat(cp.getSyncStatus()).isEqualTo("COMPLETED");
        assertThat(cp.getCursorToken()).isEqualTo("cursor-next-page");
        assertThat(cp.getTotalOrdersSynced()).isEqualTo(1);
    }

    @Test
    @DisplayName("6. Direciona pedidos com payload corrompido para DLQ sem abortar pedidos válidos")
    void shouldRouteCorruptedOrdersToDlq() {
        RawOrder validOrder = new RawOrder(
                "ORD-VALID-01",
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.PAID,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                "BRL",
                "Cliente Válido",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "{}",
                "v1",
                "MERCADO_LIVRE",
                List.of()
        );

        RawOrder corruptedOrder = new RawOrder(
                null, // externalOrderId ausente!
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                OrderStatus.PENDING,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "BRL",
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                "{\"invalid\":true}",
                "v1",
                "MERCADO_LIVRE",
                List.of()
        );

        OrderSyncResult result = orderSyncService.syncOwnOrders(
                tenantId,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(validOrder, corruptedOrder), null, false, 2)
        );

        assertThat(result.ordersCreated()).isEqualTo(1);
        assertThat(result.dlqCount()).isEqualTo(1);

        // Pedido válido persistido normalmente
        assertThat(orderRepository.findByTenantIdAndMarketplaceAndExternalOrderId(tenantId, "MERCADO_LIVRE", "ORD-VALID-01"))
                .isPresent();

        // Registro de falha na DLQ
        List<OrderDlqRecord> dlqRecords = dlqRecordRepository.findAllByTenantId(tenantId);
        assertThat(dlqRecords).hasSize(1);
        assertThat(dlqRecords.get(0).getErrorReason()).contains("externalOrderId ausente ou inválido");
        assertThat(dlqRecords.get(0).getStatus()).isEqualTo("UNRESOLVED");
    }
}
