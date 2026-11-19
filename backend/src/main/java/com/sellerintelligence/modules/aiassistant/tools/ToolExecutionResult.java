package com.sellerintelligence.modules.aiassistant.tools;

import java.time.Instant;
import java.util.List;

/**
 * Resultado estruturado da execução de uma Tool de IA com proveniência e timestamps.
 */
public record ToolExecutionResult(
        String status, // SUCCESS, UNAVAILABLE, INSUFFICIENT_DATA, ACCESS_DENIED, ERROR
        Object data,
        List<String> sources,
        String unavailabilityReason,
        Instant executedAt
) {
    public static ToolExecutionResult success(Object data, List<String> sources) {
        return new ToolExecutionResult("SUCCESS", data, sources, null, Instant.now());
    }

    public static ToolExecutionResult unavailable(String reason, List<String> sources) {
        return new ToolExecutionResult("UNAVAILABLE", null, sources, reason, Instant.now());
    }

    public static ToolExecutionResult insufficientData(String reason, List<String> sources) {
        return new ToolExecutionResult("INSUFFICIENT_DATA", null, sources, reason, Instant.now());
    }

    public static ToolExecutionResult accessDenied(String reason) {
        return new ToolExecutionResult("ACCESS_DENIED", null, List.of("SecurityGuard"), reason, Instant.now());
    }

    public static ToolExecutionResult error(String reason) {
        return new ToolExecutionResult("ERROR", null, List.of("System"), reason, Instant.now());
    }
}
