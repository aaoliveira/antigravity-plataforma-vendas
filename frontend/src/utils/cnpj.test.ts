import { describe, it, expect } from 'vitest';
import { cleanCnpj, formatCnpj, isValidCnpj, maskCnpjForList, sanitizeCnpjForLog } from './cnpj';

describe('CNPJ Utility Tests (Módulo 11 & Privacy Guard)', () => {
  const validCnpjs = [
    '33.000.167/0001-01', // Petrobras
    '00.000.000/0001-91', // Banco do Brasil
    '60.701.190/0001-04', // Itaú Unibanco
    '07.526.557/0001-00', // Ambev
    '02.429.144/0001-93', // Magazine Luiza
  ];

  const invalidCnpjs = [
    '11.111.111/1111-11', // Dígitos repetidos
    '00.000.000/0000-00', // Dígitos repetidos
    '33.000.167/0001-99', // Checksum incorreto
    '12.345.678/0001-00', // Inválido
    '1234567890123',       // Menos de 14 dígitos
    '123456789012345',     // Mais de 14 dígitos
    '',                    // Vazio
    null,                  // Nulo
  ];

  it('1. cleanCnpj deve extrair somente os números e truncar em 14 dígitos', () => {
    expect(cleanCnpj('33.000.167/0001-01')).toBe('33000167000101');
    expect(cleanCnpj('33.000.167/0001-01-extra')).toBe('33000167000101');
    expect(cleanCnpj('abc-123')).toBe('123');
    expect(cleanCnpj(null)).toBe('');
  });

  it('2. formatCnpj deve aplicar a máscara progressiva durante a digitação', () => {
    expect(formatCnpj('33')).toBe('33');
    expect(formatCnpj('33000')).toBe('33.000');
    expect(formatCnpj('33000167')).toBe('33.000.167');
    expect(formatCnpj('330001670001')).toBe('33.000.167/0001');
    expect(formatCnpj('33000167000101')).toBe('33.000.167/0001-01');
  });

  it('3. isValidCnpj deve aceitar CNPJs reais válidos com ou sem formatação', () => {
    for (const cnpj of validCnpjs) {
      expect(isValidCnpj(cnpj)).toBe(true);
      expect(isValidCnpj(cleanCnpj(cnpj))).toBe(true);
    }
  });

  it('4. isValidCnpj deve rejeitar CNPJs repetidos, checksums incorretos e comprimentos inválidos', () => {
    for (const cnpj of invalidCnpjs) {
      expect(isValidCnpj(cnpj)).toBe(false);
    }
  });

  it('5. maskCnpjForList deve ofuscar os números centrais preservando prefixo, filial e dígitos', () => {
    expect(maskCnpjForList('33000167000101')).toBe('33.***.***/0001-01');
    expect(maskCnpjForList('60.701.190/0001-04')).toBe('60.***.***/0001-04');
    expect(maskCnpjForList('123')).toBe('12.3');
  });

  it('6. sanitizeCnpjForLog deve ocultar o CNPJ para proteção em logs e telemetria', () => {
    expect(sanitizeCnpjForLog('33000167000101')).toBe('33******01');
    expect(sanitizeCnpjForLog(null)).toBe('[EMPTY_CNPJ]');
    expect(sanitizeCnpjForLog('12345')).toBe('[INVALID_CNPJ_REDACTED]');
  });
});
