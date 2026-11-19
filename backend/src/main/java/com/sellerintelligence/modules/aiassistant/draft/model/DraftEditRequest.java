package com.sellerintelligence.modules.aiassistant.draft.model;

import jakarta.validation.constraints.NotBlank;

public record DraftEditRequest(
        @NotBlank(message = "reviewedTitle é obrigatório")
        String reviewedTitle,
        @NotBlank(message = "reviewedDescription é obrigatório")
        String reviewedDescription,
        String reviewNotes
) {
}
