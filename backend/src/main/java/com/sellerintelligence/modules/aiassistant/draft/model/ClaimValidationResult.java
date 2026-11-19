package com.sellerintelligence.modules.aiassistant.draft.model;

import java.util.List;
import java.util.Map;

/**
 * Resultado da auditoria de claims e rastreabilidade de atributos confirmados (P020-BE).
 */
public record ClaimValidationResult(
        boolean valid,
        List<String> unprovenClaimsDetected,
        Map<String, String> traceableAttributes,
        String sanitizedTitle,
        String sanitizedDescription
) {
}
