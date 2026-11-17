package br.com.sellerintelligence.discovery.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EanChecksumValidatorTest {

    private EanChecksumValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EanChecksumValidator();
    }

    @Test
    @DisplayName("Should validate correct EAN-13 barcodes with valid modulo 10 checksum")
    void shouldValidateCorrectEan13() {
        // Exemplo padrão EAN-13 válido: 7891234567895
        assertThat(validator.isValidEan("7891234567895")).isTrue();
        // Coca-cola Brasil: 7894900011517
        assertThat(validator.isValidEan("7894900011517")).isTrue();
    }

    @Test
    @DisplayName("Should reject EAN-13 with invalid check digit")
    void shouldRejectInvalidEanCheckDigit() {
        // Alterando o último dígito verificador de 5 para 4
        assertThat(validator.isValidEan("7891234567894")).isFalse();
        // Dígito incorreto
        assertThat(validator.isValidEan("7894900011510")).isFalse();
    }

    @Test
    @DisplayName("Should validate valid EAN-8 barcodes")
    void shouldValidateCorrectEan8() {
        // EAN-8 válido: 96385074
        assertThat(validator.isValidEan("96385074")).isTrue();
    }

    @Test
    @DisplayName("Should reject null, blank or ill-formed barcodes")
    void shouldRejectIllFormedCodes() {
        assertThat(validator.isValidEan(null)).isFalse();
        assertThat(validator.isValidEan("")).isFalse();
        assertThat(validator.isValidEan("12345")).isFalse();
        assertThat(validator.isValidEan("ABC7891234567")).isFalse();
    }
}
