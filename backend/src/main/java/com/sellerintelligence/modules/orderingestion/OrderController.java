package com.sellerintelligence.modules.orderingestion;

import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.orderingestion.model.*;
import com.sellerintelligence.modules.orderingestion.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller oficial para consulta e gestão de pedidos e itens próprios.
 * Garante filtros server-side, busca textual, paginação e isolamento estrito de tenant.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderDetailResponse>> searchOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) UUID sellerAccountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String search
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        int safeSize = Math.min(Math.max(1, size), 100);
        int safePage = Math.max(0, page);

        OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                orderStatus = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Status inválido é tratado como nulo
            }
        }

        Page<Order> orderPage = orderService.searchOrders(
                tenantId,
                marketplace,
                sellerAccountId,
                orderStatus,
                startDate,
                endDate,
                search,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "orderCreatedAt"))
        );

        List<OrderDetailResponse> dtoList = orderPage.getContent().stream()
                .map(order -> {
                    List<OrderItem> items = orderService.getOrderItems(order.getId(), tenantId);
                    return mapToDetailResponse(order, items);
                })
                .collect(Collectors.toList());

        PageResponse<OrderDetailResponse> response = new PageResponse<>(
                dtoList,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isLast()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getOrderById(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        Order order = orderService.getOrderById(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado para o tenant ativo"));

        List<OrderItem> items = orderService.getOrderItems(order.getId(), tenantId);

        return ResponseEntity.ok(mapToDetailResponse(order, items));
    }

    @GetMapping("/checkpoints")
    public ResponseEntity<List<OrderSyncCheckpointResponse>> getCheckpoints() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        List<OrderSyncCheckpointResponse> list = orderService.getCheckpoints(tenantId).stream()
                .map(cp -> new OrderSyncCheckpointResponse(
                        cp.getId(),
                        cp.getTenantId(),
                        cp.getSellerAccountId(),
                        cp.getMarketplace(),
                        cp.getSyncStatus(),
                        cp.getCursorToken(),
                        cp.getLastOffset(),
                        cp.getPageSize(),
                        cp.getTotalOrdersSynced(),
                        cp.getStartedAt(),
                        cp.getLastCheckpointAt(),
                        cp.getCompletedAt(),
                        cp.getErrorMessage()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    @GetMapping("/dlq")
    public ResponseEntity<List<OrderDlqRecordResponse>> getDlqRecords() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        List<OrderDlqRecordResponse> list = orderService.getDlqRecords(tenantId).stream()
                .map(dlq -> new OrderDlqRecordResponse(
                        dlq.getId(),
                        dlq.getTenantId(),
                        dlq.getSellerAccountId(),
                        dlq.getMarketplace(),
                        dlq.getExternalOrderId(),
                        dlq.getRawPayloadJson(),
                        dlq.getErrorReason(),
                        dlq.getRetryCount(),
                        dlq.getStatus(),
                        dlq.getCreatedAt(),
                        dlq.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    private OrderDetailResponse mapToDetailResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getExternalItemId(),
                        item.getExternalListingId(),
                        item.getSku(),
                        item.getTitle(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getTotalItemPrice()
                ))
                .collect(Collectors.toList());

        return new OrderDetailResponse(
                order.getId(),
                order.getTenantId(),
                order.getSellerAccountId(),
                order.getMarketplace(),
                order.getExternalOrderId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getShippingCost(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getBuyerName(),
                order.getBuyerDocument(),
                order.getTrackingNumber(),
                order.getCancellationReason(),
                order.getCancelledAt(),
                order.getReturnedAt(),
                order.getRawPayloadJson(),
                order.getSchemaVersion(),
                order.getOrigin(),
                order.getOrderCreatedAt(),
                order.getSyncedAt(),
                itemResponses
        );
    }
}
