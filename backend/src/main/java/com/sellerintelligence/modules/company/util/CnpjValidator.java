package com.sellerintelligence.modules.company.util;

/**
 * Validador sintático e matemático de Cadastro Nacional da Pessoa Jurídica (CNPJ).
 * Executa conferência estrita dos dois dígitos verificadores via algoritmo módulo 11.
 * 
 * REGRA ANTI-ALUCINAÇÃO:
 * Esta classe valida única e exclusivamente a conformidade sintática do identificador.
 * O sistema NÃO assume nem deduz situação cadastral junto à Receita Federal,
 * enquadramento tributário (Simples, Presumido, Real) ou alíquota de impostos.
 */
public final class CnpjValidator {

    private static final int[] WEIGHTS_FIRST_DIGIT = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND_DIGIT = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
        // Utility class
    }

    /**
     * Sanitiza a string de CNPJ removendo caracteres não numéricos.
     */
    public static String clean(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return cnpj.replaceAll("\\D", "");
    }

    /**
     * Valida matematicamente a estrutura sintática e os dígitos verificadores do CNPJ.
     *
     * @param rawCnpj CNPJ com ou sem máscara
     * @return true se o CNPJ for matematicamente válido, false caso contrário
     */
    public static boolean isValid(String rawCnpj) {
        String cnpj = clean(rawCnpj);

        if (cnpj.length() != 14) {
            return false;
        }

        // Rejeita sequências repetidas conhecidas (00000000000000, 11111111111111...)
        if (isAllSameCharacters(cnpj)) {
            return false;
        }

        try {
            int firstCheckDigit = calculateDigit(cnpj.substring(0, 12), WEIGHTS_FIRST_DIGIT);
            int secondCheckDigit = calculateDigit(cnpj.substring(0, 12) + firstCheckDigit, WEIGHTS_SECOND_DIGIT);

            return cnpj.charAt(12) - '0' == firstCheckDigit &&
                   cnpj.charAt(13) - '0' == secondCheckDigit;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Formata um CNPJ de 14 dígitos no padrão canônico XX.XXX.XXX/XXXX-XX.
     */
    public static String format(String rawCnpj) {
        String clean = clean(rawCnpj);
        if (clean.length() != 14) {
            return rawCnpj; // Retorna original se não tiver 14 dígitos
        }
        return String.format("%s.%s.%s/%s-%s",
                clean.substring(0, 2),
                clean.substring(2, 5),
                clean.substring(5, 8),
                clean.substring(8, 12),
                clean.substring(12, 14)
        );
    }

    private static boolean isAllSameCharacters(String s) {
        char first = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    private static int calculateDigit(String base, int[] weights) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += (base.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
