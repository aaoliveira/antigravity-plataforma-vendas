package com.sellerintelligence.modules.marketplaceconnection.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptionServiceTest {

    private TokenEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        KmsKeyProvider keyProvider = new KmsKeyProvider("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        encryptionService = new TokenEncryptionService(keyProvider);
    }

    @Test
    @DisplayName("Cifra e decifra com sucesso utilizando AES-256-GCM")
    void testEncryptAndDecryptSuccess() {
        String plainToken = "APP_USR-987654321-111810-abcdef0123456789-123456789";

        String encrypted = encryptionService.encrypt(plainToken);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(plainToken);
        assertThat(encrypted).doesNotContain("APP_USR");

        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plainToken);
    }

    @Test
    @DisplayName("Cifragens sucessivas do mesmo token geram ciphertexts distintos devido ao IV aleatório")
    void testUniqueIvPerEncryption() {
        String plainToken = "TG-test-refresh-token-secret-12345";

        String encrypted1 = encryptionService.encrypt(plainToken);
        String encrypted2 = encryptionService.encrypt(plainToken);

        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // Ambos devem decifrar para o mesmo conteúdo original
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plainToken);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plainToken);
    }

    @Test
    @DisplayName("Tentativa de decifrar payload adulterado deve lançar SecurityException")
    void testTamperedPayloadThrowsSecurityException() {
        String plainToken = "secret-token-value";
        String encrypted = encryptionService.encrypt(plainToken);

        byte[] rawBytes = Base64.getUrlDecoder().decode(encrypted);
        // Altera o último byte (pertencente à authentication tag do GCM)
        rawBytes[rawBytes.length - 1] ^= 0xFF;
        String tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);

        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("payload adulterado ou chave inválida");
    }

    @Test
    @DisplayName("Tratamento de valores nulos ou vazios")
    void testNullAndBlankHandling() {
        assertThat(encryptionService.encrypt(null)).isNull();
        assertThat(encryptionService.encrypt("")).isEmpty();
        assertThat(encryptionService.decrypt(null)).isNull();
        assertThat(encryptionService.decrypt("")).isEmpty();
    }
}
