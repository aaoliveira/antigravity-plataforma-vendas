import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ListingAnalyticsDashboardView } from './ListingAnalyticsDashboardView';
import {
  AccountMetricDto,
  MetricsSummaryDto,
  MetricCatalogEntry,
} from '@/types/metrics';

const mockCurrentSummary: MetricsSummaryDto = {
  totalMetricsRecorded: 10,
  availableMetricsCount: 8,
  unavailableMetricsCount: 1,
  insufficientDataCount: 0,
  unsupportedByChannelCount: 1,
  totalVisits: 2500,
  totalOrders: 75,
  totalUnitsSold: 90,
  totalRevenue: 12500.0,
  totalClicks: 0,
  totalImpressions: 0,
  overallConversionRateOrders: 3.0,
  overallCtr: 0.0,
  antiHallucinationNotice: 'Métricas oficiais sincronizadas sem estimativas silenciosas.',
};

const mockPreviousSummary: MetricsSummaryDto = {
  totalMetricsRecorded: 10,
  availableMetricsCount: 8,
  unavailableMetricsCount: 1,
  insufficientDataCount: 0,
  unsupportedByChannelCount: 1,
  totalVisits: 2000,
  totalOrders: 60,
  totalUnitsSold: 70,
  totalRevenue: 10000.0,
  totalClicks: 0,
  totalImpressions: 0,
  overallConversionRateOrders: 3.0,
  overallCtr: 0.0,
  antiHallucinationNotice: 'Métricas oficiais sincronizadas sem estimativas silenciosas.',
};

const mockMetricsList: AccountMetricDto[] = [
  {
    id: 'm-1',
    tenantId: '11111111-1111-1111-1111-111111111111',
    sellerAccountId: 'acc-1',
    marketplace: 'MERCADO_LIVRE',
    listingId: 'list-1',
    externalListingId: 'MLB1234567890',
    targetIdentifier: 'MLB1234567890',
    metricScope: 'LISTING',
    granularity: 'DAILY',
    windowStart: '2026-11-18T00:00:00Z',
    windowEnd: '2026-11-18T23:59:59Z',
    metricKey: 'VISITS',
    metricValue: 120,
    metricUnit: 'COUNT',
    provenance: 'OFFICIAL_API',
    formulaDefinition: null,
    denominatorMetric: null,
    denominatorValue: null,
    numeratorMetric: null,
    numeratorValue: null,
    availabilityStatus: 'AVAILABLE',
    unavailabilityReason: null,
    sourceEndpoint: '/items/{id}/visits/time_window',
    ingestedAt: '2026-11-19T10:00:00Z',
    metadataJson: null,
  },
  {
    id: 'm-2',
    tenantId: '11111111-1111-1111-1111-111111111111',
    sellerAccountId: 'acc-1',
    marketplace: 'MERCADO_LIVRE',
    listingId: 'list-1',
    externalListingId: 'MLB1234567890',
    targetIdentifier: 'MLB1234567890',
    metricScope: 'LISTING',
    granularity: 'DAILY',
    windowStart: '2026-11-19T00:00:00Z',
    windowEnd: '2026-11-19T23:59:59Z',
    metricKey: 'VISITS',
    metricValue: 150,
    metricUnit: 'COUNT',
    provenance: 'OFFICIAL_API',
    formulaDefinition: null,
    denominatorMetric: null,
    denominatorValue: null,
    numeratorMetric: null,
    numeratorValue: null,
    availabilityStatus: 'AVAILABLE',
    unavailabilityReason: null,
    sourceEndpoint: '/items/{id}/visits/time_window',
    ingestedAt: '2026-11-19T10:00:00Z',
    metadataJson: null,
  },
  {
    id: 'm-3',
    tenantId: '11111111-1111-1111-1111-111111111111',
    sellerAccountId: 'acc-1',
    marketplace: 'MERCADO_LIVRE',
    listingId: 'list-1',
    externalListingId: 'MLB1234567890',
    targetIdentifier: 'MLB1234567890',
    metricScope: 'LISTING',
    granularity: 'DAILY',
    windowStart: '2026-11-18T00:00:00Z',
    windowEnd: '2026-11-18T23:59:59Z',
    metricKey: 'ORDERS_COUNT',
    metricValue: 4,
    metricUnit: 'COUNT',
    provenance: 'OFFICIAL_API',
    formulaDefinition: null,
    denominatorMetric: null,
    denominatorValue: null,
    numeratorMetric: null,
    numeratorValue: null,
    availabilityStatus: 'AVAILABLE',
    unavailabilityReason: null,
    sourceEndpoint: '/orders/search',
    ingestedAt: '2026-11-19T10:00:00Z',
    metadataJson: null,
  },
  {
    id: 'm-4',
    tenantId: '11111111-1111-1111-1111-111111111111',
    sellerAccountId: 'acc-1',
    marketplace: 'MERCADO_LIVRE',
    listingId: 'list-1',
    externalListingId: 'MLB1234567890',
    targetIdentifier: 'MLB1234567890',
    metricScope: 'LISTING',
    granularity: 'DAILY',
    windowStart: '2026-11-18T00:00:00Z',
    windowEnd: '2026-11-18T23:59:59Z',
    metricKey: 'CTR',
    metricValue: null,
    metricUnit: 'PERCENTAGE',
    provenance: 'MANUAL_IMPORT',
    formulaDefinition: 'ctr = (cliques / impressoes) * 100',
    denominatorMetric: 'IMPRESSIONS',
    denominatorValue: 0,
    numeratorMetric: 'CLICKS',
    numeratorValue: 0,
    availabilityStatus: 'UNSUPPORTED_BY_CHANNEL',
    unavailabilityReason: 'O Mercado Livre não fornece dados de impressões em tráfego orgânico via API oficial.',
    sourceEndpoint: 'UNSUPPORTED_BY_CHANNEL',
    ingestedAt: '2026-11-19T10:00:00Z',
    metadataJson: null,
  },
];

const mockCatalog: MetricCatalogEntry[] = [
  {
    marketplace: 'MERCADO_LIVRE',
    metricKey: 'VISITS',
    displayName: 'Visitas do Anúncio',
    scope: 'LISTING',
    supportedGranularities: ['DAILY', 'PERIOD_TOTAL'],
    unit: 'COUNT',
    available: true,
    unavailabilityReason: null,
    officialSourceEndpoint: '/items/{id}/visits/time_window',
    maxSupportedWindowDays: 365,
    rateLimitPerMinute: 120,
    formulaDefinition: null,
  },
  {
    marketplace: 'MERCADO_LIVRE',
    metricKey: 'CTR',
    displayName: 'Taxa de Cliques (CTR)',
    scope: 'LISTING',
    supportedGranularities: ['DAILY'],
    unit: 'PERCENTAGE',
    available: false,
    unavailabilityReason: 'O Mercado Livre não fornece dados de impressões em tráfego orgânico via API oficial. Dados disponíveis apenas sob campanhas ativas do Mercado Ads.',
    officialSourceEndpoint: 'UNSUPPORTED_BY_CHANNEL (Mercado Livre Orgânico)',
    maxSupportedWindowDays: 0,
    rateLimitPerMinute: 0,
    formulaDefinition: 'ctr = (cliques / impressoes) * 100',
  },
];

const mockAccounts = [
  { id: 'acc-1', accountName: 'Loja Oficial Meli', marketplace: 'MERCADO_LIVRE', externalAccountId: 'MELI-123' },
];

describe('ListingAnalyticsDashboardView Component (P018-FE)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    global.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/metrics/summary')) {
        // Se for o período anterior na query
        if (url.includes('endDate=') && !url.includes(new Date().toISOString().substring(0, 7))) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockPreviousSummary),
          });
        }
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockCurrentSummary),
        });
      }
      if (url.includes('/api/metrics/catalog')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockCatalog),
        });
      }
      if (url.includes('/api/metrics') && !url.includes('/summary') && !url.includes('/catalog')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: mockMetricsList, totalElements: mockMetricsList.length }),
        });
      }
      if (url.includes('/api/catalog/seller-accounts')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockAccounts),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      });
    });
  });

  it('1. Deve renderizar o painel com período e timezone explícitos', async () => {
    render(<ListingAnalyticsDashboardView />);

    expect(screen.getByText('Analytics de Anúncios & Desempenho Oficial')).toBeInTheDocument();
    expect(screen.getAllByText(/America\/Sao_Paulo \(UTC-03:00\)/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/Diretriz de Transparência e Anti-Alucinação Estrita/i)).toBeInTheDocument();

    // Presets disponíveis
    expect(screen.getByRole('button', { name: 'Hoje' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '7 Dias' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '14 Dias' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '30 Dias' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mês Atual' })).toBeInTheDocument();
  });

  it('2. Deve exibir os cards de KPIs com valores e comparação temporal correta', async () => {
    render(<ListingAnalyticsDashboardView />);

    await waitFor(() => {
      // Visitas: 2.500 (+25.0% vs. 2000 anterior)
      expect(screen.getByText('Visitas Oficiais')).toBeInTheDocument();
      expect(screen.getByText('2.500')).toBeInTheDocument();
      expect(screen.getAllByText('+25.0%').length).toBeGreaterThan(0);

      // Pedidos: 75 (+25.0% vs. 60 anterior)
      expect(screen.getByText('Pedidos de Venda')).toBeInTheDocument();
      expect(screen.getByText('75')).toBeInTheDocument();

      // Receita Bruta: R$ 12.500,00
      expect(screen.getByText('Receita Bruta Confirmada')).toBeInTheDocument();
      expect(screen.getByText(/12\.500/)).toBeInTheDocument();

      // Taxa de conversão: 3.00%
      expect(screen.getByText('Taxa de Conversão (Pedidos)')).toBeInTheDocument();
      expect(screen.getByText('3.00%')).toBeInTheDocument();
    });
  });

  it('3. Deve aplicar a regra anti-alucinação no CTR orgânico do Mercado Livre com status Não Suportado', async () => {
    render(<ListingAnalyticsDashboardView />);

    await waitFor(() => {
      expect(screen.getByText('CTR (Taxa de Cliques)')).toBeInTheDocument();
      expect(screen.getAllByText('Não Suportado').length).toBeGreaterThan(0);
      expect(
        screen.getByText(/O Mercado Livre não fornece dados de impressões em tráfego orgânico via API oficial/i)
      ).toBeInTheDocument();
    });
  });

  it('4. Deve exibir tooltips de fórmulas oficiais acessíveis por teclado/clique', async () => {
    render(<ListingAnalyticsDashboardView />);

    await waitFor(() => {
      expect(screen.getByText('Taxa de Conversão (Pedidos)')).toBeInTheDocument();
    });

    const helpBtn = screen.getByLabelText('Ver fórmula e auditoria de Taxa de Conversão (Pedidos)');
    fireEvent.click(helpBtn);

    expect(screen.getByRole('tooltip')).toBeInTheDocument();
    expect(screen.getByText(/conversao_pedidos = \(total_pedidos \/ total_visitas\) × 100/i)).toBeInTheDocument();
    expect(screen.getByText(/Cálculo determinístico em BigDecimal/i)).toBeInTheDocument();
  });

  it('5. Deve identificar lacunas de coleta no gráfico temporal e permitir alternar para tabela diária', async () => {
    render(<ListingAnalyticsDashboardView />);

    await waitFor(() => {
      expect(screen.getByText('Evolução Temporal Diária')).toBeInTheDocument();
    });

    // Deve acusar aviso de lacunas de coleta para os dias sem registro na janela
    expect(screen.getByRole('region', { name: 'Aviso de lacunas de coleta' })).toBeInTheDocument();
    expect(screen.getByText(/lacunas de coleta identificadas no intervalo/i)).toBeInTheDocument();

    // Alternar para visualização em tabela diária
    const tableToggle = screen.getByRole('button', { name: 'Tabela Diária' });
    fireEvent.click(tableToggle);

    expect(screen.getByText('Data (America/Sao_Paulo)')).toBeInTheDocument();
    expect(screen.getByText('Status de Coleta')).toBeInTheDocument();
  });

  it('6. Deve abrir e fechar a gaveta de Catálogo Oficial de Métricas e Limites', async () => {
    render(<ListingAnalyticsDashboardView />);

    const openCatalogBtn = screen.getByRole('button', { name: /Catálogo Oficial & Limites/i });
    fireEvent.click(openCatalogBtn);

    await waitFor(() => {
      expect(screen.getByRole('dialog', { name: 'Catálogo Oficial de Métricas e Capacidades de API' })).toBeInTheDocument();
      expect(screen.getByText('Visitas do Anúncio')).toBeInTheDocument();
      expect(screen.getByText('Oficial Homologada')).toBeInTheDocument();
      expect(screen.getByText('Taxa de Cliques (CTR)')).toBeInTheDocument();
      expect(screen.getByText('Não Disponível no Canal')).toBeInTheDocument();
    });

    // Fechar com Escape
    fireEvent.keyDown(window, { key: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: 'Catálogo Oficial de Métricas e Capacidades de API' })).not.toBeInTheDocument();
    });
  });

  it('7. Deve exibir a tabela de registros individuais e expandir detalhes de auditoria', async () => {
    render(<ListingAnalyticsDashboardView />);

    await waitFor(() => {
      expect(screen.getByText(/Registros Auditáveis de Métricas/i)).toBeInTheDocument();
      expect(screen.getAllByText('MLB1234567890').length).toBeGreaterThan(0);
    });

    const detailButtons = screen.getAllByRole('button', { name: 'Detalhes' });
    fireEvent.click(detailButtons[0]);

    expect(screen.getByText(/Trilha de Cálculo & Fórmula:/i)).toBeInTheDocument();
    expect(screen.getByText(/Auditoria & Disponibilidade:/i)).toBeInTheDocument();
  });
});
