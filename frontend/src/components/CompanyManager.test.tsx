import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { CompanyManager, CompanyItem } from './CompanyManager';

const mockCompanies: CompanyItem[] = [
  {
    id: '1',
    cnpj: '06.990.590/0001-23',
    tradeName: 'Matriz Alpha',
    legalName: 'Matriz Alpha Ltda',
    taxRegime: 'SIMPLES_NACIONAL',
    status: 'ACTIVE',
    createdAt: '2026-11-17T10:00:00Z',
  },
];

describe('CompanyManager Component', () => {
  it('renders company list correctly', () => {
    render(<CompanyManager initialCompanies={mockCompanies} />);

    expect(screen.getByText('Empresas e CNPJs')).toBeDefined();
    expect(screen.getByText('Matriz Alpha')).toBeDefined();
    expect(screen.getByText('06.990.590/0001-23')).toBeDefined();
    expect(screen.getByText('SIMPLES NACIONAL')).toBeDefined();
    expect(screen.getByText('Ativa')).toBeDefined();
  });

  it('renders informative disclaimer about CNPJ and tax regime', () => {
    render(<CompanyManager initialCompanies={mockCompanies} />);

    expect(
      screen.getByText(/A validação de CNPJ é estritamente algorítmica/i)
    ).toBeDefined();
  });

  it('toggles form visibility when clicking "+ Nova Empresa"', () => {
    render(<CompanyManager initialCompanies={mockCompanies} />);

    const button = screen.getByText('+ Nova Empresa');
    fireEvent.click(button);

    expect(screen.getByText('Cadastrar Nova Empresa')).toBeDefined();
    expect(screen.getByPlaceholderText('00.000.000/0000-00')).toBeDefined();
  });
});
