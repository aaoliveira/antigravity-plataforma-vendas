package br.com.sellerintelligence.tenancy.company;

import java.util.regex.Pattern;

/**
 * Deterministic CNPJ format and check-digit validator.
 * IMPORTANT: Validates only syntactical format and official check-digits.
 * Does NOT consult Receita Federal or imply active/regular tax registration status.
 */
public final class CnpjValidator {

    private static final int[] WEIGHTS_FIRST_DIGIT = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND_DIGIT = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^0-9]");

    private CnpjValidator() {
    }

    public static String clean(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return CLEAN_PATTERN.matcher(cnpj).replaceAll("");
    }

    public static boolean isValid(String cnpj) {
        if (cnpj == null) {
            return false;
        }

        String digits = clean(cnpj);
        if (digits.length() != 14) {
            return false;
        }

        // Reject repeated digits (e.g., 00000000000000, 11111111111111, etc.)
        boolean allSame = true;
        for (int i = 1; i < 14; i++) {
            if (digits.charAt(i) != digits.charAt(0)) {
                allSame = false;
                break;
            }
        }
        if (allSame) {
            return false;
        }

        int digit1 = calculateCheckDigit(digits, 12, WEIGHTS_FIRST_DIGIT);
        if (digit1 != Character.getNumericValue(digits.charAt(12))) {
            return false;
        }

        int digit2 = calculateCheckDigit(digits, 13, WEIGHTS_SECOND_DIGIT);
        return digit2 == Character.getNumericValue(digits.charAt(13));
    }

    public static String format(String cnpj) {
        String digits = clean(cnpj);
        if (digits.length() != 14) {
            return cnpj;
        }
        return String.format("%s.%s.%s/%s-%s",
                digits.substring(0, 2),
                digits.substring(2, 5),
                digits.substring(5, 8),
                digits.substring(8, 12),
                digits.substring(12, 14));
    }

    private static int calculateCheckDigit(String digits, int length, int[] weights) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
