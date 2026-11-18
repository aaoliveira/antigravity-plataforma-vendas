package com.sellerintelligence.modules.productmonitoring.model;

import java.time.Instant;
import java.util.UUID;

public record MonitoringGroupDto(
        UUID id,
        String name,
        String description,
        String color,
        int minIntervalMinutes,
        int retentionDays,
        MonitoringGroupStatus status,
        int totalItems,
        Instant createdAt,
        Instant updatedAt
) {
}
