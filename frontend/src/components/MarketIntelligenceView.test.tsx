import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MarketIntelligenceView } from './MarketIntelligenceView';

const mockObservations = [
  {
    id: 'obs-1',
    tenantId: '11111111-1111-1111-1111-111111111111',
    observedListingId: 'MLB-1001',
    marketplace: 'MERCADO_LIVRE',
    title: 'Smartphone Samsung Galaxy A54 128GB 5G',
    price: 1499.9,
    currency: 'BRL',
    itemCondition: 'NEW',
    sellerIdentifier: 'Loja Oficial Samsung',
    sourceType: 'OFFICIAL',
    sourceUrl: 'https://produto.mercadolivre.com.br/MLB-1001',
    collectedAt: '2026-11-18T10:00:00Z',
    observedAt: '2026-11-18T09:30:00Z',
    validUntil: '2026-11-25T10:00:00Z',
    method: 'OFFICIAL_API_SYNC',
    confidence: 1.0,
    gtin: '7891234567890',
    brand: 'Samsung',
    minimizedPayload: '{"item":"Galaxy A54"}',
  },
  {
    id: 'obs-2',
    tenantId: '11111111-1111-1111-1111-111111111111',
    observedListingId: 'SHOPEE-2002',
    marketplace: 'SHOPEE',
    title: 'Capa Protetora Silicone Anti Queda A54',
    price: 29.9,
    currency: 'BRL',
    itemCondition: 'NEW',
    sellerIdentifier: 'Acessórios Celulares BR',
    sourceType: 'ESTIMATED',
    sourceUrl: 'https://shopee.com.br/product/SHOPEE-2002',
    collectedAt: '2026-11-18T11:00:00Z',
    observedAt: '2026-11-18T10:45:00Z',
    validUntil: '2026-11-25T11:00:00Z',
    method: 'ESTIMATION_REGRESSION_V2',
    confidence: 0.85,
    methodologyDescription: 'Regressão estatística de volume de avaliações dos últimos 30 dias',
    modelVersion: 'v2.1.0',
    timeWindow: 'LAST_30_DAYS',
    gtin: null,
    brand: null,
    minimizedPayload: '{"item":"Capa A54"}',
  },
  {
    id: 'obs-3',
    tenantId: '11111111-1111-1111-1111-111111111111',
    observedListingId: 'AMAZON-3003',
    marketplace: 'AMAZON',
    title: 'Monitor Gamer AOC 24 Polegadas 144Hz',
    price: 899.0,
    currency: 'BRL',
    itemCondition: 'NEW',
    sellerIdentifier: 'AOC Oficial',
    sourceType: 'OBSERVED',
    sourceUrl: 'https://amazon.com.br/dp/B003',
    collectedAt: '2026-11-18T12:00:00Z',
    observedAt: '2026-11-18T11:50:00Z',
    validUntil: '2026-11-25T12:00:00Z',
    method: 'PUBLIC_CATALOG_SCRAPER',
    confidence: 1.0,
    gtin: '7899999999999',
    brand: 'AOC',
    minimizedPayload: null,
  },
];

describe('P009-FE: MarketIntelligenceView & ObservationDetailDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    global.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/market-intelligence/observations?') || url === '/api/market-intelligence/observations') {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              items: mockObservations,
              page: 0,
              size: 10,
              totalElements: 3,
              totalPages: 1,
              hasMore: false,
            }),
        });
      }
      if (url.includes('/match')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve([
              {
                canonicalProductId: 'prod-1',
                productTitle: 'Samsung Galaxy A54 128GB Canônico',
                canonicalSku: 'SKU-A54',
                brand: 'Samsung',
                scoreBreakdown: {
                  overallScore: 0.94,
                  gtinScore: 1.0,
                  titleScore: 0.88,
                  brandScore: 1.0,
                  status: 'MATCHED',
                  explanation: 'GTIN idêntico (7891234567890) (+50%), similaridade textual (+35%) e mesma marca (+15%).',
                  thresholdApplied: 0.75,
                },
              },
            ]),
        });
      }
      return Promise.reject(new Error('Unknown URL: ' + url));
    });
  });

  it('1. Renderiza título da tela e cartões de KPI de proveniência', async () => {
    render(<MarketIntelligenceView />);

    await waitFor(() => {
      expect(screen.getByText('Inteligência de Mercado & Proveniência')).toBeInTheDocument();
    });

    expect(screen.getByText('Total Observações')).toBeInTheDocument();
    expect(screen.getByText('Dados Oficiais')).toBeInTheDocument();
    expect(screen.getByText('Dados Estimados')).toBeInTheDocument();
    expect(screen.getByText('Confiabilidade Média')).toBeInTheDocument();
  });

  it('2. Critério de Aceite: Dado estimado NUNCA aparece como oficial', async () => {
    render(<MarketIntelligenceView />);

    await waitFor(() => {
      expect(screen.getAllByText('Capa Protetora Silicone Anti Queda A54').length).toBeGreaterThan(0);
    });

    // O item estimativo deve possuir badge de estimado
    const estimatedBadges = screen.getAllByText('Estimado (Modelo)');
    expect(estimatedBadges.length).toBeGreaterThan(0);

    // O item estimativo não pode ser rotulado como Oficial
    const officialBadges = screen.getAllByText('Oficial');
    expect(officialBadges.length).toBeGreaterThan(0);
  });

  it('3. Exibe dados de origem, canal, identificador e preço formatado em BRL', async () => {
    render(<MarketIntelligenceView />);

    await waitFor(() => {
      expect(screen.getAllByText('Smartphone Samsung Galaxy A54 128GB 5G').length).toBeGreaterThan(0);
    });

    expect(screen.getAllByText('MLB-1001').length).toBeGreaterThan(0);
    expect(screen.getAllByText(/MERCADO LIVRE/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText('SHOPEE').length).toBeGreaterThan(0);
    expect(screen.getAllByText('AMAZON').length).toBeGreaterThan(0);
    expect(screen.getAllByText('R$ 1.499,90').length).toBeGreaterThan(0);
  });

  it('4. Permite filtrar por tipo de evidência / proveniência e busca textual', async () => {
    render(<MarketIntelligenceView />);

    await waitFor(() => {
      expect(screen.getAllByText('Smartphone Samsung Galaxy A54 128GB 5G').length).toBeGreaterThan(0);
    });

    const selectEvidencia = screen.getByLabelText(/Classificação \/ Evidência/i);
    fireEvent.change(selectEvidencia, { target: { value: 'ESTIMATED' } });

    const searchInput = screen.getByPlaceholderText(/Ex: Samsung Galaxy/i);
    fireEvent.change(searchInput, { target: { value: 'Capa' } });

    const filterButton = screen.getByRole('button', { name: /Filtrar/i });
    fireEvent.click(filterButton);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('sourceType=ESTIMATED')
      );
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('search=Capa')
      );
    });
  });

  it('5. Abre gaveta de detalhes e explicação acessível com role="dialog" e fecha com tecla ESC', async () => {
    render(<MarketIntelligenceView />);

    await waitFor(() => {
      expect(screen.getAllByText('Smartphone Samsung Galaxy A54 128GB 5G').length).toBeGreaterThan(0);
    });

    const verExplicacaoBtns = screen.getAllByRole('button', { name: /Ver Detalhes & Explicação|Ver Explicação/i });
    fireEvent.click(verExplicacaoBtns[0]);

    // Gaveta aberta
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    expect(screen.getByText('Classificação de Proveniência:')).toBeInTheDocument();
    expect(screen.getByText(/Acessar Anúncio Oficial no/i)).toBeInTheDocument();

    // Fecha pressionando ESC
    fireEvent.keyDown(window, { key: 'Escape', code: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  it('6. Gaveta exibe aviso de transparência e metodologia documentada para itens ESTIMATED', async () => {
    render(<MarketIntelligenceView />);

    await waitFor(() => {
      expect(screen.getAllByText('Capa Protetora Silicone Anti Queda A54').length).toBeGreaterThan(0);
    });

    // Clica no botão de explicação do segundo item (Capa A54 - ESTIMATED)
    const verExplicacaoBtns = screen.getAllByRole('button', { name: /Ver Detalhes & Explicação|Ver Explicação/i });
    fireEvent.click(verExplicacaoBtns[1]);

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    // Banner de transparência
    expect(screen.getByText(/Aviso Anti-Alucinação \/ Transparência Estatística/i)).toBeInTheDocument();
    expect(screen.getByText(/v2.1.0/i)).toBeInTheDocument();
    expect(screen.getByText(/LAST_30_DAYS/i)).toBeInTheDocument();
    expect(screen.getByText(/Regressão estatística de volume de avaliações/i)).toBeInTheDocument();
    expect(screen.getByText('85.0%')).toBeInTheDocument();
  });

  it('7. Executa cálculo de matching explicável dentro da gaveta e exibe score e justificativa', async () => {
    render(<MarketIntelligenceView />);

    await waitFor(() => {
      expect(screen.getAllByText('Smartphone Samsung Galaxy A54 128GB 5G').length).toBeGreaterThan(0);
    });

    const verExplicacaoBtns = screen.getAllByRole('button', { name: /Ver Detalhes & Explicação|Ver Explicação/i });
    fireEvent.click(verExplicacaoBtns[0]);

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    const calcularMatchingBtn = screen.getByRole('button', { name: /Calcular Matching/i });
    fireEvent.click(calcularMatchingBtn);

    await waitFor(() => {
      expect(screen.getByText(/Samsung Galaxy A54 128GB Canônico/i)).toBeInTheDocument();
      expect(screen.getByText(/MATCHED \(94%\)/i)).toBeInTheDocument();
      expect(screen.getByText(/GTIN idêntico \(7891234567890\)/i)).toBeInTheDocument();
    });
  });

  it('8. Trata estado vazio e de erro com acessibilidade', async () => {
    // Simula erro
    global.fetch = vi.fn().mockImplementation(() =>
      Promise.resolve({
        ok: false,
        status: 500,
      })
    );

    render(<MarketIntelligenceView />);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
    expect(screen.getByText('Falha na Consulta de Observações')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Tentar Novamente/i })).toBeInTheDocument();
  });

  it('9. Permite alternar para a aba de Comparação & Equivalência', async () => {
    render(<MarketIntelligenceView />);

    await waitFor(() => {
      expect(screen.getByText('⚖️ Comparação & Equivalência')).toBeInTheDocument();
    });

    const comparisonTabBtn = screen.getByText('⚖️ Comparação & Equivalência');
    fireEvent.click(comparisonTabBtn);

    expect(screen.getByText('⚖️ Comparação Lado a Lado de Listings Equivalentes')).toBeInTheDocument();
    expect(screen.getByText('🎯 REFERÊNCIA PRÓPRIA')).toBeInTheDocument();
  });
});
