package com.sellerintelligence.modules.reporting.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço de armazenamento privado com isolamento estrito de namespace por tenant
 * e proteção rigorosa contra Path Traversal (CWE-22).
 */
@Service
public class TenantStorageService {

    private final Path rootStoragePath;

    public TenantStorageService(
            @Value("${app.storage.export-dir:#{systemProperties['java.io.tmpdir'] + '/sellerintelligence-exports'}}") String rootDir
    ) {
        this.rootStoragePath = Paths.get(rootDir).toAbsolutePath().normalize();
        initRootDir();
    }

    private void initRootDir() {
        try {
            Files.createDirectories(rootStoragePath);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao inicializar diretório raiz de storage privado: " + rootStoragePath, e);
        }
    }

    /**
     * Resolve e valida o diretório privado do tenant, garantindo isolamento e ausência de traversal.
     */
    public Path resolveTenantExportDir(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        // Validação estrita do UUID para impedir caracteres de injeção de caminho
        String safeTenantName = tenantId.toString().replaceAll("[^a-zA-Z0-9-]", "");
        Path tenantDir = rootStoragePath.resolve("tenants").resolve(safeTenantName).resolve("exports").normalize();

        if (!tenantDir.startsWith(rootStoragePath)) {
            throw new SecurityException("Violação de segurança: Path Traversal detectado no tenantId: " + tenantId);
        }

        try {
            Files.createDirectories(tenantDir);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao criar diretório namespaced do tenant: " + tenantDir, e);
        }

        return tenantDir;
    }

    /**
     * Salva conteúdo no storage privado sob o namespace do tenant e retorna o checksum SHA-256 e o path relativo.
     */
    public StorageResult store(UUID tenantId, String safeFileName, byte[] content) {
        Objects.requireNonNull(safeFileName, "safeFileName não pode ser nulo");
        Objects.requireNonNull(content, "content não pode ser nulo");

        // Sanitiza e valida o nome do arquivo para impedir ../ ou caminhos aninhados
        if (safeFileName.contains("..") || safeFileName.contains("/") || safeFileName.contains("\\")) {
            throw new SecurityException("Violação de segurança: Path Traversal detectado no nome do arquivo: " + safeFileName);
        }
        String sanitizedFileName = Paths.get(safeFileName).getFileName().toString();

        Path tenantDir = resolveTenantExportDir(tenantId);
        Path targetPath = tenantDir.resolve(sanitizedFileName).normalize();

        if (!targetPath.startsWith(tenantDir)) {
            throw new SecurityException("Violação de segurança: Path Traversal detectado ao salvar arquivo: " + safeFileName);
        }

        try {
            Files.write(targetPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            String sha256 = calculateSha256(content);
            String relativeStoragePath = "tenants/" + tenantId + "/exports/" + sanitizedFileName;
            return new StorageResult(relativeStoragePath, targetPath.toString(), (long) content.length, sha256);
        } catch (IOException e) {
            throw new IllegalStateException("Falha de I/O ao gravar arquivo no storage do tenant: " + targetPath, e);
        }
    }

    /**
     * Carrega o arquivo armazenado validando que ele pertence ao namespace do tenant.
     */
    public Resource loadAsResource(UUID tenantId, String relativeOrAbsoluteStoragePath) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(relativeOrAbsoluteStoragePath, "storagePath não pode ser nulo");

        Path targetPath;
        if (Paths.get(relativeOrAbsoluteStoragePath).isAbsolute()) {
            targetPath = Paths.get(relativeOrAbsoluteStoragePath).normalize();
        } else {
            targetPath = rootStoragePath.resolve(relativeOrAbsoluteStoragePath).normalize();
        }

        Path tenantDir = resolveTenantExportDir(tenantId);
        if (!targetPath.startsWith(tenantDir)) {
            throw new SecurityException("Acesso negado: Tentativa de ler arquivo fora do namespace do tenant: " + tenantId);
        }

        if (!Files.exists(targetPath) || !Files.isReadable(targetPath)) {
            throw new IllegalStateException("Arquivo de exportação não encontrado ou inacessível no storage: " + targetPath);
        }

        return new FileSystemResource(targetPath);
    }

    /**
     * Exclui arquivo do storage com validação de namespace.
     */
    public boolean delete(UUID tenantId, String relativeOrAbsoluteStoragePath) {
        try {
            Resource resource = loadAsResource(tenantId, relativeOrAbsoluteStoragePath);
            return Files.deleteIfExists(resource.getFile().toPath());
        } catch (Exception e) {
            return false;
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível", e);
        }
    }

    public record StorageResult(
            String relativePath,
            String absolutePath,
            Long fileSizeBytes,
            String checksumSha256
    ) {}
}
