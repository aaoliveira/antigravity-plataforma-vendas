package com.sellerintelligence.modules.orderingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderDlqRecordRepository extends JpaRepository<OrderDlqRecord, UUID> {

    List<OrderDlqRecord> findAllByTenantId(UUID tenantId);

    List<OrderDlqRecord> findByTenantIdAndStatus(UUID tenantId, String status);

    long countByTenantIdAndStatus(UUID tenantId, String status);
}
