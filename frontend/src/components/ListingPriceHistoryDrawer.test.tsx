import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ListingPriceHistoryDrawer, ListingPriceHistoryResponse } from './ListingPriceHistoryDrawer';

describe('ListingPriceHistoryDrawer', () => {
  const mockInitialData: ListingPriceHistoryResponse = {
    marketplace: 'MERCADO_LIVRE',
    externalListingId: 'MLB-123456',
    title: 'Monitor Gamer 27 165Hz IPS',
    dataPointsCount: 2,
    hasHistoricalTrend: true,
    minPrice: 1299.0,
    maxPrice: 1499.0,
    latestPrice: 1299.0,
    earliestPrice: 1499.0,
    overallVariationPercentage: -13.34,
    timeWindowDays: 10,
    trendDescription: 'Redução de 13.34% em 10 dias.',
    historyNotice: 'Histórico sem retroatividade inventada: apenas coletas reais com timestamp auditável são exibidas.',
    points: [
      {
        id: 'p1',
        price: 1499.0,
        currency: 'BRL',
        shippingCost: 0.0,
        observedAt: '2026-11-01T10:00:00Z',
        sourceType: 'OBSERVED',
        isPromotional: false,
        note: 'Coleta 1',
      },
      {
        id: 'p2',
        price: 1299.0,
        currency: 'BRL',
        shippingCost: 0.0,
        observedAt: '2026-11-11T10:00:00Z',
        sourceType: 'OBSERVED',
        isPromotional: true,
        note: 'Promoção Black',
      },
    ],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('não renderiza nada quando isOpen é false', () => {
    const { container } = render(
      <ListingPriceHistoryDrawer
        isOpen={false}
        onClose={vi.fn()}
        marketplace="MERCADO_LIVRE"
        externalListingId="MLB-123456"
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renderiza corretamente com dados iniciais fornecidos', () => {
    render(
      <ListingPriceHistoryDrawer
        isOpen={true}
        onClose={vi.fn()}
        marketplace="MERCADO_LIVRE"
        externalListingId="MLB-123456"
        initialData={mockInitialData}
      />
    );

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Histórico Legítimo de Preços')).toBeInTheDocument();
    expect(screen.getByText('Monitor Gamer 27 165Hz IPS')).toBeInTheDocument();
    expect(screen.getByText(/Histórico sem retroatividade inventada/i)).toBeInTheDocument();
    expect(screen.getByText('Redução de 13.34% em 10 dias.')).toBeInTheDocument();
    expect(screen.getByText('Promo')).toBeInTheDocument();
  });

  it('fecha ao pressionar a tecla ESC ou clicar no botão fechar', () => {
    const handleClose = vi.fn();
    render(
      <ListingPriceHistoryDrawer
        isOpen={true}
        onClose={handleClose}
        marketplace="MERCADO_LIVRE"
        externalListingId="MLB-123456"
        initialData={mockInitialData}
      />
    );

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(handleClose).toHaveBeenCalledTimes(1);

    const closeBtn = screen.getByLabelText('Fechar histórico de preços');
    fireEvent.click(closeBtn);
    expect(handleClose).toHaveBeenCalledTimes(2);
  });

  it('busca histórico via API quando initialData não é fornecido', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockInitialData,
    } as Response);

    render(
      <ListingPriceHistoryDrawer
        isOpen={true}
        onClose={vi.fn()}
        marketplace="MERCADO_LIVRE"
        externalListingId="MLB-123456"
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Monitor Gamer 27 165Hz IPS')).toBeInTheDocument();
    });
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/market-comparison/price-history?marketplace=MERCADO_LIVRE&externalListingId=MLB-123456'
    );
  });

  it('exibe mensagem de erro quando fetch falha', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ message: 'Serviço temporariamente indisponível' }),
    } as Response);

    render(
      <ListingPriceHistoryDrawer
        isOpen={true}
        onClose={vi.fn()}
        marketplace="MERCADO_LIVRE"
        externalListingId="MLB-123456"
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Erro ao carregar histórico')).toBeInTheDocument();
      expect(screen.getByText('Serviço temporariamente indisponível')).toBeInTheDocument();
    });
  });

  it('exibe estado vazio quando não há pontos históricos', () => {
    const emptyData: ListingPriceHistoryResponse = {
      marketplace: 'MERCADO_LIVRE',
      externalListingId: 'MLB-999',
      dataPointsCount: 0,
      hasHistoricalTrend: false,
      points: [],
    };

    render(
      <ListingPriceHistoryDrawer
        isOpen={true}
        onClose={vi.fn()}
        marketplace="MERCADO_LIVRE"
        externalListingId="MLB-999"
        initialData={emptyData}
      />
    );

    expect(screen.getByText('Nenhum ponto histórico persistido')).toBeInTheDocument();
  });
});
