package com.sellerintelligence.modules.aiassistant.gateway;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Parâmetros operacionais e limites de governança para execução de modelos de IA.
 */
public record AiModelConfig(
        String defaultModelName,
        int maxTokensLimit,
        double temperature,
        long timeoutSeconds,
        int maxRetries,
        BigDecimal costPer1kPromptTokensBrl,
        BigDecimal costPer1kCompletionTokensBrl
) {
    public static AiModelConfig forProvider(AiProvider provider) {
        return switch (provider) {
            case GEMINI -> new AiModelConfig(
                    "gemini-2.0-flash",
                    4096,
                    0.1,
                    20,
                    3,
                    new BigDecimal("0.000500"), // R$ 0,0005 por 1k tokens entrada
                    new BigDecimal("0.001500")  // R$ 0,0015 por 1k tokens saída
            );
            case OPENAI -> new AiModelConfig(
                    "gpt-4o-mini",
                    4096,
                    0.1,
                    20,
                    3,
                    new BigDecimal("0.000900"),
                    new BigDecimal("0.003600")
            );
            case CLAUDE -> new AiModelConfig(
                    "claude-3-5-haiku",
                    4096,
                    0.1,
                    20,
                    3,
                    new BigDecimal("0.001500"),
                    new BigDecimal("0.006000")
            );
            case MOCK_PROVIDER -> new AiModelConfig(
                    "mock-deterministic-v1",
                    4096,
                    0.0,
                    5,
                    1,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        };
    }

    /**
     * Calcula o custo financeiro estimado em BRL para o volume de tokens informado.
     */
    public BigDecimal calculateEstimatedCost(int promptTokens, int completionTokens) {
        BigDecimal promptCost = costPer1kPromptTokensBrl
                .multiply(BigDecimal.valueOf(promptTokens))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_EVEN);

        BigDecimal completionCost = costPer1kCompletionTokensBrl
                .multiply(BigDecimal.valueOf(completionTokens))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_EVEN);

        return promptCost.add(completionCost);
    }
}
