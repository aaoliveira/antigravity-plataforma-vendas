import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MonitoringTimelineDrawer } from './MonitoringTimelineDrawer';

describe('MonitoringTimelineDrawer Component (P012-FE)', () => {
  const mockItem = {
    id: 'item-123',
    groupId: 'group-456',
    marketplace: 'MERCADO_LIVRE',
    externalListingId: 'MLB987654321',
    title: 'Monitor Gamer 27 Polegadas 165Hz IPS',
    sellerName: 'Oficial Tech Store',
    sourceUrl: 'https://produto.mercadolivre.com.br/MLB-987654321',
    createdAt: '2026-11-18T10:00:00Z',
    minIntervalMinutes: 60,
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('1. Não deve renderizar nada quando isOpen for false', () => {
    const { container } = render(
      <MonitoringTimelineDrawer isOpen={false} item={mockItem} onClose={() => {}} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('2. Deve renderizar cabeçalho com dados do anúncio e status acessível quando aberto', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => [],
    } as Response);

    render(<MonitoringTimelineDrawer isOpen={true} item={mockItem} onClose={() => {}} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Monitor Gamer 27 Polegadas 165Hz IPS')).toBeInTheDocument();
    expect(screen.getByText(/MLB987654321/)).toBeInTheDocument();
    expect(screen.getByText('Oficial Tech Store')).toBeInTheDocument();
  });

  it('3. Deve exibir estado vazio "Aguardando Primeira Coleta Efetiva" quando o item não tiver snapshots', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => [],
    } as Response);

    render(<MonitoringTimelineDrawer isOpen={true} item={mockItem} onClose={() => {}} />);

    await waitFor(() => {
      expect(screen.getByText('Aguardando Primeira Coleta Efetiva')).toBeInTheDocument();
      expect(screen.getByText(/O histórico inicia formalmente após o primeiro snapshot coletado/)).toBeInTheDocument();
    });
  });

  it('4. Deve renderizar snapshots com preço, data e variação em relação ao ponto anterior', async () => {
    const mockSnapshots = [
      {
        id: 'snap-2',
        tenantId: 'tenant-1',
        itemId: 'item-123',
        groupId: 'group-456',
        marketplace: 'MERCADO_LIVRE',
        externalListingId: 'MLB987654321',
        price: 1399.0,
        currency: 'BRL',
        isAvailable: true,
        provenanceType: 'OFFICIAL',
        collectedAt: '2026-11-18T12:00:00Z',
        observedAt: '2026-11-18T12:00:00Z',
        createdAt: '2026-11-18T12:00:00Z',
      },
      {
        id: 'snap-1',
        tenantId: 'tenant-1',
        itemId: 'item-123',
        groupId: 'group-456',
        marketplace: 'MERCADO_LIVRE',
        externalListingId: 'MLB987654321',
        price: 1499.0,
        currency: 'BRL',
        isAvailable: true,
        provenanceType: 'OFFICIAL',
        collectedAt: '2026-11-18T11:00:00Z',
        observedAt: '2026-11-18T11:00:00Z',
        createdAt: '2026-11-18T11:00:00Z',
      },
    ];

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockSnapshots,
    } as Response);

    render(<MonitoringTimelineDrawer isOpen={true} item={mockItem} onClose={() => {}} />);

    await waitFor(() => {
      expect(screen.getAllByText(/R\$\s*1\.399,00/).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/R\$\s*1\.499,00/).length).toBeGreaterThan(0);
      // Variação de queda: R$ -100,00 (-6.7%)
      expect(screen.getByText(/▼ R\$\s*100,00/)).toBeInTheDocument();
    });
  });

  it('5. Deve sinalizar explicitamente gap temporal quando intervalo entre coletas exceder o esperado', async () => {
    // Snapshots com 24 horas de diferença (intervalo configurado é 60 minutos -> threshold = 120min)
    const mockSnapshotsWithGap = [
      {
        id: 'snap-b',
        tenantId: 'tenant-1',
        itemId: 'item-123',
        groupId: 'group-456',
        marketplace: 'MERCADO_LIVRE',
        externalListingId: 'MLB987654321',
        price: 1450.0,
        currency: 'BRL',
        isAvailable: true,
        provenanceType: 'OFFICIAL',
        collectedAt: '2026-11-18T18:00:00Z',
        observedAt: '2026-11-18T18:00:00Z',
        createdAt: '2026-11-18T18:00:00Z',
      },
      {
        id: 'snap-a',
        tenantId: 'tenant-1',
        itemId: 'item-123',
        groupId: 'group-456',
        marketplace: 'MERCADO_LIVRE',
        externalListingId: 'MLB987654321',
        price: 1450.0,
        currency: 'BRL',
        isAvailable: true,
        provenanceType: 'OFFICIAL',
        collectedAt: '2026-11-17T10:00:00Z', // Quase 32 horas antes
        observedAt: '2026-11-17T10:00:00Z',
        createdAt: '2026-11-17T10:00:00Z',
      },
    ];

    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => mockSnapshotsWithGap,
    } as Response);

    render(<MonitoringTimelineDrawer isOpen={true} item={mockItem} onClose={() => {}} />);

    await waitFor(() => {
      expect(screen.getByText(/Gap de Coleta Detectado/)).toBeInTheDocument();
      expect(screen.getByText(/A plataforma não inventa pontos intermediários/)).toBeInTheDocument();
    });
  });

  it('6. Deve permitir disparar coleta manual e exibir feedback de sucesso ou janela mínima', async () => {
    vi.spyOn(global, 'fetch')
      // 1º fetch: carregar lista inicial
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      } as Response)
      // 2º fetch: trigger manual
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          status: 'SKIPPED',
          skipReason: 'SKIPPED_WINDOW_MINIMA',
          executedAt: '2026-11-18T12:05:00Z',
        }),
      } as Response);

    render(<MonitoringTimelineDrawer isOpen={true} item={mockItem} onClose={() => {}} />);

    const triggerBtn = screen.getByRole('button', { name: /Coletar Agora/i });
    fireEvent.click(triggerBtn);

    await waitFor(() => {
      expect(screen.getByText(/janela mínima de intervalo ainda não decorrida/i)).toBeInTheDocument();
    });
  });

  it('7. Deve fechar a gaveta ao pressionar a tecla Escape ou clicar no botão fechar', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => [],
    } as Response);

    const handleClose = vi.fn();
    render(<MonitoringTimelineDrawer isOpen={true} item={mockItem} onClose={handleClose} />);

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(handleClose).toHaveBeenCalledTimes(1);

    const closeBtn = screen.getByRole('button', { name: /Fechar timeline/i });
    fireEvent.click(closeBtn);
    expect(handleClose).toHaveBeenCalledTimes(2);
  });
});
