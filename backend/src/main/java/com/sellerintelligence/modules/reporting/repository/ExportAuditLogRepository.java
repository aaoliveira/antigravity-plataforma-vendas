package com.sellerintelligence.modules.reporting.repository;

import com.sellerintelligence.modules.reporting.entity.ExportAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExportAuditLogRepository extends JpaRepository<ExportAuditLog, UUID> {

    Page<ExportAuditLog> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<ExportAuditLog> findAllByTenantIdAndExportJobIdOrderByCreatedAtDesc(UUID tenantId, UUID exportJobId);
}
