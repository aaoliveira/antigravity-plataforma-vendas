package com.sellerintelligence.core.tenant;

import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário para isolamento estrito de armazenamento de arquivos (S3 / MinIO / Local).
 * Garante que caminhos obedeçam à estrutura /tenants/{tenantId}/... e bloqueia categoricamente
 * ataques de Path Traversal (../) ou tentativas de acesso a arquivos de outras organizações.
 */
public final class TenantStoragePathResolver {

    private static final String STORAGE_PREFIX = "tenants";
    private static final Pattern TENANT_PATH_PATTERN = Pattern.compile(
            "^/?tenants/([0-9a-fA-F-]{36})/(.*)$"
    );

    private TenantStoragePathResolver() {
    }

    /**
     * Monta um caminho seguro e padronizado para armazenamento de arquivos do tenant.
     *
     * @param tenantId identificador único do tenant
     * @param category categoria ou subdiretório (ex: "reports", "exports", "invoices")
     * @param fileName nome do arquivo
     * @return caminho seguro no formato /tenants/{tenantId}/{category}/{fileName}
     */
    public static String resolvePath(UUID tenantId, String category, String fileName) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        validateInputString("category", category);
        validateInputString("fileName", fileName);

        assertNoPathTraversal(category);
        assertNoPathTraversal(fileName);

        String cleanCategory = category.trim().replaceAll("^/+|/+$", "");
        String cleanFileName = Paths.get(fileName.trim()).getFileName().toString();

        return String.format("/%s/%s/%s/%s", STORAGE_PREFIX, tenantId, cleanCategory, cleanFileName);
    }

    /**
     * Valida se um determinado caminho de storage pertence legitimamente ao tenant especificado.
     *
     * @param currentTenantId tenant ativo realizando a requisição
     * @param storagePath caminho completo do arquivo a ser acessado
     * @throws TenantAccessDeniedException caso o arquivo pertença a outro tenant
     * @throws IllegalArgumentException caso o formato do caminho seja inválido ou contenha path traversal
     */
    public static void validateAccess(UUID currentTenantId, String storagePath) {
        Objects.requireNonNull(currentTenantId, "currentTenantId não pode ser nulo");
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("storagePath não pode ser nulo ou vazio");
        }

        assertNoPathTraversal(storagePath);

        Matcher matcher = TENANT_PATH_PATTERN.matcher(storagePath.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Caminho de storage com estrutura inválida: " + storagePath);
        }

        UUID pathTenantId;
        try {
            pathTenantId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tenant ID inválido no caminho de storage: " + matcher.group(1), ex);
        }

        if (!currentTenantId.equals(pathTenantId)) {
            throw new TenantAccessDeniedException(
                    String.format("Acesso negado a arquivo de outro tenant: esperado=%s, arquivo_pertence_a=%s",
                            currentTenantId, pathTenantId)
            );
        }
    }

    /**
     * Extrai o UUID do tenant a partir de um caminho de storage.
     *
     * @param storagePath caminho de storage
     * @return UUID do tenant proprietário
     */
    public static UUID extractTenantIdFromPath(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("storagePath não pode ser nulo ou vazio");
        }

        assertNoPathTraversal(storagePath);

        Matcher matcher = TENANT_PATH_PATTERN.matcher(storagePath.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Caminho de storage inválido para extração de tenant: " + storagePath);
        }

        return UUID.fromString(matcher.group(1));
    }

    private static void validateInputString(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " não pode ser nulo ou vazio");
        }
    }

    private static void assertNoPathTraversal(String path) {
        if (path.contains("..") || path.contains("\\") || path.contains("\0")) {
            throw new IllegalArgumentException("Detectada tentativa maliciosa de Path Traversal no caminho: " + path);
        }
    }
}
