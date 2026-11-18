package com.sellerintelligence.modules.orderingestion.service;

import com.sellerintelligence.modules.orderingestion.*;
import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de consulta e leitura de pedidos e seus itens para o tenant autenticado.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderSyncCheckpointRepository checkpointRepository;
    private final OrderDlqRecordRepository dlqRecordRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderSyncCheckpointRepository checkpointRepository,
            OrderDlqRecordRepository dlqRecordRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.checkpointRepository = checkpointRepository;
        this.dlqRecordRepository = dlqRecordRepository;
    }

    @Transactional(readOnly = true)
    public Page<Order> searchOrders(
            UUID tenantId,
            String marketplace,
            UUID sellerAccountId,
            OrderStatus status,
            Instant startDate,
            Instant endDate,
            String search,
            Pageable pageable
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        return orderRepository.searchOrders(
                tenantId,
                marketplace,
                sellerAccountId,
                status,
                startDate,
                endDate,
                search,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(UUID id, UUID tenantId) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        return orderRepository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(UUID orderId, UUID tenantId) {
        Objects.requireNonNull(orderId, "orderId não pode ser nulo");
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        return orderItemRepository.findByTenantIdAndOrderId(tenantId, orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderSyncCheckpoint> getCheckpoints(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        return checkpointRepository.findAllByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<OrderDlqRecord> getDlqRecords(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        return dlqRecordRepository.findAllByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public long countOrders(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        return orderRepository.countByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public long countOrdersByStatus(UUID tenantId, OrderStatus status) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(status, "status não pode ser nulo");
        return orderRepository.countByTenantIdAndStatus(tenantId, status);
    }
}
