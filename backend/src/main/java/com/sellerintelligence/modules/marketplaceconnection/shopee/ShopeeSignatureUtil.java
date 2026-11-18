package com.sellerintelligence.modules.marketplaceconnection.shopee;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Utilitário oficial de cálculo e validação de assinaturas HMAC-SHA256 para a Shopee Open Platform v2.
 * Atende às especificações oficiais de base string para APIs Públicas, Shop-Level e Webhooks Push.
 */
public final class ShopeeSignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private ShopeeSignatureUtil() {
        // Utility class
    }

    /**
     * Calcula o HMAC-SHA256 em hexadecimal minúsculo.
     *
     * @param baseString sequência exata de parâmetros a ser assinada
     * @param partnerKey chave secreta do parceiro (partner_key)
     * @return assinatura hexadecimal minúscula
     */
    public static String calculateHmacSha256(String baseString, String partnerKey) {
        if (baseString == null || partnerKey == null) {
            throw new IllegalArgumentException("baseString e partnerKey não podem ser nulos");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(partnerKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toLowerCase();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao calcular HMAC-SHA256 para Shopee Open Platform", e);
        }
    }

    /**
     * Base string para APIs Públicas e de Handshake de Autenticação:
     * partner_id + path + timestamp
     */
    public static String buildPublicBaseString(long partnerId, String path, long timestamp) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path é obrigatório");
        }
        return partnerId + path + timestamp;
    }

    /**
     * Base string para APIs no Nível de Loja (Shop Level):
     * partner_id + path + timestamp + access_token + shop_id
     */
    public static String buildShopBaseString(long partnerId, String path, long timestamp, String accessToken, long shopId) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path é obrigatório");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken é obrigatório para APIs de loja");
        }
        return partnerId + path + timestamp + accessToken + shopId;
    }

    /**
     * Base string para Webhooks / Push Mechanism:
     * callback_url + "|" + raw_body
     */
    public static String buildWebhookBaseString(String callbackUrl, String rawBody) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new IllegalArgumentException("callbackUrl é obrigatório para verificação de webhook");
        }
        return callbackUrl + "|" + (rawBody != null ? rawBody : "");
    }

    /**
     * Verificação de assinatura em tempo constante para proteção contra side-channel timing attacks.
     *
     * @param expectedSignature assinatura calculada internamente
     * @param incomingSignature assinatura recebida da requisição (header ou query param)
     * @return true se as assinaturas forem idênticas
     */
    public static boolean verifySignature(String expectedSignature, String incomingSignature) {
        if (expectedSignature == null || incomingSignature == null) {
            return false;
        }
        String cleanIncoming = incomingSignature.replaceFirst("(?i)^SHA256\\s+", "").trim();
        return MessageDigest.isEqual(
                expectedSignature.toLowerCase().getBytes(StandardCharsets.UTF_8),
                cleanIncoming.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }
}
