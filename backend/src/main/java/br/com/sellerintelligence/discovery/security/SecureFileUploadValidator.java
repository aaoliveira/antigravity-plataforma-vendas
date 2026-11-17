package br.com.sellerintelligence.discovery.security;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Arrays;

/**
 * Validador de uploads seguros de catálogos e imagens.
 * Inspeciona magic bytes reais e previne ataques de path traversal e negação de serviço por arquivo grande.
 */
@Component
public class SecureFileUploadValidator {

    public static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB

    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    public void validateSize(long fileSizeBytes) {
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("Arquivo vazio ou tamanho inválido.");
        }
        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format("Tamanho do arquivo (%d bytes) excede o limite máximo permitido de 25 MB.", fileSizeBytes));
        }
    }

    public String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "catalog.pdf";
        }

        // Previne Path Traversal (Directory Traversal)
        String safeName = originalFilename.replace("\\", "/");
        if (safeName.contains("/")) {
            safeName = safeName.substring(safeName.lastIndexOf('/') + 1);
        }

        safeName = safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeName.startsWith(".") || safeName.isBlank()) {
            safeName = "file_" + System.currentTimeMillis() + ".pdf";
        }
        return safeName;
    }

    public String detectAndValidateMimeType(byte[] fileHeader) {
        if (fileHeader == null || fileHeader.length < 5) {
            throw new IllegalArgumentException("Cabeçalho do arquivo insuficiente para detecção de tipo seguro.");
        }

        if (matchesHeader(fileHeader, PDF_MAGIC)) {
            return "application/pdf";
        }
        if (matchesHeader(fileHeader, PNG_MAGIC)) {
            return "image/png";
        }
        if (matchesHeader(fileHeader, JPEG_MAGIC)) {
            return "image/jpeg";
        }

        throw new IllegalArgumentException("Tipo de arquivo não permitido. Apenas PDF, PNG e JPEG são aceitos.");
    }

    private boolean matchesHeader(byte[] fileHeader, byte[] expectedMagic) {
        if (fileHeader.length < expectedMagic.length) {
            return false;
        }
        for (int i = 0; i < expectedMagic.length; i++) {
            if (fileHeader[i] != expectedMagic[i]) {
                return false;
            }
        }
        return true;
    }
}
