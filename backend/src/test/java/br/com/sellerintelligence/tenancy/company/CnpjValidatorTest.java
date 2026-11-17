package br.com.sellerintelligence.tenancy.company;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "00.000.000/0001-91", // Banco do Brasil
            "33.000.167/0001-01", // Petrobras
            "06.990.590/0001-23", // Google Brasil
            "00000000000191",
            "33000167000101",
            "06990590000123"
    })
    @DisplayName("Valid CNPJs with correct check digits must pass validation")
    void validCnpjsPass(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00.000.000/0001-92", // Invalid check digit
            "33.000.167/0001-00", // Invalid check digit
            "11.111.111/1111-11", // Repeated digits
            "00.000.000/0000-00", // Repeated digits
            "12.345.678/0001",    // Too short
            "123456789012345",    // Too long (15 digits)
            "",
            "invalid-cnpj"
    })
    @DisplayName("Invalid or malformed CNPJs must fail validation")
    void invalidCnpjsFail(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isFalse();
    }

    @Test
    @DisplayName("Null CNPJ must fail validation safely")
    void nullCnpjFails() {
        assertThat(CnpjValidator.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("CNPJ formatting helper properly formats 14 digits")
    void formatCnpj() {
        assertThat(CnpjValidator.format("06990590000123")).isEqualTo("06.990.590/0001-23");
        assertThat(CnpjValidator.format("06.990.590/0001-23")).isEqualTo("06.990.590/0001-23");
    }
}
