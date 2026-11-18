package com.sellerintelligence.modules.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSanitizerTest {

    @Test
    @DisplayName("Deve retornar null se a entrada for null")
    void testNullInput() {
        assertThat(AuditSanitizer.sanitize(null)).isNull();
    }

    @Test
    @DisplayName("Deve mascarar senhas em formato JSON")
    void testSanitizeJsonPassword() {
        String json = "{\"email\": \"seller@example.com\", \"password\": \"SuperSecret#123!\"}";
        String sanitized = AuditSanitizer.sanitize(json);

        assertThat(sanitized).doesNotContain("SuperSecret#123!");
        assertThat(sanitized).contains("\"password\": \"[REDACTED]\"");
    }

    @Test
    @DisplayName("Deve mascarar tokens em formato JSON")
    void testSanitizeJsonTokens() {
        String json = "{\"access_token\": \"abc123xyz\", \"refresh_token\": \"refresh987\"}";
        String sanitized = AuditSanitizer.sanitize(json);

        assertThat(sanitized).doesNotContain("abc123xyz");
        assertThat(sanitized).doesNotContain("refresh987");
        assertThat(sanitized).contains("\"access_token\": \"[REDACTED]\"");
        assertThat(sanitized).contains("\"refresh_token\": \"[REDACTED]\"");
    }

    @Test
    @DisplayName("Deve mascarar credenciais em formato key=value")
    void testSanitizeKeyValueCredentials() {
        String query = "user=john&password=SecretPass123&token=myToken999&other=normal";
        String sanitized = AuditSanitizer.sanitize(query);

        assertThat(sanitized).doesNotContain("SecretPass123");
        assertThat(sanitized).doesNotContain("myToken999");
        assertThat(sanitized).contains("password=[REDACTED]");
        assertThat(sanitized).contains("token=[REDACTED]");
        assertThat(sanitized).contains("other=normal");
    }

    @Test
    @DisplayName("Deve mascarar Bearer tokens e JWTs soltos")
    void testSanitizeBearerAndJwt() {
        String rawJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String text = "Authorization: Bearer " + rawJwt + " and loose token: " + rawJwt;
        String sanitized = AuditSanitizer.sanitize(text);

        assertThat(sanitized).doesNotContain("SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
        assertThat(sanitized).contains("[REDACTED]");
    }

    @Test
    @DisplayName("Não deve alterar strings comuns que não contenham segredos")
    void testPreserveNormalStrings() {
        String normal = "Tenant switch completed for tenant-id: 11111111-1111-1111-1111-111111111111";
        assertThat(AuditSanitizer.sanitize(normal)).isEqualTo(normal);
    }
}
