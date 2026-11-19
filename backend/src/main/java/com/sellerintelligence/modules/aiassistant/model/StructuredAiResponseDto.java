package com.sellerintelligence.modules.aiassistant.model;

import com.sellerintelligence.modules.aiassistant.gateway.AiProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resposta estruturada emitida pelo Gateway de IA, contendo auditoria, fontes,
 * timestamps e declaração explícita de indisponibilidade quando couber.
 */
public record StructuredAiResponseDto(
        UUID executionId,
        UUID tenantId,
        AiProvider provider,
        String model,
        String promptKey,
        String promptVersion,
        String analysisText,
        Object structuredData,
        List<String> sources,
        Map<String, Instant> timestamps,
        List<String> toolsExecuted,
        AiUsageMetricsDto usageMetrics,
        List<String> unavailabilityNotes,
        String antiHallucinationDisclaimer
) {
}
