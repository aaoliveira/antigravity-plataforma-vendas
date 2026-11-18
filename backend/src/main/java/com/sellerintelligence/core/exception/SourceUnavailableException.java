package com.sellerintelligence.core.exception;

/**
 * Exceção lançada quando uma fonte externa ou serviço de integração está indisponível,
 * inalcançável, sob rate-limit severo ou com circuit-breaker ativado.
 * Cumpre a regra anti-alucinação: "Não criar snapshots se a fonte estiver indisponível".
 */
public class SourceUnavailableException extends RuntimeException {

    private final String sourceIdentifier;

    public SourceUnavailableException(String message) {
        super(message);
        this.sourceIdentifier = null;
    }

    public SourceUnavailableException(String sourceIdentifier, String message) {
        super(message);
        this.sourceIdentifier = sourceIdentifier;
    }

    public SourceUnavailableException(String sourceIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.sourceIdentifier = sourceIdentifier;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }
}
