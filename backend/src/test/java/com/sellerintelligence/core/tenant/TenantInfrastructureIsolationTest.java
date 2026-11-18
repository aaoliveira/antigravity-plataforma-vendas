package com.sellerintelligence.core.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantInfrastructureIsolationTest {

    @Test
    @DisplayName("TenantJobExecutor deve definir e limpar o contexto do tenant")
    void testTenantJobExecutor() throws Exception {
        TenantJobExecutor executor = new TenantJobExecutor();
        UUID jobTenant = UUID.randomUUID();
        AtomicReference<UUID> capturedTenant = new AtomicReference<>();

        executor.execute(jobTenant, () -> {
            capturedTenant.set(TenantContextHolder.getRequiredTenantId());
        });

        assertThat(capturedTenant.get()).isEqualTo(jobTenant);
        assertThat(TenantContextHolder.get()).isEmpty();

        String result = executor.execute(jobTenant, () -> {
            return "job-ok-" + TenantContextHolder.getRequiredTenantId();
        });
        assertThat(result).isEqualTo("job-ok-" + jobTenant);
        assertThat(TenantContextHolder.get()).isEmpty();
    }

    @Test
    @DisplayName("TenantCacheKeyGenerator deve gerar chaves com namespace estrito")
    void testTenantCacheKeyGenerator() {
        UUID tenantId = UUID.randomUUID();
        String key = TenantCacheKeyGenerator.generateKey(tenantId, "connections", "12345");

        assertThat(key).isEqualTo("tenant:" + tenantId + ":connections:12345");

        assertThatThrownBy(() -> TenantCacheKeyGenerator.generateKey(null, "entity", "id"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("TenantQueueValidator deve validar e bloquear mensagens sem header ou cross-tenant")
    void testTenantQueueValidator() {
        UUID tenantAlpha = UUID.randomUUID();
        UUID tenantBeta = UUID.randomUUID();

        Map<String, Object> validHeaders = new HashMap<>();
        validHeaders.put(TenantQueueValidator.HEADER_TENANT_ID, tenantAlpha.toString());

        assertThat(TenantQueueValidator.extractTenantId(validHeaders)).isEqualTo(tenantAlpha);

        // Deve passar quando os tenants conferem
        TenantQueueValidator.validateTenantMatch(validHeaders, tenantAlpha);

        // Deve lançar TenantAccessDeniedException quando cross-tenant
        assertThatThrownBy(() -> TenantQueueValidator.validateTenantMatch(validHeaders, tenantBeta))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Tentativa de consumo cross-tenant bloqueada");

        // Deve lançar TenantContextMissingException se o cabeçalho estiver ausente
        Map<String, Object> emptyHeaders = new HashMap<>();
        assertThatThrownBy(() -> TenantQueueValidator.extractTenantId(emptyHeaders))
                .isInstanceOf(TenantContextMissingException.class);
    }

    @Test
    @DisplayName("TenantStoragePathResolver deve bloquear path traversal e acessos cross-tenant")
    void testTenantStoragePathResolver() {
        UUID tenantAlpha = UUID.randomUUID();
        UUID tenantBeta = UUID.randomUUID();

        String pathAlpha = TenantStoragePathResolver.resolvePath(tenantAlpha, "reports", "vendas.csv");
        assertThat(pathAlpha).isEqualTo("/tenants/" + tenantAlpha + "/reports/vendas.csv");

        // Tenant Alpha acessando seu próprio arquivo -> OK
        TenantStoragePathResolver.validateAccess(tenantAlpha, pathAlpha);

        // Tenant Beta tentando acessar o arquivo de Alpha -> Bloqueado
        assertThatThrownBy(() -> TenantStoragePathResolver.validateAccess(tenantBeta, pathAlpha))
                .isInstanceOf(TenantAccessDeniedException.class)
                .hasMessageContaining("Acesso negado a arquivo de outro tenant");

        // Tentativa de path traversal -> Bloqueado
        assertThatThrownBy(() -> TenantStoragePathResolver.resolvePath(tenantAlpha, "../etc", "passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path Traversal");

        assertThatThrownBy(() -> TenantStoragePathResolver.validateAccess(tenantAlpha, "/tenants/" + tenantAlpha + "/../../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
