package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitário para geração e validação de parâmetros PKCE (Proof Key for Code Exchange — RFC 7636).
 */
public final class PkceUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";

    private PkceUtil() {
        // Utility class
    }

    /**
     * Gera um novo par PKCE (code_verifier e code_challenge S256).
     */
    public static PkceChallenge generateChallenge() {
        // 64 bytes geram ~86 caracteres Base64URL, atendendo perfeitamente o range de 43-128 caracteres da RFC 7636
        byte[] verifierBytes = new byte[64];
        SECURE_RANDOM.nextBytes(verifierBytes);
        String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);

        String codeChallenge = computeS256Challenge(codeVerifier);

        return new PkceChallenge(codeVerifier, codeChallenge, CODE_CHALLENGE_METHOD_S256);
    }

    /**
     * Calcula o code_challenge a partir do code_verifier usando SHA-256 e Base64URL.
     */
    public static String computeS256Challenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível na JVM", e);
        }
    }
}
