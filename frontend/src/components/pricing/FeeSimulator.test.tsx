import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FeeSimulator } from './FeeSimulator';
import { FeeResolutionResult } from '@/types/pricing';

describe('FeeSimulator Component (P014-FE)', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  const mockSuccessResult: FeeResolutionResult = {
    marketplace: 'MERCADO_LIVRE',
    referenceDate: '2026-11-19T10:00:00Z',
    status: 'SUCCESS',
    totalEstimatedFees: 25.2,
    hasMissingFees: false,
    missingFeeTypes: [],
    resolvedFees: [
      {
        feeType: 'COMMISSION',
        ruleId: 'rule-com-1',
        name: 'Comissão Oficial Mercado Livre Gold Pro',
        sourceType: 'OFFICIAL_PORTAL',
        sourceReference: 'https://www.mercadolivre.com.br/ajuda/custos-vender-produtos_1338',
        calculatedAmount: 19.2,
        appliedPercentage: 16.0,
        appliedFixedAmount: null,
      },
      {
        feeType: 'FIXED_FEE',
        ruleId: 'rule-fix-1',
        name: 'Tarifa Fixa Mercado Livre',
        sourceType: 'OFFICIAL_PORTAL',
        sourceReference: 'https://www.mercadolivre.com.br/ajuda/custos-vender-produtos_1338',
        calculatedAmount: 6.0,
        appliedPercentage: null,
        appliedFixedAmount: 6.0,
      },
    ],
    message: null,
  };

  const mockMissingFeesResult: FeeResolutionResult = {
    marketplace: 'SHOPEE',
    referenceDate: '2026-11-19T10:00:00Z',
    status: 'MISSING',
    totalEstimatedFees: 0.0,
    hasMissingFees: true,
    missingFeeTypes: ['COMMISSION'],
    resolvedFees: [],
    message: 'Nenhuma regra contratual cadastrada para o marketplace SHOPEE na data informada.',
  };

  const mockManualRuleResult: FeeResolutionResult = {
    marketplace: 'MERCADO_LIVRE',
    referenceDate: '2026-11-19T10:00:00Z',
    status: 'SUCCESS',
    totalEstimatedFees: 18.0,
    hasMissingFees: false,
    missingFeeTypes: [],
    resolvedFees: [
      {
        feeType: 'COMMISSION',
        ruleId: 'rule-manual-1',
        name: 'Estimativa Manual Provisória',
        sourceType: 'MANUAL_OVERRIDE',
        sourceReference: 'Anotação verbal',
        calculatedAmount: 18.0,
        appliedPercentage: 15.0,
        appliedFixedAmount: null,
      },
    ],
    message: null,
  };

  it('deve renderizar os controles do simulador de taxas', () => {
    render(<FeeSimulator />);

    expect(screen.getByRole('heading', { name: /Simulador Determinístico de Taxas e Comissões/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Marketplace/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Preço de Venda do Anúncio/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Data de Referência da Venda/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Simular e Resolver Taxas/i })).toBeInTheDocument();
  });

  it('deve simular resolução com sucesso e exibir composição de taxas e receita líquida', async () => {
    vi.spyOn(global, 'fetch').mockImplementation(async () => {
      return {
        ok: true,
        json: async () => mockSuccessResult,
      } as any;
    });

    render(<FeeSimulator />);

    const priceInput = screen.getByLabelText(/Preço de Venda do Anúncio/i);
    fireEvent.change(priceInput, { target: { value: '120.00' } });

    const submitBtn = screen.getByRole('button', { name: /Simular e Resolver Taxas/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/Demonstrativo de Taxas Calculadas • MERCADO_LIVRE/i)).toBeInTheDocument();
    });

    expect(screen.getByText('- R$ 25.20')).toBeInTheDocument();
    expect(screen.getByText('R$ 94.80')).toBeInTheDocument(); // Receita líquida: 120 - 25.20 = 94.80
    expect(screen.getByText(/Comissão Oficial Mercado Livre Gold Pro/i)).toBeInTheDocument();
    expect(screen.getByText('16.00%')).toBeInTheDocument();
  });

  it('REGRA ANTI-ALUCINAÇÃO: deve alertar enfaticamente quando taxas estiverem ausentes e recusar assumir 0%', async () => {
    vi.spyOn(global, 'fetch').mockImplementation(async () => {
      return {
        ok: true,
        json: async () => mockMissingFeesResult,
      } as any;
    });

    render(<FeeSimulator />);

    const mktSelect = screen.getByLabelText(/Marketplace/i);
    fireEvent.change(mktSelect, { target: { value: 'SHOPEE' } });

    const submitBtn = screen.getByRole('button', { name: /Simular e Resolver Taxas/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/Regra Anti-Alucinação: Taxas Obrigatórias Ausentes/i)).toBeInTheDocument();
    });

    // Confirma mensagem de anti-alucinação
    expect(screen.getByText(/NUNCA assume 0%/i)).toBeInTheDocument();
    expect(screen.getByText(/COMMISSION/i)).toBeInTheDocument();
  });

  it('REGRA ANTI-ALUCINAÇÃO: deve alertar ostensivamente quando o cálculo utilizar regra manual não oficial', async () => {
    vi.spyOn(global, 'fetch').mockImplementation(async () => {
      return {
        ok: true,
        json: async () => mockManualRuleResult,
      } as any;
    });

    render(<FeeSimulator />);

    const submitBtn = screen.getByRole('button', { name: /Simular e Resolver Taxas/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/O cálculo utilizou uma ou mais regras de taxas classificadas como/i)).toBeInTheDocument();
      expect(screen.getByText(/Entrada Manual \(Não Oficial\)/i)).toBeInTheDocument();
    });
  });

  it('deve validar preço de venda numérico positivo', () => {
    render(<FeeSimulator />);

    const priceInput = screen.getByLabelText(/Preço de Venda do Anúncio/i);
    fireEvent.change(priceInput, { target: { value: '-5.00' } });

    const form = screen.getByRole('button', { name: /Simular e Resolver Taxas/i }).closest('form')!;
    fireEvent.submit(form);

    expect(screen.getByText(/Informe um preço de venda válido maior que zero/i)).toBeInTheDocument();
  });
});
