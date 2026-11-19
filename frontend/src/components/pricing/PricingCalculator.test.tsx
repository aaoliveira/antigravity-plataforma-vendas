import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { PricingCalculator } from './PricingCalculator';
import { PricingCalculationResult } from '@/types/pricing';

const mockCalculationSuccess: PricingCalculationResult = {
  grossSellingPrice: 100.0,
  promotionalDiscount: 0.0,
  netSellingPrice: 100.0,
  totalDirectCosts: 30.0,
  totalVariableDeductions: 25.0,
  totalFixedDeductions: 10.0,
  totalDeductions: 65.0,
  contributionMarginAmount: 35.0,
  contributionMarginPercent: 35.0,
  breakEvenPrice: 53.33,
  isBreakEvenAttainable: true,
  minPrice: 60.0,
  isMinPriceAttainable: true,
  targetPrice: 80.0,
  isTargetPriceAttainable: true,
  currency: 'BRL',
  formulaVersion: 'v1.0.0',
  calculatedAt: '2026-11-19T10:00:00Z',
  deductionItems: [
    {
      name: 'Custo de Aquisição',
      category: 'DIRECT_COST',
      amount: 30.0,
      percentageOfNetRevenue: 30.0,
      appliedRate: 30.0,
      isPercentage: false,
    },
    {
      name: 'Comissão do Canal',
      category: 'COMMISSION',
      amount: 15.0,
      percentageOfNetRevenue: 15.0,
      appliedRate: 15.0,
      isPercentage: true,
    },
    {
      name: 'Impostos Faturados',
      category: 'TAX',
      amount: 10.0,
      percentageOfNetRevenue: 10.0,
      appliedRate: 10.0,
      isPercentage: true,
    },
    {
      name: 'Tarifa Fixa do Canal',
      category: 'FIXED_FEE',
      amount: 10.0,
      percentageOfNetRevenue: 10.0,
      appliedRate: 10.0,
      isPercentage: false,
    },
  ],
  assumptions: ['Cálculo com parâmetros nominais'],
  warnings: [],
};

describe('PricingCalculator Component (P015-FE)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('1. Deve renderizar o cabeçalho, aviso anti-alucinação sobre lucro líquido e alternância de modos', () => {
    render(<PricingCalculator />);

    // Disclaimer Anti-Alucinação
    expect(
      screen.getByText(/Margem de Contribuição NÃO é Lucro Líquido/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/despesas fixas da empresa e gerar lucro/i)
    ).toBeInTheDocument();

    // Título e Modos
    expect(
      screen.getByText(/Calculadora Interativa de Precificação & Rentabilidade/i)
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Entrada Manual/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Por Produto Cadastrado/i })).toBeInTheDocument();

    // Inputs essenciais do modo manual
    expect(screen.getByLabelText(/Preço de Venda Bruto/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Custo de Aquisição/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Comissão do Marketplace/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Alíquota de Imposto/i)).toBeInTheDocument();
  });

  it('2. Deve validar campos obrigatórios e rejeitar valores negativos ou inconsistentes', async () => {
    render(<PricingCalculator />);

    const grossInput = screen.getByLabelText(/Preço de Venda Bruto/i);
    const discountInput = screen.getByLabelText(/Desconto Promocional/i);
    const submitBtn = screen.getByRole('button', { name: /Calcular Preço/i });

    // Preço zerado
    fireEvent.change(grossInput, { target: { value: '0' } });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText(/Preço de venda deve ser um número maior que zero/i)
    ).toBeInTheDocument();

    // Desconto maior que o preço de venda
    fireEvent.change(grossInput, { target: { value: '50.00' } });
    fireEvent.change(discountInput, { target: { value: '60.00' } });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText(/Desconto não pode ser maior ou igual ao preço de venda/i)
    ).toBeInTheDocument();
  });

  it('3. Deve validar formato de UUID no modo Por Produto Cadastrado', async () => {
    render(<PricingCalculator />);

    // Alternar para modo produto
    fireEvent.click(screen.getByRole('button', { name: /Por Produto Cadastrado/i }));

    expect(screen.getByLabelText(/UUID do Produto Mestre/i)).toBeInTheDocument();

    const submitBtn = screen.getByRole('button', { name: /Calcular Preço/i });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText(/Identificador do produto \(UUID\) é obrigatório/i)
    ).toBeInTheDocument();

    // Formato inválido
    const productInput = screen.getByLabelText(/UUID do Produto Mestre/i);
    fireEvent.change(productInput, { target: { value: 'invalid-uuid-123' } });
    fireEvent.click(submitBtn);

    expect(
      await screen.findByText(/Formato de UUID inválido/i)
    ).toBeInTheDocument();
  });

  it('4. Deve executar cálculo manual com sucesso, renderizar métricas, cenários e deduções detalhadas', async () => {
    vi.spyOn(global, 'fetch').mockImplementationOnce(async () => ({
      ok: true,
      json: async () => mockCalculationSuccess,
    }) as any);

    render(<PricingCalculator />);

    const submitBtn = screen.getByRole('button', { name: /Calcular Preço/i });
    fireEvent.click(submitBtn);

    // Aguarda exibição do resultado
    await waitFor(() => {
      expect(screen.getByText(/Receita Líquida Unitária/i)).toBeInTheDocument();
    });

    // Validação dos cards
    expect(screen.getAllByText('R$ 35,00').length).toBeGreaterThanOrEqual(1); // Margem de Contribuição
    expect(screen.getByText(/35,00% da Receita Líquida/i)).toBeInTheDocument();
    expect(screen.getAllByText('R$ 53,33').length).toBeGreaterThanOrEqual(1); // Break-Even

    // Tabela de Cenários Comparativos
    expect(screen.getByText(/Simulação Comparativa de Cenários/i)).toBeInTheDocument();
    expect(screen.getByText('Cenário Atual')).toBeInTheDocument();
    expect(screen.getByText(/Ponto de Equilíbrio \(Zero a Zero\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Meta de Rentabilidade/i)).toBeInTheDocument();

    // Composição Detalhada das Deduções
    expect(screen.getByText(/Composição Detalhada das Deduções Unitárias/i)).toBeInTheDocument();
    expect(screen.getByText('Custo de Aquisição')).toBeInTheDocument();
    expect(screen.getByText('Comissão do Canal')).toBeInTheDocument();
    expect(screen.getByText('Impostos Faturados')).toBeInTheDocument();
    expect(screen.getByText('Tarifa Fixa do Canal')).toBeInTheDocument();
    expect(screen.getByText('-R$ 30,00')).toBeInTheDocument();
  });

  it('5. Deve exibir alerta ostensivo quando a API retornar avisos de taxas manuais ou desatualizadas', async () => {
    const mockWithWarnings: PricingCalculationResult = {
      ...mockCalculationSuccess,
      warnings: [
        'Atenção: A regra de comissão aplicada (15%) foi configurada manualmente pelo usuário e não possui validação contratual oficial.',
      ],
    };

    vi.spyOn(global, 'fetch').mockImplementationOnce(async () => ({
      ok: true,
      json: async () => mockWithWarnings,
    }) as any);

    render(<PricingCalculator />);

    fireEvent.click(screen.getByRole('button', { name: /Calcular Preço/i }));

    expect(
      await screen.findByText(/Alertas de Conformidade e Fontes de Dados:/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/A regra de comissão aplicada \(15%\) foi configurada manualmente/i)
    ).toBeInTheDocument();
  });

  it('6. Deve tratar adequadamente impossibilidade matemática de break-even (taxas >= 100%)', async () => {
    const mockUnattainableBreakEven: PricingCalculationResult = {
      ...mockCalculationSuccess,
      breakEvenPrice: null,
      isBreakEvenAttainable: false,
      warnings: [
        'Aviso: O preço de equilíbrio é matematicamente inatingível pois a soma das taxas variáveis é maior ou igual a 100%.',
      ],
    };

    vi.spyOn(global, 'fetch').mockImplementationOnce(async () => ({
      ok: true,
      json: async () => mockUnattainableBreakEven,
    }) as any);

    render(<PricingCalculator />);

    fireEvent.click(screen.getByRole('button', { name: /Calcular Preço/i }));

    expect(
      await screen.findByText('Inatingível (Taxas >= 100%)')
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Deduções variáveis inviabilizam o equilíbrio/i)
    ).toBeInTheDocument();
  });

  it('7. Deve exibir mensagem de erro amigável quando o backend falhar', async () => {
    vi.spyOn(global, 'fetch').mockImplementationOnce(async () => ({
      ok: false,
      json: async () => ({ message: 'Produto não encontrado no tenant ativo.' }),
    }) as any);

    render(<PricingCalculator />);

    fireEvent.click(screen.getByRole('button', { name: /Calcular Preço/i }));

    expect(
      await screen.findByText(/Produto não encontrado no tenant ativo/i)
    ).toBeInTheDocument();
  });
});
