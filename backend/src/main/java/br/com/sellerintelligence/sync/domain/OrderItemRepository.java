package br.com.sellerintelligence.sync.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);
}
