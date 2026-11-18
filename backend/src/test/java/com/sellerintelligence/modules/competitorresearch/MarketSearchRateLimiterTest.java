package com.sellerintelligence.modules.competitorresearch;

import com.sellerintelligence.modules.competitorresearch.service.MarketSearchRateLimiter;
import com.sellerintelligence.modules.marketplaceadapters.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketSearchRateLimiterTest {

    private MarketSearchRateLimiter rateLimiter;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        rateLimiter = new MarketSearchRateLimiter();
        rateLimiter.resetForTest();
    }

    @Test
    @DisplayName("Deve permitir requisições dentro do limite por minuto e decrementar cota restante")
    void shouldAllowRequestsWithinLimit() {
        int limit = 5;
        String policyId = "POL-TEST-01";

        int remaining1 = rateLimiter.checkAndConsume(tenantId, policyId, limit);
        assertThat(remaining1).isEqualTo(4);

        int remaining2 = rateLimiter.checkAndConsume(tenantId, policyId, limit);
        assertThat(remaining2).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve lançar RateLimitExceededException quando limite for atingido")
    void shouldThrowRateLimitExceededExceptionWhenLimitIsReached() {
        int limit = 2;
        String policyId = "POL-TEST-01";

        rateLimiter.checkAndConsume(tenantId, policyId, limit);
        rateLimiter.checkAndConsume(tenantId, policyId, limit);

        assertThatThrownBy(() -> rateLimiter.checkAndConsume(tenantId, policyId, limit))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Limite de taxa excedido");
    }

    @Test
    @DisplayName("Não deve interferir entre tenants distintos")
    void shouldIsolateTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        String policyId = "POL-TEST-01";

        rateLimiter.checkAndConsume(tenantA, policyId, 1);

        // Tenant A esgotou
        assertThatThrownBy(() -> rateLimiter.checkAndConsume(tenantA, policyId, 1))
                .isInstanceOf(RateLimitExceededException.class);

        // Tenant B ainda tem cota
        int remainingB = rateLimiter.checkAndConsume(tenantB, policyId, 1);
        assertThat(remainingB).isEqualTo(0);
    }
}
