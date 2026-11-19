import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ProductCostForm } from './ProductCostForm';
import { ProductCost } from '@/types/pricing';

describe('ProductCostForm Component (P014-FE)', () => {
  const mockCostHistory: ProductCost[] = [
    {
      id: 'cost-1',
      tenantId: 'tenant-123',
      productId: '550e8400-e29b-41d4-a716-446655440000',
      variantId: null,
      companyId: null,
      currency: 'BRL',
      unit: 'UN',
      acquisitionCost: 45.0,
      packagingCost: 3.5,
      additionalCost: 1.5,
      subsidizedShippingCost: 0.0,
      taxRatePercent: 8.5,
      otherExpensesPercent: 2.0,
      effectiveFrom: '2026-11-01T00:00:00Z',
      effectiveTo: '2026-11-15T00:00:00Z',
      version: 1,
      sourceType: 'USER_INPUT',
      notes: 'Lote inicial',
      createdByName: 'Admin',
      createdAt: '2026-11-01T00:00:00Z',
    },
    {
      id: 'cost-2',
      tenantId: 'tenant-123',
      productId: '550e8400-e29b-41d4-a716-446655440000',
      variantId: null,
      companyId: null,
      currency: 'BRL',
      unit: 'UN',
      acquisitionCost: 50.0,
      packagingCost: 4.0,
      additionalCost: 2.0,
      subsidizedShippingCost: 0.0,
      taxRatePercent: 8.5,
      otherExpensesPercent: 2.0,
      effectiveFrom: '2026-11-15T00:00:00Z',
      effectiveTo: null,
      version: 2,
      sourceType: 'ERP_SYNC',
      notes: 'Reajuste de matéria-prima',
      createdByName: 'Financeiro',
      createdAt: '2026-11-15T00:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('deve renderizar o formulário com campos obrigatórios e acessíveis', () => {
    render(<ProductCostForm />);

    expect(screen.getByRole('heading', { name: /Cadastro e Versionamento de Custos/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/ID do Produto \(UUID\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Custo de Aquisição/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Vigência Inicial/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Registrar Versão de Custo/i })).toBeInTheDocument();
  });

  it('deve validar campos obrigatórios e formato de UUID ao tentar submeter formulário vazio', async () => {
    render(<ProductCostForm />);

    const submitBtn = screen.getByRole('button', { name: /Registrar Versão de Custo/i });
    fireEvent.click(submitBtn);

    expect(screen.getByText(/O ID do produto é obrigatório/i)).toBeInTheDocument();
    expect(screen.getByText(/Custo de aquisição é obrigatório e deve ser numérico/i)).toBeInTheDocument();
  });

  it('deve validar limites numéricos (aquisição > 0, alíquotas 0-100%)', async () => {
    render(<ProductCostForm />);

    const prodInput = screen.getByLabelText(/ID do Produto \(UUID\)/i);
    fireEvent.change(prodInput, { target: { value: '550e8400-e29b-41d4-a716-446655440000' } });

    const acqInput = screen.getByLabelText(/Custo de Aquisição/i);
    fireEvent.change(acqInput, { target: { value: '-10' } });

    const taxInput = screen.getByLabelText(/Imposto Estimado \(%\)/i);
    fireEvent.change(taxInput, { target: { value: '150' } });

    const submitBtn = screen.getByRole('button', { name: /Registrar Versão de Custo/i });
    fireEvent.click(submitBtn);

    expect(screen.getByText(/Custo de aquisição deve ser maior que zero/i)).toBeInTheDocument();
    expect(screen.getByText(/Alíquota de imposto deve estar entre 0% e 100%/i)).toBeInTheDocument();
  });

  it('deve carregar e exibir histórico de versões com vigência e status vigente vs encerrado', async () => {
    vi.spyOn(global, 'fetch').mockImplementation(async (url: any) => {
      if (url.toString().includes('/api/pricing/costs/history')) {
        return {
          ok: true,
          json: async () => mockCostHistory,
        } as any;
      }
      return { ok: false, json: async () => ({}) } as any;
    });

    render(<ProductCostForm initialProductId="550e8400-e29b-41d4-a716-446655440000" />);

    await waitFor(() => {
      expect(screen.getByText('2 versão(ões)')).toBeInTheDocument();
    });

    // Versão 1 (Encerrada)
    expect(screen.getByText('v1')).toBeInTheDocument();
    expect(screen.getByText('Encerrado')).toBeInTheDocument();
    expect(screen.getByText('BRL 45.00')).toBeInTheDocument();

    // Versão 2 (Vigente)
    expect(screen.getByText('v2')).toBeInTheDocument();
    expect(screen.getByText('Vigente')).toBeInTheDocument();
    expect(screen.getByText('BRL 50.00')).toBeInTheDocument();
  });

  it('deve registrar nova versão de custo com sucesso e notificar callback', async () => {
    const onCostSaved = vi.fn();

    vi.spyOn(global, 'fetch').mockImplementation(async (url: any, opts: any) => {
      if (url.toString().includes('/api/pricing/costs/history')) {
        return { ok: true, json: async () => [] } as any;
      }
      if (opts?.method === 'POST') {
        return {
          ok: true,
          json: async () => ({
            id: 'cost-new',
            tenantId: 'tenant-123',
            productId: '550e8400-e29b-41d4-a716-446655440000',
            version: 3,
            acquisitionCost: 55.0,
            currency: 'BRL',
          }),
        } as any;
      }
      return { ok: false, json: async () => ({}) } as any;
    });

    render(<ProductCostForm initialProductId="550e8400-e29b-41d4-a716-446655440000" onCostSaved={onCostSaved} />);

    const acqInput = screen.getByLabelText(/Custo de Aquisição/i);
    fireEvent.change(acqInput, { target: { value: '55.00' } });

    const submitBtn = screen.getByRole('button', { name: /Registrar Versão de Custo/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/Custo registrado com sucesso! Versão 3 já vigente/i);
    });

    expect(onCostSaved).toHaveBeenCalledWith(expect.objectContaining({ version: 3 }));
  });
});
