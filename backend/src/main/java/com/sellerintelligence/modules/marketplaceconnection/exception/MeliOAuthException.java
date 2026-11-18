package com.sellerintelligence.modules.marketplaceconnection.exception;

/**
 * Exceção lançada em caso de falha de comunicação ou resposta de erro do Mercado Livre OAuth.
 */
public class MeliOAuthException extends RuntimeException {

    private final String errorCode;

    public MeliOAuthException(String message) {
        super(message);
        this.errorCode = "OAUTH_ERROR";
    }

    public MeliOAuthException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MeliOAuthException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "OAUTH_COMMUNICATION_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
