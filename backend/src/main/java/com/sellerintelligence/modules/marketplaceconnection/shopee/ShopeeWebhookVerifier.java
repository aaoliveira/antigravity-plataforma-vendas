package com.sellerintelligence.modules.marketplaceconnection.shopee;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verificador oficial de assinaturas HMAC-SHA256 para Push Notifications (Webhooks) da Shopee Open Platform.
 * Base string: callback_url + "|" + raw_body
 */
@Component
public class ShopeeWebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(ShopeeWebhookVerifier.class);

    private final ShopeeOAuthProperties properties;

    public ShopeeWebhookVerifier(ShopeeOAuthProperties properties) {
        this.properties = properties;
    }

    /**
     * Valida a assinatura recebida no header Authorization da notificação de push da Shopee.
     *
     * @param authorizationHeader valor do cabeçalho HTTP "Authorization" enviado pela Shopee
     * @param rawBody corpo bruto da requisição HTTP (não serializado/sem alteração de formatação)
     * @param requestUrl URL completa do endpoint receptor configurado na Shopee
     * @return true se a assinatura for válida
     */
    public boolean verify(String authorizationHeader, String rawBody, String requestUrl) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.warn("Webhook da Shopee recebido sem cabeçalho Authorization");
            return false;
        }

        String partnerKey = properties.getPartnerKey();
        if (partnerKey == null || partnerKey.isBlank()) {
            log.warn("partnerKey não configurada para validação de webhooks da Shopee");
            return false;
        }

        String targetUrl = requestUrl != null && !requestUrl.isBlank() ? requestUrl : properties.getWebhookUrl();
        String baseString = ShopeeSignatureUtil.buildWebhookBaseString(targetUrl, rawBody);
        String expectedSignature = ShopeeSignatureUtil.calculateHmacSha256(baseString, partnerKey);

        boolean isValid = ShopeeSignatureUtil.verifySignature(expectedSignature, authorizationHeader);
        if (!isValid) {
            log.warn("Assinatura HMAC de webhook da Shopee inválida. URL: {}", targetUrl);
        }
        return isValid;
    }
}
