/**
 * Validador e formatador determinístico de CNPJ (formato e dígitos verificadores).
 * IMPORTANTE: Esta validação é estritamente algorítmica (sintaxe e dígitos).
 * NÃO consulta órgãos externos e NÃO declara regularidade ou situação cadastral perante a Receita Federal.
 */

const WEIGHTS_FIRST_DIGIT = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
const WEIGHTS_SECOND_DIGIT = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

export function cleanCnpj(cnpj: string): string {
  return cnpj ? cnpj.replace(/\D/g, '') : '';
}

export function isValidCnpj(cnpj: string): boolean {
  if (!cnpj) return false;
  const digits = cleanCnpj(cnpj);
  if (digits.length !== 14) return false;

  // Rejeita sequências repetidas (ex: 00000000000000, 11111111111111)
  if (/^(\d)\1{13}$/.test(digits)) return false;

  const calcDigit = (sliceLen: number, weights: number[]): number => {
    let sum = 0;
    for (let i = 0; i < sliceLen; i++) {
      sum += parseInt(digits[i], 10) * weights[i];
    }
    const rem = sum % 11;
    return rem < 2 ? 0 : 11 - rem;
  };

  const d1 = calcDigit(12, WEIGHTS_FIRST_DIGIT);
  if (d1 !== parseInt(digits[12], 10)) return false;

  const d2 = calcDigit(13, WEIGHTS_SECOND_DIGIT);
  return d2 === parseInt(digits[13], 10);
}

export function formatCnpj(cnpj: string): string {
  const digits = cleanCnpj(cnpj);
  if (digits.length !== 14) return cnpj;
  return digits.replace(
    /^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/,
    '$1.$2.$3/$4-$5'
  );
}
