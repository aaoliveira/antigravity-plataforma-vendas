package com.sellerintelligence.modules.marketplaceconnection.exception;

/**
 * Exceção lançada quando a assinatura HMAC-SHA256 de um webhook da Shopee Open Platform é inválida ou ausente.
 */
public class ShopeeWebhookSignatureException extends RuntimeException {

    public ShopeeWebhookSignatureException(String message) {
        super(message);
    }
}
