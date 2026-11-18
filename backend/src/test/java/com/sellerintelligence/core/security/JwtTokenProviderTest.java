package com.sellerintelligence.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "test-secret-key-that-must-be-at-least-256-bits-long-for-hmac-sha256!";
    private final long expirationSeconds = 900;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationSeconds, new ObjectMapper());
    }

    @Test
    @DisplayName("Deve gerar e validar token JWT com sucesso extraindo claims corretas")
    void shouldGenerateAndValidateTokenSuccessfully() {
        UUID userId = UUID.randomUUID();
        String email = "test@seller.com";
        UUID tenantId = UUID.randomUUID();
        List<String> roles = List.of("ROLE_OWNER", "ROLE_ADMIN");

        String token = jwtTokenProvider.generateToken(userId, email, tenantId, roles);
        assertThat(token).isNotBlank();

        JwtTokenProvider.Claims claims = jwtTokenProvider.validateAndParseToken(token);
        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.email()).isEqualTo(email);
        assertThat(claims.tenantId()).isEqualTo(tenantId);
        assertThat(claims.roles()).containsExactly("ROLE_OWNER", "ROLE_ADMIN");
        assertThat(claims.expirationEpochSecond()).isGreaterThan(System.currentTimeMillis() / 1000);
    }

    @Test
    @DisplayName("Deve rejeitar token quando a assinatura HMAC-SHA256 for adulterada")
    void shouldRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId, "tamper@seller.com", UUID.randomUUID(), List.of("ROLE_USER"));

        // Adultera a assinatura (último caractere)
        String tamperedToken = token.substring(0, token.length() - 1) + "X";

        assertThatThrownBy(() -> jwtTokenProvider.validateAndParseToken(tamperedToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Assinatura do token JWT adulterada ou inválida");
    }

    @Test
    @DisplayName("Deve rejeitar token quando o tempo de expiração tiver sido ultrapassado")
    void shouldRejectExpiredToken() {
        UUID userId = UUID.randomUUID();
        // Gera token com expiração negativa (-10 segundos atrás)
        String expiredToken = jwtTokenProvider.generateToken(userId, "expired@seller.com", UUID.randomUUID(), List.of("ROLE_USER"), -10);

        assertThatThrownBy(() -> jwtTokenProvider.validateAndParseToken(expiredToken))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("Token JWT expirado");
    }

    @Test
    @DisplayName("Deve rejeitar token com formato malformado ou nulo")
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> jwtTokenProvider.validateAndParseToken(null))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token JWT ausente ou vazio");

        assertThatThrownBy(() -> jwtTokenProvider.validateAndParseToken("invalido.sem.tres.partes.extras"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Estrutura do token JWT inválida");
    }
}
