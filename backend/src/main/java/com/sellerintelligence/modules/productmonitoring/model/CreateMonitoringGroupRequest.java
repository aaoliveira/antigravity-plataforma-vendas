package com.sellerintelligence.modules.productmonitoring.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMonitoringGroupRequest(
        @NotBlank(message = "O nome do grupo é obrigatório.")
        @Size(max = 120, message = "O nome não pode exceder 120 caracteres.")
        String name,

        @Size(max = 500, message = "A descrição não pode exceder 500 caracteres.")
        String description,

        @Size(max = 30, message = "A cor não pode exceder 30 caracteres.")
        String color,

        Integer minIntervalMinutes,

        Integer retentionDays
) {
    public CreateMonitoringGroupRequest {
        if (minIntervalMinutes == null || minIntervalMinutes < 15) {
            minIntervalMinutes = 60;
        }
        if (retentionDays == null || retentionDays < 7) {
            retentionDays = 90;
        }
        if (color == null || color.isBlank()) {
            color = "#3B82F6";
        }
    }
}
