package br.com.sellerintelligence.discovery.security;

import org.springframework.stereotype.Component;

/**
 * Validador determinístico de código de barras EAN-13, EAN-8 e GTIN-14.
 * Verifica o dígito verificador matemático (checksum módulo 10).
 */
@Component
public class EanChecksumValidator {

    public boolean isValidEan(String code) {
        if (code == null) {
            return false;
        }
        String clean = code.trim().replaceAll("[^0-9]", "");
        if (clean.length() != 8 && clean.length() != 12 && clean.length() != 13 && clean.length() != 14) {
            return false;
        }

        int length = clean.length();
        int sum = 0;
        for (int i = 0; i < length - 1; i++) {
            int digit = Character.getNumericValue(clean.charAt(i));
            // No cálculo de EAN, da direita para a esquerda antes do checksum os pesos alternam entre 3 e 1
            int weight = ((length - 1 - i) % 2 == 1) ? 3 : 1;
            sum += digit * weight;
        }

        int expectedCheckDigit = (10 - (sum % 10)) % 10;
        int actualCheckDigit = Character.getNumericValue(clean.charAt(length - 1));

        return expectedCheckDigit == actualCheckDigit;
    }
}
