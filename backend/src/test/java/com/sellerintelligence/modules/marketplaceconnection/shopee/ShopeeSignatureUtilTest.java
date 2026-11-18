package com.sellerintelligence.modules.marketplaceconnection.shopee;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShopeeSignatureUtilTest {

    private static final String PARTNER_KEY = "test_partner_key_12345";
    private static final long PARTNER_ID = 100001L;

    @Test
    @DisplayName("1. Calcula base string pública conforme especificação oficial da Shopee v2")
    void testBuildPublicBaseString() {
        long timestamp = 1763467200L;
        String path = "/api/v2/auth/token/get";

        String baseString = ShopeeSignatureUtil.buildPublicBaseString(PARTNER_ID, path, timestamp);
        assertThat(baseString).isEqualTo("100001/api/v2/auth/token/get1763467200");
    }

    @Test
    @DisplayName("2. Calcula base string de loja (Shop Level) com access_token e shop_id")
    void testBuildShopBaseString() {
        long timestamp = 1763467200L;
        String path = "/api/v2/shop/get_shop_info";
        String accessToken = "shopee-token-xyz";
        long shopId = 987654321L;

        String baseString = ShopeeSignatureUtil.buildShopBaseString(PARTNER_ID, path, timestamp, accessToken, shopId);
        assertThat(baseString).isEqualTo("100001/api/v2/shop/get_shop_info1763467200shopee-token-xyz987654321");
    }

    @Test
    @DisplayName("3. Calcula base string de Webhook Push: callback_url + '|' + raw_body")
    void testBuildWebhookBaseString() {
        String callbackUrl = "https://app.sellerintelligence.com.br/api/v1/integrations/shopee/webhook";
        String rawBody = "{\"code\":2,\"shop_id\":987654321,\"timestamp\":1763467200}";

        String baseString = ShopeeSignatureUtil.buildWebhookBaseString(callbackUrl, rawBody);
        assertThat(baseString).isEqualTo("https://app.sellerintelligence.com.br/api/v1/integrations/shopee/webhook|{\"code\":2,\"shop_id\":987654321,\"timestamp\":1763467200}");
    }

    @Test
    @DisplayName("4. Gera assinatura HMAC-SHA256 determinística em hexadecimal minúsculo")
    void testCalculateHmacSha256() {
        String baseString = "100001/api/v2/auth/token/get1763467200";
        String sign = ShopeeSignatureUtil.calculateHmacSha256(baseString, PARTNER_KEY);

        assertThat(sign).isNotBlank();
        assertThat(sign).matches("^[0-9a-f]{64}$");
        // Verifica determinismo
        String sign2 = ShopeeSignatureUtil.calculateHmacSha256(baseString, PARTNER_KEY);
        assertThat(sign).isEqualTo(sign2);
    }

    @Test
    @DisplayName("5. Valida assinatura em tempo constante com suporte a prefixo SHA256 e case-insensitive")
    void testVerifySignature() {
        String baseString = "100001/api/v2/auth/token/get1763467200";
        String sign = ShopeeSignatureUtil.calculateHmacSha256(baseString, PARTNER_KEY);

        // Assinatura exata
        assertThat(ShopeeSignatureUtil.verifySignature(sign, sign)).isTrue();
        // Assinatura em maiúsculo
        assertThat(ShopeeSignatureUtil.verifySignature(sign, sign.toUpperCase())).isTrue();
        // Assinatura com prefixo SHA256
        assertThat(ShopeeSignatureUtil.verifySignature(sign, "SHA256 " + sign)).isTrue();

        // Assinatura adulterada
        assertThat(ShopeeSignatureUtil.verifySignature(sign, "forged_signature_1234567890abcdef")).isFalse();
        // Parâmetros nulos
        assertThat(ShopeeSignatureUtil.verifySignature(sign, null)).isFalse();
        assertThat(ShopeeSignatureUtil.verifySignature(null, sign)).isFalse();
    }

    @Test
    @DisplayName("6. Rejeita parâmetros nulos na geração de base strings e assinaturas")
    void testNullParameterValidations() {
        assertThatThrownBy(() -> ShopeeSignatureUtil.calculateHmacSha256(null, PARTNER_KEY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShopeeSignatureUtil.calculateHmacSha256("test", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShopeeSignatureUtil.buildPublicBaseString(PARTNER_ID, null, 12345L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShopeeSignatureUtil.buildShopBaseString(PARTNER_ID, "/path", 12345L, null, 999L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShopeeSignatureUtil.buildWebhookBaseString(null, "body"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
