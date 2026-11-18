package com.sellerintelligence.modules.orderingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);

    void deleteByTenantIdAndOrderId(UUID tenantId, UUID orderId);

    List<OrderItem> findAllByTenantId(UUID tenantId);
}
