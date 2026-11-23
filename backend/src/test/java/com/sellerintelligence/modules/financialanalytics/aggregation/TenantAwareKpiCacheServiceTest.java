package com.sellerintelligence.modules.financialanalytics.aggregation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TenantAwareKpiCacheService - Testes de Isolamento e Ciclo de Vida do Cache")
class TenantAwareKpiCacheServiceTest {

    private TenantAwareKpiCacheService cacheService;
    private final UUID tenantAlpha = UUID.randomUUID();
    private final UUID tenantBeta = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cacheService = new TenantAwareKpiCacheService();
    }

    @Test
    @DisplayName("Deve armazenar e recuperar dados em cache para o mesmo tenant")
    void shouldStoreAndRetrieveForSameTenant() {
        String data = "KPI_RESULT_ALPHA";
        cacheService.put(tenantAlpha, "summary", "filter_1", data);

        Optional<String> cached = cacheService.get(tenantAlpha, "summary", "filter_1", String.class);
        assertThat(cached).isPresent().contains("KPI_RESULT_ALPHA");
        assertThat(cacheService.getHitCount()).isEqualTo(1);
        assertThat(cacheService.getMissCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Critério de Aceite: Cache não mistura tenant - Tenant Beta não lê cache de Tenant Alpha")
    void cacheShouldNeverMixTenants() {
        // Tenant Alpha armazena dados
        cacheService.put(tenantAlpha, "summary", "same_filter", "CONFIDENTIAL_ALPHA_DATA");

        // Tenant Beta tenta consultar exatamente a mesma chave/endpoint/filtro
        Optional<String> betaAccess = cacheService.get(tenantBeta, "summary", "same_filter", String.class);

        // Deve retornar vazio (miss) e nunca os dados de Alpha
        assertThat(betaAccess).isEmpty();
        assertThat(cacheService.getMissCount()).isEqualTo(1);

        // Tenant Beta armazena seus próprios dados
        cacheService.put(tenantBeta, "summary", "same_filter", "BETA_DATA");

        Optional<String> alphaCached = cacheService.get(tenantAlpha, "summary", "same_filter", String.class);
        Optional<String> betaCached = cacheService.get(tenantBeta, "summary", "same_filter", String.class);

        assertThat(alphaCached).contains("CONFIDENTIAL_ALPHA_DATA");
        assertThat(betaCached).contains("BETA_DATA");
    }

    @Test
    @DisplayName("Critério de Aceite: Invalidação seletiva por tenant - invalidar Alpha não afeta Beta")
    void selectiveInvalidationShouldOnlyAffectSpecifiedTenant() {
        cacheService.put(tenantAlpha, "summary", "f1", "DATA_ALPHA");
        cacheService.put(tenantBeta, "summary", "f1", "DATA_BETA");

        assertThat(cacheService.size()).isEqualTo(2);

        // Invalida apenas o Tenant Alpha
        long removed = cacheService.invalidateTenant(tenantAlpha);
        assertThat(removed).isEqualTo(1);
        assertThat(cacheService.getEvictionCount()).isEqualTo(1);

        // Cache de Alpha foi removido
        assertThat(cacheService.get(tenantAlpha, "summary", "f1", String.class)).isEmpty();

        // Cache de Beta permanece 100% íntegro e legível
        assertThat(cacheService.get(tenantBeta, "summary", "f1", String.class)).contains("DATA_BETA");
    }

    @Test
    @DisplayName("Deve expirar dados quando o TTL for ultrapassado")
    void shouldExpireDataAfterTtl() throws InterruptedException {
        // Armazena com TTL de 50ms
        cacheService.put(tenantAlpha, "summary", "short_ttl", "EXPIRES_SOON", Duration.ofMillis(50));

        Optional<String> immediately = cacheService.get(tenantAlpha, "summary", "short_ttl", String.class);
        assertThat(immediately).contains("EXPIRES_SOON");

        Thread.sleep(60);

        Optional<String> expired = cacheService.get(tenantAlpha, "summary", "short_ttl", String.class);
        assertThat(expired).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar operações sem tenantId obrigatório")
    void shouldRejectOperationsWithoutTenantId() {
        assertThatThrownBy(() -> cacheService.get(null, "summary", "f1", String.class))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");

        assertThatThrownBy(() -> cacheService.put(null, "summary", "f1", "DATA", Duration.ofMinutes(5)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");

        assertThatThrownBy(() -> cacheService.invalidateTenant(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
    }
}
