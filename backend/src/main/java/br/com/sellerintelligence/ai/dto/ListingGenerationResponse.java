package br.com.sellerintelligence.ai.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ListingGenerationResponse(
        UUID listingId,
        String promptType,
        String promptVersion,
        String modelName,
        String suggestedTitle,
        String suggestedDescription,
        Map<String, String> confirmedAttributesUsed,
        List<String> missingAttributesIdentified,
        String rationale,
        String dataProvenance, // AI_GENERATED
        String disclaimer
) {}
