package br.com.sellerintelligence.connector.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenCryptoServiceTest {

    private TokenCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new TokenCryptoService("test-secret-encryption-key-32b-length");
    }

    @Test
    @DisplayName("Encrypt and decrypt roundtrip restores original token")
    void encryptAndDecryptRoundtrip() {
        String plainToken = "APP_USR-1234567890-abcdef-oauth-token";
        String encrypted = cryptoService.encrypt(plainToken);

        assertThat(encrypted).isNotNull().isNotEqualTo(plainToken);
        assertThat(cryptoService.decrypt(encrypted)).isEqualTo(plainToken);
    }

    @Test
    @DisplayName("Encrypting twice produces distinct ciphertexts due to random IV (GCM)")
    void distinctIvPerEncryption() {
        String plainToken = "secret-token-same-content";
        String encrypted1 = cryptoService.encrypt(plainToken);
        String encrypted2 = cryptoService.encrypt(plainToken);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(cryptoService.decrypt(encrypted1)).isEqualTo(plainToken);
        assertThat(cryptoService.decrypt(encrypted2)).isEqualTo(plainToken);
    }

    @Test
    @DisplayName("Corrupted ciphertext throws exception and fails safely")
    void corruptedCiphertextFails() {
        assertThatThrownBy(() -> cryptoService.decrypt("not-a-valid-base64-or-gcm-string"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Null or blank input returns null safely")
    void nullOrBlankReturnsNull() {
        assertThat(cryptoService.encrypt(null)).isNull();
        assertThat(cryptoService.encrypt("")).isNull();
        assertThat(cryptoService.decrypt(null)).isNull();
        assertThat(cryptoService.decrypt("")).isNull();
    }
}
