package com.sellerintelligence.modules.marketplaceconnection.security;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Serviço de cifragem simétrica de tokens de marketplace (AES-256-GCM).
 * Aplica criptografia autenticada com vetor de inicialização (IV) de 12 bytes aleatório
 * e tag de autenticação de 128 bits para proteção contra adulteração e replay.
 */
@Service
public class TokenEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final KmsKeyProvider keyProvider;
    private final SecureRandom secureRandom;

    public TokenEncryptionService(KmsKeyProvider keyProvider) {
        this.keyProvider = Objects.requireNonNull(keyProvider, "keyProvider não pode ser nulo");
        this.secureRandom = new SecureRandom();
    }

    /**
     * Cifra um token sensível utilizando AES-256-GCM.
     *
     * @param plainToken token em texto puro
     * @returns representação Base64 contendo [IV (12B) + Ciphertext + Tag (16B)]
     */
    public String encrypt(String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            return plainToken;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = keyProvider.getMasterKey();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] cipherBytes = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherBytes);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(byteBuffer.array());
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Falha ao cifrar token de marketplace com AES-256-GCM", e);
        }
    }

    /**
     * Decifra um token armazenado no banco de dados.
     *
     * @param encryptedBase64 token cifrado em Base64
     * @returns token em texto puro
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            return encryptedBase64;
        }

        try {
            byte[] encryptedBytes = Base64.getUrlDecoder().decode(encryptedBase64);
            if (encryptedBytes.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
                throw new IllegalArgumentException("Payload cifrado inválido ou corrompido");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = keyProvider.getMasterKey();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(cipherBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new SecurityException("Falha ao decifrar token de marketplace: payload adulterado ou chave inválida", e);
        }
    }
}
