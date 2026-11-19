package com.sellerintelligence.modules.reporting.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("P021-BE: Testes do TenantStorageService e Isolamento de Namespace")
class TenantStorageServiceTest {

    @TempDir
    Path tempDir;

    private TenantStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new TenantStorageService(tempDir.toString());
    }

    @Test
    void shouldStoreAndLoadFileSuccessfully() throws IOException {
        UUID tenantId = UUID.randomUUID();
        String fileName = "relatorio_pedidos.csv";
        byte[] content = "ID,Preco\n1,100.50".getBytes(StandardCharsets.UTF_8);

        TenantStorageService.StorageResult result = storageService.store(tenantId, fileName, content);

        assertThat(result.relativePath()).isEqualTo("tenants/" + tenantId + "/exports/" + fileName);
        assertThat(result.fileSizeBytes()).isEqualTo((long) content.length);
        assertThat(result.checksumSha256()).isNotEmpty();

        Resource loadedResource = storageService.loadAsResource(tenantId, result.relativePath());
        assertThat(loadedResource.exists()).isTrue();
        assertThat(loadedResource.getContentAsByteArray()).isEqualTo(content);
    }

    @Test
    void shouldRejectPathTraversalInFileName() {
        UUID tenantId = UUID.randomUUID();
        byte[] content = "malicious payload".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> storageService.store(tenantId, "../../malicious.sh", content))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path Traversal detectado");

        assertThatThrownBy(() -> storageService.store(tenantId, "nested/path.csv", content))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldRejectCrossTenantFileAccess() {
        UUID tenantAlpha = UUID.randomUUID();
        UUID tenantBeta = UUID.randomUUID();

        byte[] content = "dados confidenciais do tenant alpha".getBytes(StandardCharsets.UTF_8);
        TenantStorageService.StorageResult result = storageService.store(tenantAlpha, "alpha_data.csv", content);

        // Tenant Beta tenta carregar o arquivo do Tenant Alpha
        assertThatThrownBy(() -> storageService.loadAsResource(tenantBeta, result.relativePath()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acesso negado: Tentativa de ler arquivo fora do namespace do tenant");
    }

    @Test
    void shouldDeleteFileSuccessfully() {
        UUID tenantId = UUID.randomUUID();
        byte[] content = "arquivo temporario".getBytes(StandardCharsets.UTF_8);
        TenantStorageService.StorageResult result = storageService.store(tenantId, "to_delete.csv", content);

        boolean deleted = storageService.delete(tenantId, result.relativePath());
        assertThat(deleted).isTrue();

        assertThatThrownBy(() -> storageService.loadAsResource(tenantId, result.relativePath()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não encontrado ou inacessível");
    }
}
