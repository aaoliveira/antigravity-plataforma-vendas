package com.sellerintelligence.modules.productmonitoring.model;

import jakarta.validation.constraints.Size;

public record UpdateMonitoringGroupRequest(
        @Size(max = 120, message = "O nome não pode exceder 120 caracteres.")
        String name,

        @Size(max = 500, message = "A descrição não pode exceder 500 caracteres.")
        String description,

        @Size(max = 30, message = "A cor não pode exceder 30 caracteres.")
        String color,

        Integer minIntervalMinutes,

        Integer retentionDays,

        MonitoringGroupStatus status
) {
}
