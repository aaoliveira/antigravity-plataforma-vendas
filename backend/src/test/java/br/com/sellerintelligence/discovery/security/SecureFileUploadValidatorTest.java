package br.com.sellerintelligence.discovery.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureFileUploadValidatorTest {

    private SecureFileUploadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SecureFileUploadValidator();
    }

    @Test
    @DisplayName("Should detect and validate valid PDF magic bytes")
    void shouldValidatePdfMagicBytes() {
        byte[] pdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37}; // %PDF-1.7
        String mime = validator.detectAndValidateMimeType(pdfHeader);
        assertThat(mime).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should detect and validate PNG and JPEG magic bytes")
    void shouldValidateImageMagicBytes() {
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertThat(validator.detectAndValidateMimeType(pngHeader)).isEqualTo("image/png");

        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        assertThat(validator.detectAndValidateMimeType(jpegHeader)).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Should reject disallowed or executable files masquerading as PDF")
    void shouldRejectDisallowedFileTypes() {
        byte[] exeHeader = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03}; // MZ executable
        assertThatThrownBy(() -> validator.detectAndValidateMimeType(exeHeader))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de arquivo não permitido");
    }

    @Test
    @DisplayName("Should reject file exceeding 25 MB limit")
    void shouldRejectOversizedFiles() {
        long oversized = 26 * 1024 * 1024L; // 26 MB
        assertThatThrownBy(() -> validator.validateSize(oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("excede o limite máximo permitido de 25 MB");
    }

    @Test
    @DisplayName("Should sanitize filenames preventing path traversal attacks")
    void shouldPreventPathTraversalInFilenames() {
        String dangerous1 = "../../../../etc/passwd";
        assertThat(validator.sanitizeFilename(dangerous1)).isEqualTo("passwd");

        String dangerous2 = "..\\..\\windows\\system32\\calc.exe";
        assertThat(validator.sanitizeFilename(dangerous2)).isEqualTo("calc.exe");

        String dangerous3 = "/var/log/secret.pdf";
        assertThat(validator.sanitizeFilename(dangerous3)).isEqualTo("secret.pdf");
    }
}
