/**
 * Utilitários para validação sintática, formatação, mascaramento protetivo
 * e sanitização de CNPJs no frontend da Seller Intelligence Platform.
 * 
 * Cumpre a regra anti-alucinação: validação puramente algorítmica (Módulo 11),
 * sem consultar serviços de terceiros e sem inferir situações cadastrais/fiscais.
 */

const REPEATED_CNPJS = new Set([
  '00000000000000',
  '11111111111111',
  '22222222222222',
  '33333333333333',
  '44444444444444',
  '55555555555555',
  '66666666666666',
  '77777777777777',
  '88888888888888',
  '99999999999999',
]);

const WEIGHTS_FIRST_DIGIT = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
const WEIGHTS_SECOND_DIGIT = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

/**
 * Remove qualquer caractere não numérico da string de CNPJ.
 */
export function cleanCnpj(value: string | null | undefined): string {
  if (!value) return '';
  return value.replace(/\D/g, '').slice(0, 14);
}

/**
 * Aplica máscara progressiva de CNPJ (00.000.000/0000-00) durante a digitação.
 */
export function formatCnpj(value: string | null | undefined): string {
  const digits = cleanCnpj(value);
  if (!digits) return '';

  if (digits.length <= 2) {
    return digits;
  }
  if (digits.length <= 5) {
    return `${digits.slice(0, 2)}.${digits.slice(2)}`;
  }
  if (digits.length <= 8) {
    return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5)}`;
  }
  if (digits.length <= 12) {
    return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8)}`;
  }
  return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8, 12)}-${digits.slice(12, 14)}`;
}

/**
 * Validação algorítmica sintática estrita de CNPJ pelo Módulo 11.
 * Retorna true apenas se possuir 14 dígitos válidos, não repetidos e com checksum correto.
 */
export function isValidCnpj(value: string | null | undefined): boolean {
  const digits = cleanCnpj(value);
  if (digits.length !== 14) {
    return false;
  }

  if (REPEATED_CNPJS.has(digits)) {
    return false;
  }

  // Primeiro dígito verificador
  let sum1 = 0;
  for (let i = 0; i < 12; i++) {
    sum1 += parseInt(digits.charAt(i), 10) * WEIGHTS_FIRST_DIGIT[i];
  }
  const rem1 = sum1 % 11;
  const d1 = rem1 < 2 ? 0 : 11 - rem1;

  if (d1 !== parseInt(digits.charAt(12), 10)) {
    return false;
  }

  // Segundo dígito verificador
  let sum2 = 0;
  for (let i = 0; i < 13; i++) {
    sum2 += parseInt(digits.charAt(i), 10) * WEIGHTS_SECOND_DIGIT[i];
  }
  const rem2 = sum2 % 11;
  const d2 = rem2 < 2 ? 0 : 11 - rem2;

  return d2 === parseInt(digits.charAt(13), 10);
}

/**
 * Mascara o CNPJ para exibição segura em listagens públicas/gerais (Ex: 12.***.*** / 0001-90).
 * Protege a privacidade de dados corporativos e previne exposição desnecessária em UI.
 */
export function maskCnpjForList(value: string | null | undefined): string {
  const digits = cleanCnpj(value);
  if (digits.length !== 14) {
    return formatCnpj(digits);
  }
  const prefix = digits.slice(0, 2);
  const branch = digits.slice(8, 12);
  const check = digits.slice(12, 14);
  return `${prefix}.***.***/${branch}-${check}`;
}

/**
 * Sanitiza valores de CNPJ para registros de telemetria, logs de debug ou relatórios,
 * garantindo que nenhum CNPJ completo vaze no console do cliente.
 */
export function sanitizeCnpjForLog(value: string | null | undefined): string {
  const digits = cleanCnpj(value);
  if (!digits) return '[EMPTY_CNPJ]';
  if (digits.length === 14) {
    return `${digits.slice(0, 2)}******${digits.slice(12)}`;
  }
  return '[INVALID_CNPJ_REDACTED]';
}
