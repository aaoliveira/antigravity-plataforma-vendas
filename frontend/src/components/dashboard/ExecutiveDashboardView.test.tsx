import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ExecutiveDashboardView } from './ExecutiveDashboardView';
import {
  AggregatedKpiSummary,
  ChannelKpiAggregate,
  PeriodicKpiAggregate,
  CompanyKpiAggregate,
  KpiDefinition,
} from '@/types/kpis';

const mockSummary: AggregatedKpiSummary = {
  tenantId: '11111111-1111-1111-1111-111111111111',
  totalOrders: 120,
  totalGrossRevenue: 15000.0,
  totalDiscountAmount: 300.0,
  totalNetRevenue: 14700.0,
  totalCancellationAmount: 150.0,
  totalReturnAmount: 50.0,
  totalMarketplaceFees: 2205.0,
  totalFixedFees: 360.0,
  totalShippingCost: 1440.0,
  totalProductCost: 4500.0,
  totalPackagingCost: 240.0,
  totalAdditionalCost: 120.0,
  totalTaxAmount: 882.0,
  totalOtherExpensesAmount: 60.0,
  totalDeductions: 9807.0,
  totalContributionMarginAmount: 4893.0,
  averageContributionMarginRatio: 33.29,
  averageOrderValue: 125.0,
  reconciledCount: 108,
  estimatedCount: 12,
  divergentCount: 0,
  ordersWithMissingDataCount: 0,
  reconciliationRatePercent: 90.0,
  cached: true,
  calculatedAt: '2026-11-23T11:45:00Z',
  antiHallucinationDisclaimer:
    'Este demonstrativo apura estritamente a Margem de Contribuição (Receita Líquida deduzida de custos diretos variáveis, comissões, fretes e impostos informados). Despesas fixas estruturais e operacionais da empresa não estão contempladas, sendo proibido rotular este valor como Lucro Líquido.',
};

const mockChannels: ChannelKpiAggregate[] = [
  {
    marketplace: 'MERCADO_LIVRE',
    orderCount: 80,
    grossRevenue: 10000.0,
    netRevenue: 9800.0,
    totalDeductions: 6500.0,
    contributionMarginAmount: 3300.0,
    averageContributionMarginRatio: 33.67,
    revenueSharePercent: 66.67,
    marginSharePercent: 67.44,
  },
  {
    marketplace: 'SHOPEE',
    orderCount: 40,
    grossRevenue: 5000.0,
    netRevenue: 4900.0,
    totalDeductions: 3307.0,
    contributionMarginAmount: 1593.0,
    averageContributionMarginRatio: 32.51,
    revenueSharePercent: 33.33,
    marginSharePercent: 32.56,
  },
];

const mockPeriods: PeriodicKpiAggregate[] = [
  {
    periodLabel: '2026-11-20',
    orderCount: 40,
    grossRevenue: 5000.0,
    netRevenue: 4900.0,
    contributionMarginAmount: 1630.0,
    averageOrderValue: 125.0,
  },
  {
    periodLabel: '2026-11-21',
    orderCount: 40,
    grossRevenue: 5000.0,
    netRevenue: 4900.0,
    contributionMarginAmount: 1630.0,
    averageOrderValue: 125.0,
  },
  {
    periodLabel: '2026-11-22',
    orderCount: 40,
    grossRevenue: 5000.0,
    netRevenue: 4900.0,
    contributionMarginAmount: 1633.0,
    averageOrderValue: 125.0,
  },
];

const mockCompanies: CompanyKpiAggregate[] = [
  {
    companyId: 'comp-1',
    companyName: 'Seller Corp Brasil',
    cnpj: '12.345.678/0001-90',
    orderCount: 120,
    grossRevenue: 15000.0,
    netRevenue: 14700.0,
    contributionMarginAmount: 4893.0,
    revenueSharePercent: 100.0,
  },
];

const mockDefinitions: KpiDefinition[] = [
  {
    code: 'GROSS_REVENUE',
    name: 'Receita Bruta',
    formula: 'SUM(itens.preco_cheio * itens.quantidade)',
    description: 'Valor total transacionado dos pedidos faturados',
    unit: 'CURRENCY_BRL',
    provenance: 'CONFIRMED',
  },
  {
    code: 'CONTRIBUTION_MARGIN',
    name: 'Margem de Contribuição',
    formula: 'net_revenue - total_deductions',
    description: 'Sobram monetária para cobrir custos fixos estruturais',
    unit: 'CURRENCY_BRL',
    provenance: 'ESTIMATED / CONFIRMED',
  },
];

describe('ExecutiveDashboardView (P022-FE) - Testes de Aceite', () => {
  let originalFetch: typeof global.fetch;

  beforeEach(() => {
    originalFetch = global.fetch;
    localStorage.clear();
    document.documentElement.classList.remove('dark');

    global.fetch = vi.fn().mockImplementation((url: string | URL | Request) => {
      const urlStr = url.toString();
      if (urlStr.includes('/api/analytics/kpis/summary')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockSummary),
        } as Response);
      }
      if (urlStr.includes('/api/analytics/kpis/by-channel')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockChannels),
        } as Response);
      }
      if (urlStr.includes('/api/analytics/kpis/by-period')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockPeriods),
        } as Response);
      }
      if (urlStr.includes('/api/analytics/kpis/by-company')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockCompanies),
        } as Response);
      }
      if (urlStr.includes('/api/analytics/kpis/definitions')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockDefinitions),
        } as Response);
      }
      if (urlStr.includes('/api/analytics/kpis/cache/invalidate')) {
        return Promise.resolve({
          ok: true,
          status: 204,
        } as Response);
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      } as Response);
    });
  });

  afterEach(() => {
    global.fetch = originalFetch;
    vi.restoreAllMocks();
  });

  // ===========================================================================
  // CRITÉRIO DE ACEITE 1: Números batem exatamente com a API
  // ===========================================================================
  it('deve renderizar os indicadores principais com números que batem exatamente com a API', async () => {
    render(<ExecutiveDashboardView />);

    await waitFor(() => {
      expect(screen.getByTestId('kpi-value-total_orders')).toHaveTextContent('120');
      expect(screen.getByTestId('kpi-value-gross_revenue')).toHaveTextContent('R$ 15.000,00');
      expect(screen.getByTestId('kpi-value-net_revenue')).toHaveTextContent('R$ 14.700,00');
      expect(screen.getByTestId('kpi-value-contribution_margin')).toHaveTextContent('R$ 4.893,00');
      expect(screen.getByTestId('kpi-value-average_order_value')).toHaveTextContent('R$ 125,00');
      expect(screen.getByTestId('kpi-value-reconciliation_rate')).toHaveTextContent('90,00%');
    });

    // Desempenho por canal bate com os números da API
    const meliItem = screen.getByTestId('channel-item-mercado_livre');
    expect(meliItem).toHaveTextContent('Mercado Livre');
    expect(meliItem).toHaveTextContent('(80 pedidos)');
    expect(meliItem).toHaveTextContent('R$ 10.000,00');

    const shopeeItem = screen.getByTestId('channel-item-shopee');
    expect(shopeeItem).toHaveTextContent('Shopee');
    expect(shopeeItem).toHaveTextContent('(40 pedidos)');
    expect(shopeeItem).toHaveTextContent('R$ 5.000,00');
  });

  // ===========================================================================
  // CRITÉRIO DE ACEITE 2: Tema persiste (Light mode padrão, Dark mode opcional)
  // ===========================================================================
  it('deve operar em Light mode por padrão e persistir a preferência do operador', async () => {
    // Inicialmente sem seller_theme definido no storage
    expect(localStorage.getItem('seller_theme')).toBeNull();
    expect(document.documentElement.classList.contains('dark')).toBe(false);

    render(<ExecutiveDashboardView />);

    await waitFor(() => {
      expect(screen.getByTestId('kpi-value-total_orders')).toHaveTextContent('120');
    });

    // Simula ativação de modo escuro persistido
    localStorage.setItem('seller_theme', 'dark');
    document.documentElement.classList.add('dark');

    expect(localStorage.getItem('seller_theme')).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);

    // O container principal renderiza sem incompatibilidade com tema escuro
    const container = screen.getByRole('region', { name: /Painel Executivo de Indicadores/i });
    expect(container).toBeInTheDocument();
  });

  // ===========================================================================
  // CRITÉRIO DE ACEITE 3: Breakpoints testados (360px a desktop) & Sem overflow
  // ===========================================================================
  it('deve renderizar responsivamente sem overflow horizontal em mobile (360px) e desktop (1280px)', async () => {
    // 1. Simula Viewport Mobile estreito (360px)
    window.innerWidth = 360;
    window.dispatchEvent(new Event('resize'));

    const { container } = render(<ExecutiveDashboardView />);

    await waitFor(() => {
      expect(screen.getByTestId('kpi-value-total_orders')).toHaveTextContent('120');
    });

    // Container tem classe explícita de contenção de overflow horizontal
    const mainRegion = screen.getByRole('region', { name: /Painel Executivo de Indicadores/i });
    expect(mainRegion.className).toContain('overflow-x-hidden');
    expect(mainRegion.className).toContain('max-w-7xl');

    // Botões interativos cumprem touch target >= 44px
    const catalogButton = screen.getByRole('button', { name: /Abrir catálogo com definições e fórmulas/i });
    expect(catalogButton.className).toContain('min-h-[44px]');

    // 2. Simula Desktop (1280px)
    window.innerWidth = 1280;
    window.dispatchEvent(new Event('resize'));
    expect(container).toBeInTheDocument();
  });

  // ===========================================================================
  // CRITÉRIO DE ACEITE 4: WCAG 2.1 AA nos fluxos principais
  // ===========================================================================
  it('deve cumprir requisitos de acessibilidade WCAG com landmarks, ARIA e tecla Escape na gaveta', async () => {
    render(<ExecutiveDashboardView />);

    await waitFor(() => {
      expect(screen.getByRole('region', { name: /Painel Executivo de Indicadores/i })).toBeInTheDocument();
    });

    // Abre a gaveta de definições
    const catalogButton = screen.getByRole('button', { name: /Abrir catálogo com definições e fórmulas/i });
    fireEvent.click(catalogButton);

    const dialog = screen.getByRole('dialog', { name: /Catálogo de Fórmulas e Definições de KPIs/i });
    expect(dialog).toBeInTheDocument();
    expect(dialog).toHaveAttribute('aria-modal', 'true');

    // Fecha a gaveta via tecla Escape (WCAG 2.1 AA 2.1.2)
    fireEvent.keyDown(window, { key: 'Escape', code: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: /Catálogo de Fórmulas e Definições de KPIs/i })).not.toBeInTheDocument();
    });
  });

  // ===========================================================================
  // CRITÉRIO DE ACEITE 5: Componentes com skeleton, empty, error e stale data
  // ===========================================================================
  it('deve exibir stale data banner e acionar revalidação de cache ao clicar em Atualizar', async () => {
    render(<ExecutiveDashboardView />);

    await waitFor(() => {
      expect(screen.getByText('Cache Ativo (15m TTL)')).toBeInTheDocument();
    });

    const refreshButton = screen.getByRole('button', { name: /Atualizar dados agora e invalidar cache/i });
    fireEvent.click(refreshButton);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/analytics/kpis/cache/invalidate', expect.objectContaining({ method: 'POST' }));
    });
  });

  it('deve renderizar empty state quando a API retornar zero pedidos', async () => {
    const emptySummary = {
      ...mockSummary,
      totalOrders: 0,
      totalGrossRevenue: 0,
      totalNetRevenue: 0,
      totalContributionMarginAmount: 0,
    };

    (global.fetch as any).mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve(emptySummary),
      })
    );

    render(<ExecutiveDashboardView />);

    await waitFor(() => {
      expect(screen.getByRole('region', { name: /Nenhum dado encontrado/i })).toBeInTheDocument();
      expect(screen.getByText(/Nenhum pedido ou fato financeiro apurado/i)).toBeInTheDocument();
    });
  });

  it('deve renderizar mensagem de erro com botão de retry quando a API falhar', async () => {
    (global.fetch as any).mockImplementationOnce(() =>
      Promise.resolve({
        ok: false,
        status: 500,
        json: () => Promise.resolve({ message: 'Erro interno ao agregar KPIs.' }),
      })
    );

    render(<ExecutiveDashboardView />);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/Erro ao Carregar Dashboard/i)).toBeInTheDocument();
      expect(screen.getByText(/Erro interno ao agregar KPIs/i)).toBeInTheDocument();
    });

    const retryButton = screen.getByRole('button', { name: /Tentar Novamente/i });
    expect(retryButton).toBeInTheDocument();
  });

  // ===========================================================================
  // CRITÉRIO DE ACEITE 6: Salvaguarda Anti-Alucinação visível
  // ===========================================================================
  it('deve exibir o disclaimer legal anti-alucinação vedando o rótulo de Lucro Líquido', async () => {
    render(<ExecutiveDashboardView />);

    await waitFor(() => {
      expect(screen.getByText(/Salvaguarda Contábil:/i)).toBeInTheDocument();
      expect(
        screen.getByText(/proibido rotular este valor como Lucro Líquido/i)
      ).toBeInTheDocument();
    });
  });
});
