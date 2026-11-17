import { describe, it, expect } from 'vitest';
import { cleanCnpj, isValidCnpj, formatCnpj } from './cnpj';

describe('CNPJ Utility', () => {
  it('cleans non-digit characters correctly', () => {
    expect(cleanCnpj('06.990.590/0001-23')).toBe('06990590000123');
    expect(cleanCnpj('abc-123')).toBe('123');
    expect(cleanCnpj('')).toBe('');
  });

  it('validates genuine CNPJs by check digits', () => {
    expect(isValidCnpj('06.990.590/0001-23')).toBe(true);
    expect(isValidCnpj('33.000.167/0001-01')).toBe(true);
    expect(isValidCnpj('00.000.000/0001-91')).toBe(true);
  });

  it('rejects invalid or malformed CNPJs', () => {
    expect(isValidCnpj('06.990.590/0001-99')).toBe(false); // wrong check digit
    expect(isValidCnpj('00.000.000/0000-00')).toBe(false); // repeated
    expect(isValidCnpj('11.111.111/1111-11')).toBe(false); // repeated
    expect(isValidCnpj('12345')).toBe(false); // too short
    expect(isValidCnpj('')).toBe(false);
  });

  it('formats clean 14 digits to standard mask', () => {
    expect(formatCnpj('06990590000123')).toBe('06.990.590/0001-23');
    expect(formatCnpj('06.990.590/0001-23')).toBe('06.990.590/0001-23');
  });
});
