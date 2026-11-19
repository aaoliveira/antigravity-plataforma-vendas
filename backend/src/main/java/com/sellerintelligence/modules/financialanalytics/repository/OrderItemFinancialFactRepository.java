package com.sellerintelligence.modules.financialanalytics.repository;

import com.sellerintelligence.modules.financialanalytics.entity.OrderItemFinancialFactRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderItemFinancialFactRepository extends JpaRepository<OrderItemFinancialFactRecord, UUID> {

    List<OrderItemFinancialFactRecord> findAllByTenantIdAndOrderFactIdOrderByCreatedAtAsc(UUID tenantId, UUID orderFactId);

    Optional<OrderItemFinancialFactRecord> findByTenantIdAndOrderItemId(UUID tenantId, UUID orderItemId);

    List<OrderItemFinancialFactRecord> findAllByTenantIdAndOrderId(UUID tenantId, UUID orderId);
}
