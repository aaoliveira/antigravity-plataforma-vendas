package com.sellerintelligence.modules.productmonitoring.repository;

import com.sellerintelligence.modules.productmonitoring.entity.MonitoringJobExecutionRecord;
import com.sellerintelligence.modules.productmonitoring.model.JobExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MonitoringJobExecutionRepository extends JpaRepository<MonitoringJobExecutionRecord, UUID> {

    Page<MonitoringJobExecutionRecord> findAllByTenantIdOrderByExecutedAtDesc(UUID tenantId, Pageable pageable);

    Page<MonitoringJobExecutionRecord> findAllByTenantIdAndExecutionStatusOrderByExecutedAtDesc(
            UUID tenantId, JobExecutionStatus executionStatus, Pageable pageable
    );
}
