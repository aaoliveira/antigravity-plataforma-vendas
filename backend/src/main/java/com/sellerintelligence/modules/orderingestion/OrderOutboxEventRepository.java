package com.sellerintelligence.modules.orderingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderOutboxEventRepository extends JpaRepository<OrderOutboxEvent, UUID> {

    List<OrderOutboxEvent> findByTenantIdAndStatus(UUID tenantId, String status);

    List<OrderOutboxEvent> findByTenantIdAndAggregateId(UUID tenantId, UUID aggregateId);

    List<OrderOutboxEvent> findAllByTenantId(UUID tenantId);
}
