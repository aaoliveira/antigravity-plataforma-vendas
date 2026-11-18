package com.sellerintelligence.modules.marketplaceconnection.exception;

/**
 * Exceção lançada quando ocorre falha no fluxo de autorização ou troca de tokens da Shopee Open Platform.
 */
public class ShopeeOAuthException extends RuntimeException {

    private final String errorCode;

    public ShopeeOAuthException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "shopee_oauth_error";
    }

    public ShopeeOAuthException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : "shopee_oauth_error";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
