import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { PriceHistoryChart, MonitoredItemSnapshotDto } from './PriceHistoryChart';

describe('PriceHistoryChart Component (P013-FE)', () => {
  const baseTimestamp = new Date('2026-11-18T12:00:00Z').getTime();

  const mockSnapshots: MonitoredItemSnapshotDto[] = [
    {
      id: 'snap-1',
      tenantId: 'tenant-1',
      itemId: 'item-100',
      groupId: 'group-1',
      marketplace: 'MERCADO_LIVRE',
      externalListingId: 'MLB100',
      price: 1500.0,
      currency: 'BRL',
      availableQuantity: 10,
      provenanceType: 'OFFICIAL',
      sourceType: 'OFFICIAL_API',
      sourceUrl: 'https://produto.mercadolivre.com.br/MLB-100',
      collectionStatus: 'SUCCESS',
      changeType: 'INITIAL',
      collectedAt: new Date(baseTimestamp - 4 * 60 * 60 * 1000).toISOString(), // -4h
      observedAt: new Date(baseTimestamp - 4 * 60 * 60 * 1000).toISOString(),
      validUntil: new Date(baseTimestamp - 3 * 60 * 60 * 1000).toISOString(),
      createdAt: new Date(baseTimestamp - 4 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: 'snap-2',
      tenantId: 'tenant-1',
      itemId: 'item-100',
      groupId: 'group-1',
      marketplace: 'MERCADO_LIVRE',
      externalListingId: 'MLB100',
      price: 1450.0,
      currency: 'BRL',
      availableQuantity: 8,
      provenanceType: 'OFFICIAL',
      sourceType: 'OFFICIAL_API',
      sourceUrl: 'https://produto.mercadolivre.com.br/MLB-100',
      collectionStatus: 'SUCCESS',
      changeType: 'PRICE_DECREASE',
      priceChangeAmount: -50.0,
      priceChangePercent: -3.33,
      collectedAt: new Date(baseTimestamp - 3 * 60 * 60 * 1000).toISOString(), // -3h (1h após snap-1: dentro do intervalo)
      observedAt: new Date(baseTimestamp - 3 * 60 * 60 * 1000).toISOString(),
      validUntil: new Date(baseTimestamp - 2 * 60 * 60 * 1000).toISOString(),
      createdAt: new Date(baseTimestamp - 3 * 60 * 60 * 1000).toISOString(),
    },
    {
      // Snapshot após 24h de diferença (GAP > 2x 60min)
      id: 'snap-3',
      tenantId: 'tenant-1',
      itemId: 'item-100',
      groupId: 'group-1',
      marketplace: 'MERCADO_LIVRE',
      externalListingId: 'MLB100',
      price: 1550.0,
      currency: 'BRL',
      availableQuantity: 5,
      provenanceType: 'OFFICIAL',
      sourceType: 'OFFICIAL_API',
      sourceUrl: 'https://produto.mercadolivre.com.br/MLB-100',
      collectionStatus: 'SUCCESS',
      changeType: 'PRICE_INCREASE',
      priceChangeAmount: 100.0,
      priceChangePercent: 6.9,
      collectedAt: new Date(baseTimestamp).toISOString(), // agora
      observedAt: new Date(baseTimestamp).toISOString(),
      validUntil: new Date(baseTimestamp + 60 * 60 * 1000).toISOString(),
      createdAt: new Date(baseTimestamp).toISOString(),
    },
  ];

  it('1. Deve renderizar o título, o marketplace e os cards de estatísticas (Atual, Mínimo, Máximo e Variação)', () => {
    render(
      <PriceHistoryChart
        snapshots={mockSnapshots}
        minIntervalMinutes={60}
        marketplace="MERCADO_LIVRE"
        itemTitle="Câmera Mirrorless 4K"
      />
    );

    expect(screen.getByRole('region', { name: /Gráfico de histórico de preços/i })).toBeInTheDocument();
    expect(screen.getByText('Histórico de Preços & Observações')).toBeInTheDocument();
    expect(screen.getByText('MERCADO_LIVRE')).toBeInTheDocument();
    expect(screen.getByText('Câmera Mirrorless 4K')).toBeInTheDocument();

    // KPIs do período
    expect(screen.getByText('Preço Atual')).toBeInTheDocument();
    expect(screen.getByText('Mínimo no Período')).toBeInTheDocument();
    expect(screen.getByText('Máximo no Período')).toBeInTheDocument();
    expect(screen.getByText('Variação no Período')).toBeInTheDocument();

    // Valores formatados
    expect(screen.getAllByText(/1\.550,00/).length).toBeGreaterThan(0); // Atual e Máximo
    expect(screen.getAllByText(/1\.450,00/).length).toBeGreaterThan(0); // Mínimo
  });

  it('2. Deve permitir alternar entre intervalos de tempo (24h, 7d, 30d, 90d, Tudo)', () => {
    render(
      <PriceHistoryChart
        snapshots={mockSnapshots}
        minIntervalMinutes={60}
      />
    );

    const btn24h = screen.getByRole('button', { name: '24h' });
    const btn7d = screen.getByRole('button', { name: '7d' });
    const btnAll = screen.getByRole('button', { name: 'Tudo' });

    expect(btnAll).toHaveAttribute('aria-pressed', 'true');
    expect(btn24h).toHaveAttribute('aria-pressed', 'false');

    fireEvent.click(btn24h);
    expect(btn24h).toHaveAttribute('aria-pressed', 'true');
    expect(btnAll).toHaveAttribute('aria-pressed', 'false');

    fireEvent.click(btn7d);
    expect(btn7d).toHaveAttribute('aria-pressed', 'true');
  });

  it('3. REGRA ANTI-ALUCINAÇÃO: Não interpolar visualmente dados ausentes e sinalizar gaps', () => {
    render(
      <PriceHistoryChart
        snapshots={mockSnapshots}
        minIntervalMinutes={60}
      />
    );

    // O mockSnapshots tem um gap entre snap-2 e snap-3 de quase 27 horas (> 2 * 60 min)
    // Deve haver marcador de lacuna no SVG
    const gapMarker = screen.getByText(/Lacuna/i);
    expect(gapMarker).toBeInTheDocument();

    // Clicar na lacuna deve abrir banner explicativo com garantia anti-alucinação
    fireEvent.click(gapMarker);
    expect(screen.getByText(/Lacuna de Coleta Identificada/i)).toBeInTheDocument();
    expect(
      screen.getByText(/A plataforma não interpola valores intermediários nem assume estabilidade artificial de preços/i)
    ).toBeInTheDocument();
  });

  it('4. Deve exibir detalhes da observação (tooltip / card de auditoria) ao clicar no ponto', () => {
    const handleSelect = vi.fn();
    render(
      <PriceHistoryChart
        snapshots={mockSnapshots}
        minIntervalMinutes={60}
        onSelectSnapshot={handleSelect}
      />
    );

    // Selecionar o último ponto (snap-3)
    const points = screen.getAllByRole('button', { name: /Observação em/i });
    expect(points.length).toBe(3);

    fireEvent.click(points[2]); // snap-3
    expect(handleSelect).toHaveBeenCalledWith(mockSnapshots[2]);

    // Verificar exibição do detalhe com proveniência e integridade
    expect(screen.getByRole('region', { name: /Detalhes da observação selecionada/i })).toBeInTheDocument();
    expect(screen.getByText(/Tipo da Fonte/i)).toBeInTheDocument();
    expect(screen.getAllByText('OFFICIAL_API').length).toBeGreaterThan(0);
    expect(screen.getByText('Status de Coleta')).toBeInTheDocument();
    expect(screen.getAllByText('SUCCESS').length).toBeGreaterThan(0);
    expect(screen.getByText('Estoque Disponível')).toBeInTheDocument();
    expect(screen.getByText('5 un.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Ver Anúncio na Fonte Oficial/i })).toHaveAttribute(
      'href',
      'https://produto.mercadolivre.com.br/MLB-100'
    );
  });

  it('5. Deve permitir navegação por teclado nos pontos com setas (ArrowRight, ArrowLeft) e Enter', () => {
    render(
      <PriceHistoryChart
        snapshots={mockSnapshots}
        minIntervalMinutes={60}
      />
    );

    const points = screen.getAllByRole('button', { name: /Observação em/i });

    // Pressionar ArrowRight no ponto 0 seleciona o ponto 1
    fireEvent.keyDown(points[0], { key: 'ArrowRight' });
    expect(screen.getByRole('region', { name: /Detalhes da observação selecionada/i })).toBeInTheDocument();
    expect(screen.getByText(/8 un\./)).toBeInTheDocument(); // snap-2 estoque

    // Pressionar ArrowLeft volta ao ponto 0
    fireEvent.keyDown(points[1], { key: 'ArrowLeft' });
    expect(screen.getByText(/10 un\./)).toBeInTheDocument(); // snap-1 estoque
  });

  it('6. Modo Alternativo em Tabela Acessível (WCAG 2.2 AA): Deve alternar e exibir gaps e dados estruturados', () => {
    render(
      <PriceHistoryChart
        snapshots={mockSnapshots}
        minIntervalMinutes={60}
      />
    );

    const tableToggleBtn = screen.getByRole('button', { name: /Visualização em Tabela/i });
    fireEvent.click(tableToggleBtn);

    // Deve exibir tabela com role e cabeçalhos
    expect(screen.getByRole('table', { name: /Tabela detalhada de observações/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /Preço/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /Fonte/i })).toBeInTheDocument();

    // Deve conter alerta de lacuna na tabela
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText(/Lacuna de Coleta/i)).toBeInTheDocument();
  });

  it('7. Estado vazio: Deve exibir mensagem amigável quando não houver snapshots', () => {
    render(
      <PriceHistoryChart
        snapshots={[]}
        minIntervalMinutes={60}
      />
    );

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/Nenhum snapshot encontrado para o período selecionado/i)).toBeInTheDocument();
  });

  it('8. Mobile: Botões e controles interativos devem possuir altura mínima de 44px (touch target WCAG)', () => {
    const { container } = render(
      <PriceHistoryChart
        snapshots={mockSnapshots}
        minIntervalMinutes={60}
      />
    );

    const touchButtons = container.querySelectorAll('button.min-h-\\[44px\\]');
    expect(touchButtons.length).toBeGreaterThan(0);
  });
});
