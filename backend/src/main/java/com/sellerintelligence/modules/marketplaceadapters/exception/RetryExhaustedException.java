package com.sellerintelligence.modules.marketplaceadapters.exception;

/**
 * Lançada quando todas as tentativas de retry são esgotadas sem sucesso.
 */
public class RetryExhaustedException extends MarketplaceAdapterException {

    private final int attemptsMade;

    public RetryExhaustedException(String marketplaceId, int attemptsMade, Throwable lastError) {
        super(marketplaceId, String.format(
                "Número máximo de tentativas esgotado (%d tentativas) para o canal '%s'. Último erro: %s",
                attemptsMade, marketplaceId, lastError != null ? lastError.getMessage() : "Desconhecido"
        ), lastError);
        this.attemptsMade = attemptsMade;
    }

    public int getAttemptsMade() {
        return attemptsMade;
    }
}
