import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ProductMonitoringView } from './ProductMonitoringView';

describe('ProductMonitoringView Component (P012-FE)', () => {
  const mockGroups = [
    {
      id: 'grp-1',
      tenantId: 'tenant-111',
      name: 'Monitores e Displays Gamers',
      description: 'Concorrentes diretos de monitores 144Hz+',
      color: '#3B82F6',
      minIntervalMinutes: 60,
      retentionDays: 90,
      status: 'ACTIVE' as const,
      itemCount: 2,
      createdAt: '2026-11-18T10:00:00Z',
      updatedAt: '2026-11-18T10:00:00Z',
    },
    {
      id: 'grp-2',
      tenantId: 'tenant-111',
      name: 'Fones TWS Bluetooth',
      description: 'Fones sem fio cancelamento de ruído',
      color: '#10B981',
      minIntervalMinutes: 120,
      retentionDays: 60,
      status: 'PAUSED' as const,
      itemCount: 0,
      createdAt: '2026-11-18T11:00:00Z',
      updatedAt: '2026-11-18T11:00:00Z',
    },
  ];

  const mockItems = [
    {
      id: 'item-1',
      tenantId: 'tenant-111',
      groupId: 'grp-1',
      marketplace: 'MERCADO_LIVRE',
      externalListingId: 'MLB111222333',
      title: 'Monitor 24 Polegadas 144Hz Full HD',
      sellerName: 'Vendedor Alpha',
      sourceUrl: 'https://produto.mercadolivre.com.br/MLB-111222333',
      lastPrice: 899.9,
      currency: 'BRL',
      status: 'ACTIVE' as const,
      firstSnapshotAt: '2026-11-18T10:30:00Z',
      lastSnapshotAt: '2026-11-18T12:00:00Z',
      createdAt: '2026-11-18T10:00:00Z',
      updatedAt: '2026-11-18T12:00:00Z',
    },
    {
      id: 'item-2',
      tenantId: 'tenant-111',
      groupId: 'grp-1',
      marketplace: 'SHOPEE',
      externalListingId: '222333444',
      title: 'Monitor Curvo 27 Polegadas 165Hz',
      sellerName: 'Loja Shopee Tech',
      sourceUrl: 'https://shopee.com.br/product-222333444',
      lastPrice: 1299.0,
      currency: 'BRL',
      status: 'ACTIVE' as const,
      firstSnapshotAt: null, // Aguardando 1ª coleta
      lastSnapshotAt: null,
      createdAt: '2026-11-18T11:45:00Z',
      updatedAt: '2026-11-18T11:45:00Z',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('1. Deve renderizar o título do módulo, cartões de KPI e barra de controles', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({ items: mockGroups, totalElements: 2 }),
    } as Response);

    render(<ProductMonitoringView />);

    expect(screen.getByRole('heading', { name: /Monitoramento de Produtos & Concorrentes/i })).toBeInTheDocument();
    expect(screen.getByText('Total de Grupos')).toBeInTheDocument();
    expect(screen.getByText('Grupos Ativos')).toBeInTheDocument();
    expect(screen.getByText('Anti-Retroatividade')).toBeInTheDocument();
  });

  it('2. Deve exibir estado vazio quando o tenant não possuir grupos cadastrados', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({ items: [], totalElements: 0 }),
    } as Response);

    render(<ProductMonitoringView />);

    await waitFor(() => {
      expect(screen.getByText('Nenhum grupo de monitoramento encontrado')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Criar Primeiro Grupo' })).toBeInTheDocument();
    });
  });

  it('3. Deve listar os grupos cadastrados com nome, cor, intervalo e retenção', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({ items: mockGroups, totalElements: 2 }),
    } as Response);

    render(<ProductMonitoringView />);

    await waitFor(() => {
      expect(screen.getByText('Monitores e Displays Gamers')).toBeInTheDocument();
      expect(screen.getByText('Fones TWS Bluetooth')).toBeInTheDocument();
      expect(screen.getByText('60 min')).toBeInTheDocument();
      expect(screen.getByText('90 dias')).toBeInTheDocument();
    });
  });

  it('4. Deve filtrar grupos por status (Ativos / Pausados)', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValueOnce({
      ok: true,
      json: async () => ({ items: mockGroups, totalElements: 2 }),
    } as Response);

    render(<ProductMonitoringView />);

    await waitFor(() => {
      expect(screen.getByText('Monitores e Displays Gamers')).toBeInTheDocument();
      expect(screen.getByText('Fones TWS Bluetooth')).toBeInTheDocument();
    });

    // Clica no filtro "Pausados"
    const pausedFilterBtn = screen.getByRole('button', { name: 'Pausados' });
    fireEvent.click(pausedFilterBtn);

    // Apenas o grupo pausado deve ser exibido
    expect(screen.queryByText('Monitores e Displays Gamers')).not.toBeInTheDocument();
    expect(screen.getByText('Fones TWS Bluetooth')).toBeInTheDocument();
  });

  it('5. Deve abrir os detalhes do grupo e listar os itens monitorados', async () => {
    vi.spyOn(global, 'fetch')
      // 1º fetch: listar grupos
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockGroups, totalElements: 2 }),
      } as Response)
      // 2º fetch: listar itens do grupo 1
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockItems, totalElements: 2 }),
      } as Response);

    render(<ProductMonitoringView />);

    await waitFor(() => {
      expect(screen.getByText('Monitores e Displays Gamers')).toBeInTheDocument();
    });

    // Clica no botão "Abrir" do primeiro grupo
    const openBtns = screen.getAllByRole('button', { name: /Abrir/i });
    fireEvent.click(openBtns[0]);

    await waitFor(() => {
      // Exibe cabeçalho do grupo e botão de voltar
      expect(screen.getByRole('button', { name: /Todos os Grupos/i })).toBeInTheDocument();
      // Exibe itens monitorados
      expect(screen.getByText('Monitor 24 Polegadas 144Hz Full HD')).toBeInTheDocument();
      expect(screen.getByText('Monitor Curvo 27 Polegadas 165Hz')).toBeInTheDocument();
      // Item sem snapshot exibe "Aguardando 1ª coleta"
      expect(screen.getByText('Aguardando 1ª coleta')).toBeInTheDocument();
    });
  });

  it('6. Deve alternar entre visualização em Tabela e Cards no detalhe do grupo', async () => {
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockGroups, totalElements: 2 }),
      } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockItems, totalElements: 2 }),
      } as Response);

    render(<ProductMonitoringView />);

    await waitFor(() => {
      const openBtns = screen.getAllByRole('button', { name: /Abrir/i });
      fireEvent.click(openBtns[0]);
    });

    await waitFor(() => {
      expect(screen.getByRole('table')).toBeInTheDocument();
    });

    // Alterna para visualização em Cards
    const cardsViewBtn = screen.getByRole('button', { name: /Visualização em cards/i });
    fireEvent.click(cardsViewBtn);

    // Tabela não deve estar presente no DOM
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    // Cards com títulos devem estar visíveis
    expect(screen.getByText('Monitor 24 Polegadas 144Hz Full HD')).toBeInTheDocument();
  });

  it('7. Deve exibir modal de confirmação de exclusão com aviso de retenção legal', async () => {
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockGroups, totalElements: 2 }),
      } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockItems, totalElements: 2 }),
      } as Response);

    render(<ProductMonitoringView />);

    await waitFor(() => {
      const openBtns = screen.getAllByRole('button', { name: /Abrir/i });
      fireEvent.click(openBtns[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('Monitor 24 Polegadas 144Hz Full HD')).toBeInTheDocument();
    });

    // Clica no botão de remover item
    const deleteBtns = screen.getAllByTitle('Remover do monitoramento');
    fireEvent.click(deleteBtns[0]);

    // Modal de confirmação deve abrir com aviso de retenção
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Remover Anúncio/i)).toBeInTheDocument();
    expect(screen.getByText(/Política de Retenção & Agendamento/i)).toBeInTheDocument();
    expect(screen.getByText(/permanecem auditáveis durante a janela de retenção/i)).toBeInTheDocument();
  });

  it('8. Deve abrir o drawer de timeline ao clicar no botão Timeline', async () => {
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockGroups, totalElements: 2 }),
      } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockItems, totalElements: 2 }),
      } as Response)
      // fetch do drawer
      .mockResolvedValueOnce({
        ok: true,
        json: async () => [],
      } as Response);

    render(<ProductMonitoringView />);

    await waitFor(() => {
      const openBtns = screen.getAllByRole('button', { name: /Abrir/i });
      fireEvent.click(openBtns[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('Monitor 24 Polegadas 144Hz Full HD')).toBeInTheDocument();
    });

    // Clica em Timeline do item
    const timelineBtns = screen.getAllByRole('button', { name: /Timeline/i });
    fireEvent.click(timelineBtns[0]);

    // Drawer de timeline deve estar presente com dialog
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /Linha do Tempo de Preços & Observações/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Coletar Agora/i })).toBeInTheDocument();
    });
  });

  it('9. Deve abrir o modal de adição de item ao clicar em "Adicionar Anúncio"', async () => {
    vi.spyOn(global, 'fetch')
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockGroups, totalElements: 2 }),
      } as Response)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ items: mockItems, totalElements: 2 }),
      } as Response);

    render(<ProductMonitoringView />);

    await waitFor(() => {
      const openBtns = screen.getAllByRole('button', { name: /Abrir/i });
      fireEvent.click(openBtns[0]);
    });

    const addBtn = screen.getByRole('button', { name: /Adicionar Anúncio/i });
    fireEvent.click(addBtn);

    // Modal de adição deve estar aberto com as abas de pesquisa e inserção direta
    expect(screen.getByRole('heading', { name: 'Adicionar Anúncio ao Monitoramento' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Pesquisar no Mercado/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Inserção Direta de Anúncio/i })).toBeInTheDocument();
  });
});
