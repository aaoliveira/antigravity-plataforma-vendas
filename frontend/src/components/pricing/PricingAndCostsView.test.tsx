import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { PricingAndCostsView } from './PricingAndCostsView';

describe('PricingAndCostsView Component (P014-FE)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(global, 'fetch').mockImplementation(async () => {
      return { ok: true, json: async () => [] } as any;
    });
  });

  it('deve renderizar o cabeçalho do módulo com título, badges e abas de navegação', () => {
    render(<PricingAndCostsView />);

    expect(screen.getByRole('heading', { name: /Gestão de Custos, Taxas e Simulação Financeira/i })).toBeInTheDocument();
    expect(screen.getByText(/Precisão BigDecimal/i)).toBeInTheDocument();
    expect(screen.getByText(/Anti-Alucinação Ativa/i)).toBeInTheDocument();

    expect(screen.getByRole('tab', { name: /Custos por Produto/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Regras de Taxas & Tarifas/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Simulador de Resolução/i })).toBeInTheDocument();
  });

  it('deve alternar entre as abas e renderizar o tabpanel correspondente', () => {
    render(<PricingAndCostsView />);

    // Aba inicial: Custos
    expect(screen.getByRole('tab', { name: /Custos por Produto/i })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel', { name: /Custos por Produto/i })).toBeInTheDocument();
    expect(screen.getByText(/Cadastro e Versionamento de Custos/i)).toBeInTheDocument();

    // Alterna para Regras de Taxas
    const feesTab = screen.getByRole('tab', { name: /Regras de Taxas & Tarifas/i });
    fireEvent.click(feesTab);

    expect(feesTab).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel', { name: /Regras de Taxas & Tarifas/i })).toBeInTheDocument();
    expect(screen.getByText(/Cadastro de Regras de Tarifas e Comissões/i)).toBeInTheDocument();

    // Alterna para Simulador
    const simTab = screen.getByRole('tab', { name: /Simulador de Resolução/i });
    fireEvent.click(simTab);

    expect(simTab).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel', { name: /Simulador de Resolução/i })).toBeInTheDocument();
    expect(screen.getByText(/Simulador Determinístico de Taxas e Comissões/i)).toBeInTheDocument();
  });
});
