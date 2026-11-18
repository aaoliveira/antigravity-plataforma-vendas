package com.sellerintelligence.modules.productmonitoring.model;

import java.time.Instant;
import java.util.UUID;

public record MonitoringJobExecutionDto(
        UUID id,
        UUID itemId,
        UUID groupId,
        String jobType,
        String marketplace,
        String sourcePolicyId,
        JobExecutionStatus executionStatus,
        String skipReason,
        Instant executedAt,
        Long durationMs
) {
}
