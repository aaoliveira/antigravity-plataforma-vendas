package com.sellerintelligence.modules.aiassistant.draft.model;

import jakarta.validation.constraints.NotBlank;

public record DraftRejectRequest(
        @NotBlank(message = "rejectionReason é obrigatório")
        String rejectionReason
) {
}
