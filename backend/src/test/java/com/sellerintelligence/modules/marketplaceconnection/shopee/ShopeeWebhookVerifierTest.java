package com.sellerintelligence.modules.marketplaceconnection.shopee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShopeeWebhookVerifierTest {

    private ShopeeOAuthProperties properties;
    private ShopeeWebhookVerifier verifier;

    private static final String PARTNER_KEY = "test_shopee_key_99999";
    private static final String WEBHOOK_URL = "https://app.sellerintelligence.com.br/api/v1/integrations/shopee/webhook";

    @BeforeEach
    void setUp() {
        properties = new ShopeeOAuthProperties();
        properties.setPartnerKey(PARTNER_KEY);
        properties.setWebhookUrl(WEBHOOK_URL);
        verifier = new ShopeeWebhookVerifier(properties);
    }

    @Test
    @DisplayName("1. Aprova webhook com assinatura HMAC-SHA256 válida gerada conforme base string oficial")
    void testValidWebhookSignature() {
        String rawBody = "{\"code\":2,\"shop_id\":12345678,\"timestamp\":1763467200,\"data\":{}}";
        String baseString = ShopeeSignatureUtil.buildWebhookBaseString(WEBHOOK_URL, rawBody);
        String validSign = ShopeeSignatureUtil.calculateHmacSha256(baseString, PARTNER_KEY);

        boolean result = verifier.verify(validSign, rawBody, WEBHOOK_URL);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("2. Rejeita webhook com corpo adulterado (tampered body)")
    void testTamperedWebhookBodyRejected() {
        String originalBody = "{\"code\":2,\"shop_id\":12345678,\"timestamp\":1763467200,\"data\":{}}";
        String baseString = ShopeeSignatureUtil.buildWebhookBaseString(WEBHOOK_URL, originalBody);
        String validSign = ShopeeSignatureUtil.calculateHmacSha256(baseString, PARTNER_KEY);

        String tamperedBody = "{\"code\":1,\"shop_id\":12345678,\"timestamp\":1763467200,\"data\":{}}";
        boolean result = verifier.verify(validSign, tamperedBody, WEBHOOK_URL);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("3. Rejeita webhook com cabeçalho Authorization ausente ou vazio")
    void testMissingAuthorizationHeaderRejected() {
        String rawBody = "{\"code\":2,\"shop_id\":12345678}";
        assertThat(verifier.verify(null, rawBody, WEBHOOK_URL)).isFalse();
        assertThat(verifier.verify("", rawBody, WEBHOOK_URL)).isFalse();
        assertThat(verifier.verify("   ", rawBody, WEBHOOK_URL)).isFalse();
    }

    @Test
    @DisplayName("4. Rejeita webhook se a URL de destino for divergente da registrada")
    void testMismatchedUrlRejected() {
        String rawBody = "{\"code\":2,\"shop_id\":12345678}";
        String baseString = ShopeeSignatureUtil.buildWebhookBaseString("https://divergent.url.com", rawBody);
        String sign = ShopeeSignatureUtil.calculateHmacSha256(baseString, PARTNER_KEY);

        boolean result = verifier.verify(sign, rawBody, WEBHOOK_URL);
        assertThat(result).isFalse();
    }
}
