package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PkceUtilTest {

    @Test
    @DisplayName("Gera desafio PKCE em conformidade estrita com RFC 7636")
    void testGenerateChallengeRfcCompliance() {
        PkceChallenge challenge = PkceUtil.generateChallenge();

        assertThat(challenge).isNotNull();
        assertThat(challenge.codeVerifier()).isNotBlank();
        assertThat(challenge.codeChallenge()).isNotBlank();
        assertThat(challenge.codeChallengeMethod()).isEqualTo("S256");

        // RFC 7636 Section 4.1: length between 43 and 128 characters
        assertThat(challenge.codeVerifier().length())
                .isBetween(43, 128);

        // Deve conter apenas caracteres Base64URL seguros [A-Za-z0-9_-]
        assertThat(challenge.codeVerifier())
                .matches("^[A-Za-z0-9_-]+$");

        assertThat(challenge.codeChallenge())
                .matches("^[A-Za-z0-9_-]+$");

        // O code_challenge deve ser reproduzível a partir do code_verifier
        String recomputed = PkceUtil.computeS256Challenge(challenge.codeVerifier());
        assertThat(challenge.codeChallenge()).isEqualTo(recomputed);
    }
}
