package com.sellerintelligence.modules.aiassistant.draft.model;

import com.sellerintelligence.modules.aiassistant.gateway.AiProvider;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DraftGenerationRequest(
        UUID productId,
        UUID productVariantId,
        @NotBlank(message = "targetMarketplace é obrigatório")
        String targetMarketplace,
        DraftTone tone,
        String brand,
        String model,
        String category,
        List<String> materials,
        Map<String, String> dimensions,
        String warranty,
        Map<String, String> technicalSpecifications,
        List<String> confirmedBenefits,
        List<String> keywords,
        String promptTemplateId,
        AiProvider provider
) {
}
