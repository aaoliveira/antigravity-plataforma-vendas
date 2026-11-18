package com.sellerintelligence.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BruteForceProtectionServiceTest {

    private BruteForceProtectionService service;

    @BeforeEach
    void setUp() {
        // 3 tentativas máximas, 60 segundos de bloqueio
        service = new BruteForceProtectionService(3, 60);
    }

    @Test
    @DisplayName("Não deve bloquear enquanto o número de tentativas com falha for menor que o limite")
    void shouldNotBlockUnderThreshold() {
        String key = "192.168.1.1:user@example.com";

        assertThat(service.isBlocked(key)).isFalse();

        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isFalse();

        service.recordFailure(key);
        assertThat(service.isBlocked(key)).isFalse();
    }

    @Test
    @DisplayName("Deve bloquear ao atingir o limite exato de tentativas com falha")
    void shouldBlockWhenThresholdReached() {
        String key = "192.168.1.1:user@example.com";

        service.recordFailure(key);
        service.recordFailure(key);
        service.recordFailure(key); // 3ª tentativa (limite)

        assertThat(service.isBlocked(key)).isTrue();
        assertThat(service.getRemainingLockoutSeconds(key)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Deve zerar as tentativas e desbloquear quando o login obtém sucesso")
    void shouldResetOnSuccess() {
        String key = "192.168.1.1:user@example.com";

        service.recordFailure(key);
        service.recordFailure(key);

        service.recordSuccess(key);

        assertThat(service.isBlocked(key)).isFalse();
    }
}
