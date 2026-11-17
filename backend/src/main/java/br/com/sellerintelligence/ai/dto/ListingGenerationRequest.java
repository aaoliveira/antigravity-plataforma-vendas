package br.com.sellerintelligence.ai.dto;

import java.util.List;
import java.util.Map;

public record ListingGenerationRequest(
        String promptType, // TITLE, DESCRIPTION
        List<String> targetKeywords,
        Map<String, String> additionalConfirmedAttributes
) {}
