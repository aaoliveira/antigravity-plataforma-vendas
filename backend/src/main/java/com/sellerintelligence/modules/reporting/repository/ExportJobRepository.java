package com.sellerintelligence.modules.reporting.repository;

import com.sellerintelligence.modules.reporting.entity.ExportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {

    Optional<ExportJob> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ExportJob> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    Page<ExportJob> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<ExportJob> findAllByTenantId(UUID tenantId);
}
