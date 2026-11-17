package br.com.sellerintelligence.ai.provider;

import java.util.List;
import java.util.Map;

public record ListingAiOutput(
        String textResult,
        Map<String, String> confirmedAttributesUsed,
        List<String> missingAttributesIdentified,
        String rationale,
        String modelUsed,
        long latencyMs
) {}
