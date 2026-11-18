package com.sellerintelligence.modules.marketplaceconnection.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Provedor de chave mestra de criptografia (KMS / Secret Manager Pattern).
 * Obtém a chave mestre a partir de variáveis de ambiente gerenciadas ou
 * deriva deterministicamente para ambientes isolados de teste.
 */
@Component
public class KmsKeyProvider {

    private final SecretKey masterSecretKey;

    public KmsKeyProvider(
            @Value("${security.token-encryption.master-key:}") String configuredKey
    ) {
        this.masterSecretKey = resolveSecretKey(configuredKey);
    }

    public SecretKey getMasterKey() {
        return masterSecretKey;
    }

    private SecretKey resolveSecretKey(String configuredKey) {
        byte[] keyBytes;
        if (configuredKey != null && !configuredKey.isBlank()) {
            String trimmed = configuredKey.trim();
            // Tenta decodificar como Hex se for 64 caracteres
            if (trimmed.length() == 64 && trimmed.matches("^[0-9a-fA-F]+$")) {
                keyBytes = HexFormat.of().parseHex(trimmed);
            } else {
                // Deriva 256 bits via SHA-256
                keyBytes = sha256(trimmed.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            // Chave de segurança padrão para testes e desenvolvimento local
            keyBytes = sha256("seller-intelligence-default-kms-dev-key".getBytes(StandardCharsets.UTF_8));
        }

        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM", e);
        }
    }
}
