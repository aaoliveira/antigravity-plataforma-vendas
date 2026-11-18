package com.sellerintelligence.modules.marketplaceconnection.exception;

/**
 * Exceção lançada quando a Shopee Open Platform atinge limites de taxa ou quotas (error_rate_limit / error_limit).
 */
public class ShopeeRateLimitException extends RuntimeException {

    private final String errorCode;
    private final long retryAfterSeconds;

    public ShopeeRateLimitException(String errorCode, String message, long retryAfterSeconds) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : "error_rate_limit";
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
