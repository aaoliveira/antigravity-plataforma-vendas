package com.sellerintelligence.modules.orderingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.modules.marketplaceadapters.api.MarketplaceAdapter;
import com.sellerintelligence.modules.marketplaceadapters.model.ExecutionContext;
import com.sellerintelligence.modules.orderingestion.*;
import com.sellerintelligence.modules.orderingestion.model.*;
import com.sellerintelligence.modules.orderingestion.operations.ReadOwnOrdersOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Motor central de ingestão e reconciliação de pedidos e itens próprios.
 * Garante idempotência por (tenant_id, marketplace, external_order_id),
 * paginação resiliente com checkpoint/cursor, tratamento de cancelamentos/devoluções,
 * emissão transacional de outbox events e desvio de falhas irrecuperáveis para a DLQ.
 */
@Service
public class OrderSyncService {

    private static final Logger log = LoggerFactory.getLogger(OrderSyncService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderSyncCheckpointRepository checkpointRepository;
    private final OrderOutboxEventRepository outboxEventRepository;
    private final OrderDlqRecordRepository dlqRecordRepository;
    private final ObjectMapper objectMapper;

    public OrderSyncService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderSyncCheckpointRepository checkpointRepository,
            OrderOutboxEventRepository outboxEventRepository,
            OrderDlqRecordRepository dlqRecordRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.checkpointRepository = checkpointRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.dlqRecordRepository = dlqRecordRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Sincroniza pedidos próprios a partir do adapter oficial com paginação, checkpoint e reconciliação.
     */
    @Transactional
    public OrderSyncResult syncOwnOrders(
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            int pageSize,
            MarketplaceAdapter adapter,
            BiFunction<OrderPageRequest, ExecutionContext, OrderPageResponse> pageFetcher
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        Objects.requireNonNull(adapter, "adapter não pode ser nulo");
        Objects.requireNonNull(pageFetcher, "pageFetcher não pode ser nulo");

        int safePageSize = pageSize > 0 ? pageSize : 50;

        // Recupera ou cria checkpoint de sincronização para o tenant, conta e canal
        OrderSyncCheckpoint checkpoint = checkpointRepository
                .findByTenantIdAndSellerAccountIdAndMarketplace(tenantId, sellerAccountId, marketplace)
                .orElseGet(() -> checkpointRepository.save(new OrderSyncCheckpoint(
                        UUID.randomUUID(),
                        tenantId,
                        sellerAccountId,
                        marketplace,
                        "IN_PROGRESS",
                        null,
                        0,
                        safePageSize,
                        0
                )));

        ReadOwnOrdersOperation operation = new ReadOwnOrdersOperation(pageFetcher);
        ExecutionContext context = new ExecutionContext(
                tenantId,
                sellerAccountId.toString(),
                null,
                UUID.randomUUID().toString()
        );
        OrderPageRequest request = new OrderPageRequest(
                sellerAccountId,
                safePageSize,
                checkpoint.getCursorToken(),
                checkpoint.getLastOffset()
        );

        OrderPageResponse response;
        try {
            response = adapter.execute(operation, request, context);
        } catch (Exception e) {
            log.error("Erro na execução da operação de busca de pedidos: tenant={} account={} mkt={}",
                    tenantId, sellerAccountId, marketplace, e);
            checkpoint.fail(e.getMessage());
            checkpointRepository.save(checkpoint);
            throw new OrderSyncException("Falha na sincronização de pedidos: " + e.getMessage(), e);
        }

        int created = 0;
        int updated = 0;
        int dlqCount = 0;

        List<RawOrder> orders = response.orders();
        for (RawOrder rawOrder : orders) {
            // Validação de integridade do payload
            if (rawOrder == null || rawOrder.externalOrderId() == null || rawOrder.externalOrderId().isBlank()) {
                log.warn("Payload corrompido ou externalOrderId ausente detectado: tenant={} account={}",
                        tenantId, sellerAccountId);
                OrderDlqRecord dlqRecord = new OrderDlqRecord(
                        UUID.randomUUID(),
                        tenantId,
                        sellerAccountId,
                        marketplace,
                        null,
                        rawOrder != null ? rawOrder.rawPayloadJson() : null,
                        "externalOrderId ausente ou inválido no payload de pedido"
                );
                dlqRecordRepository.save(dlqRecord);
                dlqCount++;
                continue;
            }

            Optional<Order> existingOpt = orderRepository
                    .findByTenantIdAndMarketplaceAndExternalOrderId(tenantId, marketplace, rawOrder.externalOrderId());

            if (existingOpt.isPresent()) {
                // Reconciliação de pedido existente
                Order existing = existingOpt.get();
                OrderStatus oldStatus = existing.getStatus();
                OrderStatus newStatus = rawOrder.status() != null ? rawOrder.status() : oldStatus;

                if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
                    existing.markCancelled(rawOrder.cancellationReason(), rawOrder.cancelledAt());
                    emitOutboxEvent(tenantId, "ORDER_CANCELLED", existing);
                } else if (newStatus == OrderStatus.RETURNED && oldStatus != OrderStatus.RETURNED) {
                    existing.markReturned(rawOrder.returnedAt());
                    emitOutboxEvent(tenantId, "ORDER_RETURNED", existing);
                } else if (newStatus != oldStatus) {
                    existing.reconcile(
                            newStatus,
                            rawOrder.subtotal(),
                            rawOrder.shippingCost(),
                            rawOrder.totalAmount(),
                            rawOrder.trackingNumber(),
                            rawOrder.cancellationReason(),
                            rawOrder.cancelledAt(),
                            rawOrder.returnedAt(),
                            rawOrder.rawPayloadJson()
                    );
                    emitOutboxEvent(tenantId, "ORDER_STATUS_CHANGED", existing);
                } else {
                    existing.reconcile(
                            newStatus,
                            rawOrder.subtotal(),
                            rawOrder.shippingCost(),
                            rawOrder.totalAmount(),
                            rawOrder.trackingNumber(),
                            rawOrder.cancellationReason(),
                            rawOrder.cancelledAt(),
                            rawOrder.returnedAt(),
                            rawOrder.rawPayloadJson()
                    );
                }

                orderRepository.save(existing);

                // Reconciliação dos itens do pedido (substituição idempotente)
                orderItemRepository.deleteByTenantIdAndOrderId(tenantId, existing.getId());
                persistOrderItems(tenantId, existing.getId(), rawOrder.items());

                updated++;
            } else {
                // Ingestão de novo pedido
                UUID orderId = UUID.randomUUID();
                Order newOrder = new Order(
                        orderId,
                        tenantId,
                        sellerAccountId,
                        marketplace,
                        rawOrder.externalOrderId(),
                        rawOrder.status() != null ? rawOrder.status() : OrderStatus.PENDING,
                        rawOrder.subtotal(),
                        rawOrder.shippingCost(),
                        rawOrder.totalAmount(),
                        rawOrder.currency(),
                        rawOrder.buyerName(),
                        rawOrder.buyerDocument(),
                        rawOrder.trackingNumber(),
                        rawOrder.cancellationReason(),
                        rawOrder.cancelledAt(),
                        rawOrder.returnedAt(),
                        rawOrder.rawPayloadJson(),
                        rawOrder.schemaVersion(),
                        rawOrder.origin() != null ? rawOrder.origin() : marketplace,
                        rawOrder.orderCreatedAt(),
                        Instant.now()
                );
                orderRepository.save(newOrder);

                persistOrderItems(tenantId, orderId, rawOrder.items());

                emitOutboxEvent(tenantId, "ORDER_SYNCED", newOrder);
                created++;
            }
        }

        // Atualização do checkpoint
        int totalProcessed = orders.size();
        checkpoint.updateProgress(
                response.nextCursorToken(),
                checkpoint.getLastOffset() + totalProcessed,
                totalProcessed
        );

        if (!response.hasMore() || totalProcessed == 0) {
            checkpoint.complete();
        }
        checkpointRepository.save(checkpoint);

        log.info("Sincronização de pedidos concluída: tenant={} account={} mkt={} total={} created={} updated={} dlq={}",
                tenantId, sellerAccountId, marketplace, totalProcessed, created, updated, dlqCount);

        return new OrderSyncResult(
                tenantId,
                sellerAccountId,
                marketplace,
                totalProcessed,
                created,
                updated,
                dlqCount,
                checkpoint.getSyncStatus()
        );
    }

    private void persistOrderItems(UUID tenantId, UUID orderId, List<RawOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (RawOrderItem item : items) {
            OrderItem orderItem = new OrderItem(
                    UUID.randomUUID(),
                    tenantId,
                    orderId,
                    null, // vinculação com catálogo canônico opcional
                    null,
                    item.externalListingId(),
                    item.externalItemId(),
                    item.sku(),
                    item.title() != null ? item.title() : "Item sem título",
                    item.unitPrice() != null ? item.unitPrice() : BigDecimal.ZERO,
                    item.quantity() != null ? item.quantity() : 1,
                    item.totalPrice() != null ? item.totalPrice() : BigDecimal.ZERO
            );
            orderItemRepository.save(orderItem);
        }
    }

    private void emitOutboxEvent(UUID tenantId, String eventType, Order order) {
        try {
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("orderId", order.getId().toString());
            payloadMap.put("tenantId", tenantId.toString());
            payloadMap.put("sellerAccountId", order.getSellerAccountId().toString());
            payloadMap.put("marketplace", order.getMarketplace());
            payloadMap.put("externalOrderId", order.getExternalOrderId());
            payloadMap.put("status", order.getStatus().name());
            payloadMap.put("totalAmount", order.getTotalAmount().toPlainString());
            payloadMap.put("currency", order.getCurrency());
            payloadMap.put("syncedAt", order.getSyncedAt().toString());

            String payloadJson = objectMapper.writeValueAsString(payloadMap);

            OrderOutboxEvent outboxEvent = new OrderOutboxEvent(
                    UUID.randomUUID(),
                    tenantId,
                    eventType,
                    "ORDER",
                    order.getId(),
                    payloadJson
            );
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.error("Falha ao serializar evento para outbox: orderId={}", order.getId(), e);
            throw new OrderSyncException("Falha ao gerar evento de outbox para pedido: " + order.getId(), e);
        }
    }
}
