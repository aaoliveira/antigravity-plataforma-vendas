import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FeeRuleForm } from './FeeRuleForm';
import { FeeRule } from '@/types/pricing';

describe('FeeRuleForm Component (P014-FE)', () => {
  const mockRules: FeeRule[] = [
    {
      id: 'rule-meli-1',
      tenantId: 'tenant-123',
      marketplace: 'MERCADO_LIVRE',
      feeType: 'COMMISSION',
      feeValueType: 'PERCENTAGE',
      percentageValue: 16.5,
      fixedAmount: null,
      minPriceThreshold: null,
      maxPriceThreshold: null,
      listingType: 'gold_pro',
      category: 'Informática',
      effectiveFrom: '2026-01-01T00:00:00Z',
      effectiveTo: null,
      version: 1,
      isActive: true,
      sourceType: 'OFFICIAL_PORTAL',
      sourceReference: 'https://www.mercadolivre.com.br/ajuda/custos-vender-produtos_1338',
      notes: 'Tabela oficial 2026',
      createdByName: 'Admin',
      createdAt: '2026-01-01T00:00:00Z',
    },
    {
      id: 'rule-shopee-manual',
      tenantId: 'tenant-123',
      marketplace: 'SHOPEE',
      feeType: 'COMMISSION',
      feeValueType: 'PERCENTAGE',
      percentageValue: 14.0,
      fixedAmount: null,
      minPriceThreshold: null,
      maxPriceThreshold: null,
      listingType: 'standard',
      category: null,
      effectiveFrom: '2026-02-01T00:00:00Z',
      effectiveTo: null,
      version: 1,
      isActive: true,
      sourceType: 'MANUAL_OVERRIDE',
      sourceReference: 'Estimativa verbal do gerente comercial',
      notes: 'Aguardando publicação do contrato',
      createdByName: 'Vendedor',
      createdAt: '2026-02-01T00:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('deve renderizar o formulário e a tabela de regras de taxas', async () => {
    vi.spyOn(global, 'fetch').mockImplementation(async () => {
      return {
        ok: true,
        json: async () => mockRules,
      } as any;
    });

    render(<FeeRuleForm />);

    expect(screen.getByRole('heading', { name: /Cadastro de Regras de Tarifas e Comissões/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Marketplace/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Tipo de Cobrança/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Tipo de Fonte da Regra/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('MERCADO_LIVRE')).toBeInTheDocument();
      expect(screen.getByText('SHOPEE')).toBeInTheDocument();
    });
  });

  it('REGRA ANTI-ALUCINAÇÃO: deve exibir alerta ostensivo ao selecionar Entrada Manual (Não Oficial)', () => {
    vi.spyOn(global, 'fetch').mockImplementation(async () => {
      return { ok: true, json: async () => [] } as any;
    });

    render(<FeeRuleForm />);

    const sourceTypeSelect = screen.getByLabelText(/Tipo de Fonte da Regra/i);
    fireEvent.change(sourceTypeSelect, { target: { value: 'MANUAL_OVERRIDE' } });

    // Deve exibir o banner de alerta anti-alucinação
    expect(screen.getByText(/Regra Manual Não Verificada:/i)).toBeInTheDocument();
    expect(screen.getByText(/Ela nunca será exibida como taxa oficial do marketplace/i)).toBeInTheDocument();
  });

  it('REGRA ANTI-ALUCINAÇÃO: tabela deve diferenciar estritamente regras oficiais vs manuais não verificadas', async () => {
    vi.spyOn(global, 'fetch').mockImplementation(async () => {
      return {
        ok: true,
        json: async () => mockRules,
      } as any;
    });

    render(<FeeRuleForm />);

    await waitFor(() => {
      // Regra oficial do Mercado Livre deve exibir badge oficial
      expect(screen.getByText(/🛡️ Portal Oficial/i)).toBeInTheDocument();

      // Regra manual da Shopee NUNCA pode ser apresentada como oficial e deve exibir aviso ostensivo
      expect(screen.getByText(/⚠️ Manual \(Não Oficial\)/i)).toBeInTheDocument();
    });
  });

  it('deve validar obrigatoriedade de sourceReference (fonte oficial/contratual)', async () => {
    vi.spyOn(global, 'fetch').mockImplementation(async () => {
      return { ok: true, json: async () => [] } as any;
    });

    render(<FeeRuleForm />);

    const refInput = screen.getByLabelText(/Referência da Fonte/i);
    fireEvent.change(refInput, { target: { value: '' } });

    const submitBtn = screen.getByRole('button', { name: /Cadastrar Regra de Taxa/i });
    fireEvent.click(submitBtn);

    expect(screen.getByText(/A referência da fonte oficial\/contratual é obrigatória/i)).toBeInTheDocument();
  });

  it('deve validar coerência de faixas de preço (mínimo <= máximo)', async () => {
    vi.spyOn(global, 'fetch').mockImplementation(async () => {
      return { ok: true, json: async () => [] } as any;
    });

    render(<FeeRuleForm />);

    const minPriceInput = screen.getByLabelText(/Preço Mínimo da Faixa/i);
    fireEvent.change(minPriceInput, { target: { value: '150.00' } });

    const maxPriceInput = screen.getByLabelText(/Preço Máximo da Faixa/i);
    fireEvent.change(maxPriceInput, { target: { value: '100.00' } });

    const submitBtn = screen.getByRole('button', { name: /Cadastrar Regra de Taxa/i });
    fireEvent.click(submitBtn);

    expect(screen.getByText(/O preço mínimo da faixa não pode ser maior que o preço máximo/i)).toBeInTheDocument();
  });

  it('deve cadastrar regra com sucesso e exibir feedback', async () => {
    const onRuleCreated = vi.fn();

    vi.spyOn(global, 'fetch').mockImplementation(async (url: any, opts: any) => {
      if (opts?.method === 'POST') {
        return {
          ok: true,
          json: async () => ({
            id: 'rule-created-1',
            marketplace: 'MERCADO_LIVRE',
            version: 2,
            feeType: 'COMMISSION',
          }),
        } as any;
      }
      return { ok: true, json: async () => [] } as any;
    });

    render(<FeeRuleForm onRuleCreated={onRuleCreated} />);

    const submitBtn = screen.getByRole('button', { name: /Cadastrar Regra de Taxa/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/Regra de taxa para MERCADO_LIVRE cadastrada com sucesso \(v2\)!/i);
    });

    expect(onRuleCreated).toHaveBeenCalledWith(expect.objectContaining({ marketplace: 'MERCADO_LIVRE', version: 2 }));
  });
});
