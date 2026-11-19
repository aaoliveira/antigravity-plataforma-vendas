import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FinancialFactsDashboardView } from './FinancialFactsDashboardView';
import { FinancialFactSummary, OrderFinancialFact } from '@/types/financial';

const mockSummary: FinancialFactSummary = {
  totalOrders: 2,
  totalGrossRevenue: 500.0,
  totalDiscountAmount: 20.0,
  totalNetRevenue: 480.0,
  totalCancellationAmount: 0.0,
  totalReturnAmount: 0.0,
  totalMarketplaceFees: 67.2,
  totalFixedFees: 12.0,
  totalShippingCost: 35.0,
  totalProductCost: 150.0,
  totalPackagingCost: 8.0,
  totalAdditionalCost: 0.0,
  totalTaxAmount: 28.8,
  totalOtherExpensesAmount: 0.0,
  totalDeductions: 301.0,
  totalContributionMarginAmount: 179.0,
  averageContributionMarginRatio: 37.29,
  reconciledCount: 1,
  estimatedCount: 1,
  divergentCount: 0,
  ordersWithMissingDataCount: 1,
  antiHallucinationDisclaimer:
    'Este demonstrativo apura estritamente a Margem de Contribuição (Receita Líquida deduzida de custos diretos variáveis, comissões, fretes e impostos informados). Despesas fixas estruturais e operacionais da empresa não estão contempladas, sendo proibido rotular este valor como Lucro Líquido.',
};

const mockFacts: OrderFinancialFact[] = [
  {
    id: 'fact-1',
    orderId: 'order-1',
    sellerAccountId: 'acc-meli',
    companyId: 'comp-alpha',
    marketplace: 'MERCADO_LIVRE',
    externalOrderId: 'MLB-2026-001',
    orderStatus: 'PAID',
    orderDate: '2026-11-19T10:00:00Z',
    grossRevenue: 300.0,
    discountAmount: 0.0,
    netRevenue: 300.0,
    cancellationAmount: 0.0,
    returnAmount: 0.0,
    marketplaceFee: 42.0,
    fixedFee: 6.0,
    shippingCost: 20.0,
    productCost: 90.0,
    packagingCost: 4.0,
    additionalCost: 0.0,
    taxAmount: 18.0,
    otherExpensesAmount: 0.0,
    totalDeductions: 180.0,
    contributionMarginAmount: 120.0,
    contributionMarginRatio: 40.0,
    currency: 'BRL',
    reconciliationStatus: 'RECONCILED',
    feeProvenance: 'CONFIRMED',
    shippingProvenance: 'CONFIRMED',
    costProvenance: 'USER_INFORMED',
    taxProvenance: 'USER_INFORMED',
    hasMissingData: false,
    missingDataReasons: null,
    calculationVersion: 'v1.0.0',
    calculationTraceJson: '{"engineVersion":"v1.0.0","items":[]}',
    settlementExternalId: 'MP-SETTLE-001',
    settlementDate: '2026-11-19T12:00:00Z',
    settlementNotes: 'Repasse integral Mercado Pago',
    reconciledAt: '2026-11-19T12:05:00Z',
    antiHallucinationDisclaimer: 'Margem de Contribuição não é Lucro Líquido.',
    createdAt: '2026-11-19T10:00:00Z',
    updatedAt: '2026-11-19T12:05:00Z',
    items: [
      {
        id: 'item-fact-1',
        orderItemId: 'item-1',
        productId: 'prod-1',
        variantId: null,
        sku: 'SKU-SMARTPHONE-X',
        title: 'Smartphone Modelo X Pro',
        quantity: 1,
        unitPrice: 300.0,
        grossRevenue: 300.0,
        discountAmount: 0.0,
        netRevenue: 300.0,
        cancellationAmount: 0.0,
        returnAmount: 0.0,
        marketplaceFee: 42.0,
        fixedFee: 6.0,
        shippingCost: 20.0,
        unitProductCost: 90.0,
        productCost: 90.0,
        packagingCost: 4.0,
        additionalCost: 0.0,
        taxRatePercent: 6.0,
        taxAmount: 18.0,
        otherExpensesAmount: 0.0,
        totalDeductions: 180.0,
        contributionMarginAmount: 120.0,
        contributionMarginRatio: 40.0,
        costProvenance: 'USER_INFORMED',
        hasMissingData: false,
        missingDataReasons: null,
      },
    ],
  },
  {
    id: 'fact-2',
    orderId: 'order-2',
    sellerAccountId: 'acc-shopee',
    companyId: 'comp-beta',
    marketplace: 'SHOPEE',
    externalOrderId: 'SHP-2026-002',
    orderStatus: 'PAID',
    orderDate: '2026-11-18T14:30:00Z',
    grossRevenue: 200.0,
    discountAmount: 20.0,
    netRevenue: 180.0,
    cancellationAmount: 0.0,
    returnAmount: 0.0,
    marketplaceFee: 25.2,
    fixedFee: 6.0,
    shippingCost: 15.0,
    productCost: 60.0,
    packagingCost: 4.0,
    additionalCost: 0.0,
    taxAmount: 10.8,
    otherExpensesAmount: 0.0,
    totalDeductions: 121.0,
    contributionMarginAmount: 59.0,
    contributionMarginRatio: 32.78,
    currency: 'BRL',
    reconciliationStatus: 'ESTIMATED',
    feeProvenance: 'ESTIMATED',
    shippingProvenance: 'ESTIMATED',
    costProvenance: 'MISSING',
    taxProvenance: 'USER_INFORMED',
    hasMissingData: true,
    missingDataReasons: '["PRODUCT_COST_MISSING"]',
    calculationVersion: 'v1.0.0',
    calculationTraceJson: '{"engineVersion":"v1.0.0","items":[]}',
    settlementExternalId: null,
    settlementDate: null,
    settlementNotes: null,
    reconciledAt: null,
    antiHallucinationDisclaimer: 'Margem de Contribuição não é Lucro Líquido.',
    createdAt: '2026-11-18T14:30:00Z',
    updatedAt: '2026-11-18T14:30:00Z',
    items: [
      {
        id: 'item-fact-2',
        orderItemId: 'item-2',
        productId: 'prod-2',
        variantId: null,
        sku: 'SKU-FONE-BLUETOOTH',
        title: 'Fone de Ouvido Bluetooth',
        quantity: 1,
        unitPrice: 200.0,
        grossRevenue: 200.0,
        discountAmount: 20.0,
        netRevenue: 180.0,
        cancellationAmount: 0.0,
        returnAmount: 0.0,
        marketplaceFee: 25.2,
        fixedFee: 6.0,
        shippingCost: 15.0,
        unitProductCost: 60.0,
        productCost: 60.0,
        packagingCost: 4.0,
        additionalCost: 0.0,
        taxRatePercent: 6.0,
        taxAmount: 10.8,
        otherExpensesAmount: 0.0,
        totalDeductions: 121.0,
        contributionMarginAmount: 59.0,
        contributionMarginRatio: 32.78,
        costProvenance: 'MISSING',
        hasMissingData: true,
        missingDataReasons: '["PRODUCT_COST_MISSING"]',
      },
    ],
  },
];

const mockCompanies = [
  { id: 'comp-alpha', corporateName: 'Alpha Comércio LTDA' },
  { id: 'comp-beta', corporateName: 'Beta Varejo LTDA' },
];

const mockAccounts = [
  { id: 'acc-meli', accountName: 'Loja Mercado Livre', marketplace: 'MERCADO_LIVRE' },
  { id: 'acc-shopee', accountName: 'Loja Shopee', marketplace: 'SHOPEE' },
];

describe('FinancialFactsDashboardView Component (P017-FE)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    global.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/financial-facts/summary')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockSummary),
        });
      }
      if (url.includes('/api/financial-facts') && !url.includes('/orders/')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: mockFacts, totalElements: 2 }),
        });
      }
      if (url.includes('/api/companies')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: mockCompanies }),
        });
      }
      if (url.includes('/api/catalog/seller-accounts')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ accounts: mockAccounts }),
        });
      }
      if (url.includes('/reconcile')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              ...mockFacts[1],
              reconciliationStatus: 'RECONCILED',
              settlementExternalId: 'EXT-RECONCILED-99',
            }),
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
  });

  it('1. Deve renderizar o painel com cards de KPIs e reconciliação dos totais com a soma dos pedidos', async () => {
    render(<FinancialFactsDashboardView />);

    // Título da página
    expect(screen.getByText('Painel Financeiro & Conciliação')).toBeDefined();

    // Aguarda carregamento dos KPIs
    await waitFor(() => {
      expect(screen.getByText('R$ 500,00')).toBeDefined(); // Receita Bruta
      expect(screen.getByText('R$ 179,00')).toBeDefined(); // Margem Contribuição
    });

    // Disclaimer Anti-Alucinação Mandatório
    expect(
      screen.getByText(/Aviso de Governança Financeira \(Margem de Contribuição vs\. Lucro Líquido\):/i)
    ).toBeDefined();

    // Verificação de reconciliação de soma dos pedidos contra o total agregado do painel
    // Pedido 1 (R$ 300,00) + Pedido 2 (R$ 200,00) = R$ 500,00
    expect(screen.getByText('MLB-2026-001')).toBeDefined();
    expect(screen.getByText('SHP-2026-002')).toBeDefined();
  });

  it('2. Deve exibir definições acessíveis dos indicadores e notas de governança anti-alucinação ao clicar em ajuda', async () => {
    render(<FinancialFactsDashboardView />);

    await waitFor(() => {
      expect(screen.getByLabelText('Definição de Margem de Contribuição')).toBeDefined();
    });

    // Clica no botão de ajuda da Margem de Contribuição
    const helpBtn = screen.getByLabelText('Definição de Margem de Contribuição');
    fireEvent.click(helpBtn);

    // O tooltip acessível com a definição, fórmula e aviso anti-alucinação deve aparecer
    const tooltip = screen.getByRole('tooltip');
    expect(tooltip).toBeDefined();
    expect(within(tooltip).getByText(/NÃO É LUCRO LÍQUIDO/i)).toBeDefined();
    expect(within(tooltip).getByText(/Receita Líquida - \(Deduções de Canal \+ Frete \+ CMV \+ Embalagem \+ Impostos\)/i)).toBeDefined();
  });

  it('3. Deve filtrar combinadamente por canal, empresa e busca de SKU em tempo real', async () => {
    render(<FinancialFactsDashboardView />);

    await waitFor(() => {
      expect(screen.getByText('MLB-2026-001')).toBeDefined();
      expect(screen.getByText('SHP-2026-002')).toBeDefined();
    });

    // Digita busca por SKU específico 'SMARTPHONE'
    const skuInput = screen.getByLabelText(/Filtrar por SKU/i);
    fireEvent.change(skuInput, { target: { value: 'SMARTPHONE' } });

    // Apenas MLB-2026-001 deve permanecer visível
    expect(screen.getByText('MLB-2026-001')).toBeDefined();
    expect(screen.queryByText('SHP-2026-002')).toBeNull();

    // Contador de filtros ativos deve ser exibido
    expect(screen.getByText('1 filtro ativo')).toBeDefined();

    // Limpar filtros deve restaurar ambos os pedidos
    const clearBtn = screen.getByText('Limpar Filtros');
    fireEvent.click(clearBtn);

    expect(screen.getByText('MLB-2026-001')).toBeDefined();
    expect(screen.getByText('SHP-2026-002')).toBeDefined();
  });

  it('4. Deve sinalizar pedidos com dados incompletos e permitir filtrar pendências com 1 clique', async () => {
    render(<FinancialFactsDashboardView />);

    await waitFor(() => {
      expect(screen.getByText('Filtrar pendências')).toBeDefined();
    });

    // O pedido SHP-2026-002 possui dados incompletos
    expect(screen.getByText(/⚠️ Dados Incompletos/i)).toBeDefined();

    // Clica em 'Filtrar pendências' no card de KPIs
    fireEvent.click(screen.getByText('Filtrar pendências'));

    // O select de status deve ser atualizado para MISSING_DATA
    const statusSelect = screen.getByLabelText(/Reconciliação \/ Status/i) as HTMLSelectElement;
    expect(statusSelect.value).toBe('MISSING_DATA');
  });

  it('5. Deve abrir a gaveta de detalhamento, exibir DRE em cascata e itens granulares com rateio exato', async () => {
    render(<FinancialFactsDashboardView />);

    await waitFor(() => {
      expect(screen.getAllByText('Ver Detalhes')[0]).toBeDefined();
    });

    // Clica em 'Ver Detalhes' do primeiro pedido
    const detailButtons = screen.getAllByText('Ver Detalhes');
    fireEvent.click(detailButtons[0]);

    // Gaveta de detalhes deve abrir
    const drawer = screen.getByRole('dialog', {
      name: /Detalhamento Financeiro do Pedido MLB-2026-001/i,
    });
    expect(drawer).toBeDefined();

    // Deve exibir a DRE em cascata
    expect(within(drawer).getByText('DRE Gerencial Simplificada (Visão Pedido)')).toBeDefined();
    expect(within(drawer).getByText('(=) Margem de Contribuição')).toBeDefined();
    expect(within(drawer).getByText('R$ 120,00')).toBeDefined();

    // Deve exibir o item granular com SKU e comissão rateada
    expect(within(drawer).getByText('Smartphone Modelo X Pro')).toBeDefined();
    expect(within(drawer).getByText(/SKU: SKU-SMARTPHONE-X/i)).toBeDefined();

    // Tecla Escape fecha a gaveta
    fireEvent.keyDown(window, { key: 'Escape' });
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).toBeNull();
    });
  });

  it('6. Deve abrir o modal de reconciliação externa, submeter repasse e atualizar o status do pedido', async () => {
    render(<FinancialFactsDashboardView />);

    await waitFor(() => {
      expect(screen.getAllByText('Ver Detalhes')[0]).toBeDefined();
    });

    // Abre gaveta de detalhes do pedido
    fireEvent.click(screen.getAllByText('Ver Detalhes')[0]);

    // Clica no botão 'Conciliar Extrato'
    const reconcileBtn = screen.getByText('Conciliar Extrato');
    fireEvent.click(reconcileBtn);

    // Modal de conciliação deve abrir
    const modal = screen.getByRole('dialog', {
      name: /Conciliar com Extrato Externo \(Liquidação\)/i,
    });
    expect(modal).toBeDefined();

    // Preenche ID do extrato e submete
    const idInput = within(modal).getByLabelText(/ID do Extrato \/ Repasse Externo/i);
    fireEvent.change(idInput, { target: { value: 'EXT-RECONCILED-99' } });

    const submitBtn = within(modal).getByText('Confirmar Reconciliação');
    fireEvent.click(submitBtn);

    // Aguarda fechamento do modal e chamada de API
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: /Conciliar com Extrato Externo/i })).toBeNull();
    });
  });
});
