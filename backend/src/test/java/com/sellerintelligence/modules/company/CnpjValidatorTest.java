package com.sellerintelligence.modules.company;

import com.sellerintelligence.modules.company.util.CnpjValidator;
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
            "60.701.190/0001-04", // Itaú Unibanco
            "07.526.557/0001-00", // Ambev
            "00000000000191",    // Sem máscara
            "33000167000101",
            "60701190000104"
    })
    @DisplayName("Deve validar como válidos CNPJs reais conhecidos com ou sem máscara")
    void shouldValidateValidCnpjs(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00.000.000/0000-00",
            "11.111.111/1111-11",
            "22.222.222/2222-22",
            "33.333.333/3333-33",
            "44.444.444/4444-44",
            "55.555.555/5555-55",
            "66.666.666/6666-66",
            "77.777.777/7777-77",
            "88.888.888/8888-88",
            "99.999.999/9999-99"
    })
    @DisplayName("Deve rejeitar sequências de dígitos repetidos")
    void shouldRejectRepeatedDigits(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "123",
            "1234567890123",       // 13 dígitos
            "123456789012345",     // 15 dígitos
            "00.000.000/0001-92",  // Dígito verificador incorreto
            "33.000.167/0001-99",  // Dígito verificador incorreto
            "abc.def.ghi/jklm-no"
    })
    @DisplayName("Deve rejeitar CNPJs com tamanho incorreto, letras ou dígitos verificadores inválidos")
    void shouldRejectInvalidCnpjs(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isFalse();
    }

    @Test
    @DisplayName("Deve formatar corretamente CNPJ de 14 dígitos")
    void shouldFormatCnpjCorrectly() {
        assertThat(CnpjValidator.format("00000000000191")).isEqualTo("00.000.000/0001-91");
        assertThat(CnpjValidator.format("33000167000101")).isEqualTo("33.000.167/0001-01");
    }

    @Test
    @DisplayName("Deve sanitizar string nula sem lançar NullPointerException")
    void shouldHandleNullGracefully() {
        assertThat(CnpjValidator.isValid(null)).isFalse();
        assertThat(CnpjValidator.clean(null)).isEmpty();
    }
}
