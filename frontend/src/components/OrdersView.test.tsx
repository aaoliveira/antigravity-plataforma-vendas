import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { OrdersView } from './OrdersView';
import { AuthProvider } from '@/context/AuthContext';

const mockOrdersData = {
  content: [
    {
      id: 'ord-1',
      tenantId: '11111111-1111-1111-1111-111111111111',
      sellerAccountId: 'acc-1',
      marketplace: 'MERCADO_LIVRE',
      externalOrderId: 'MLB-ORD-1001',
      status: 'PAID',
      subtotal: 299.9,
      shippingCost: 20.0,
      totalAmount: 319.9,
      currency: 'BRL',
      buyerName: 'João Silva',
      buyerDocument: '***.123.456-**',
      trackingNumber: 'BR123456789XP',
      cancellationReason: null,
      cancelledAt: null,
      returnedAt: null,
      rawPayloadJson: '{"id":"MLB-ORD-1001"}',
      schemaVersion: 'v1',
      origin: 'MERCADO_LIVRE_ORDERS_API',
      orderCreatedAt: '2026-11-18T10:00:00Z',
      syncedAt: '2026-11-18T12:00:00Z',
      items: [
        {
          id: 'item-1',
          externalItemId: 'ITM-01',
          externalListingId: 'MLB-99901',
          sku: 'SKU-HEADSET-01',
          title: 'Headset Gamer 7.1 Surround',
          unitPrice: 299.9,
          quantity: 1,
          totalItemPrice: 299.9,
        },
      ],
    },
    {
      id: 'ord-2',
      tenantId: '11111111-1111-1111-1111-111111111111',
      sellerAccountId: 'acc-2',
      marketplace: 'SHOPEE',
      externalOrderId: 'SHP-ORD-2002',
      status: 'SHIPPED',
      subtotal: 150.0,
      shippingCost: 0.0,
      totalAmount: 150.0,
      currency: 'BRL',
      buyerName: null, // Comprador ausente -> Incompleto!
      buyerDocument: null,
      trackingNumber: null, // Sem rastreio em status SHIPPED -> Incompleto!
      cancellationReason: null,
      cancelledAt: null,
      returnedAt: null,
      rawPayloadJson: '{"id":"SHP-ORD-2002"}',
      schemaVersion: 'v1',
      origin: 'SHOPEE_OPENAPI_V2',
      orderCreatedAt: '2026-11-18T11:00:00Z',
      syncedAt: '2026-11-18T12:15:00Z',
      items: [
        {
          id: 'item-2',
          externalItemId: 'ITM-02',
          externalListingId: 'SHP-88802',
          sku: null, // Sem SKU
          title: 'Mousepad Extra Grande Speed',
          unitPrice: 75.0,
          quantity: 2,
          totalItemPrice: 150.0,
        },
      ],
    },
  ],
  page: 0,
  size: 10,
  totalElements: 2,
  totalPages: 1,
  last: true,
};

const mockAccountsData = [
  {
    id: 'acc-1',
    marketplace: 'MERCADO_LIVRE',
    externalAccountId: 'SELLER_MELI_01',
    accountName: 'Loja Oficial Mercado Livre',
    status: 'ACTIVE',
  },
  {
    id: 'acc-2',
    marketplace: 'SHOPEE',
    externalAccountId: 'SHOP_SHOPEE_01',
    accountName: 'Loja Shopee Alpha',
    status: 'ACTIVE',
  },
];

const mockCheckpointsData = [
  {
    id: 'cp-1',
    sellerAccountId: 'acc-1',
    marketplace: 'MERCADO_LIVRE',
    syncStatus: 'COMPLETED',
    cursorToken: 'cursor-token-meli',
    lastOffset: 10,
    pageSize: 50,
    totalOrdersSynced: 120,
    startedAt: '2026-11-18T12:00:00Z',
    lastCheckpointAt: '2026-11-18T12:30:00Z',
    completedAt: '2026-11-18T12:30:00Z',
    errorMessage: null,
  },
];

describe('P008-FE: OrdersView and OrderDetailDrawer', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (url.includes('/api/catalog/seller-accounts')) {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => mockAccountsData,
          });
        }
        if (url.includes('/api/orders/checkpoints')) {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => mockCheckpointsData,
          });
        }
        if (url.includes('/api/orders')) {
          return Promise.resolve({
            ok: true,
            status: 200,
            json: async () => mockOrdersData,
          });
        }
        return Promise.reject(new Error(`Unhandled route: ${url}`));
      })
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderComponent = () => {
    return render(
      <AuthProvider>
        <OrdersView />
      </AuthProvider>
    );
  };

  it('1. Renderiza cabeçalho, contadores e lista com totais conciliados', async () => {
    renderComponent();

    expect(await screen.findByText('Pedidos de Venda & Reconciliação')).toBeDefined();

    // KPI Cards
    expect(screen.getByText('Total de Pedidos')).toBeDefined();
    expect(screen.getByText('Faturamento na Página')).toBeDefined();
    expect(screen.getByText('Frete Consolidado')).toBeDefined();
    expect(screen.getByText('Pedidos Incompletos')).toBeDefined();

    await waitFor(() => {
      expect(screen.getAllByText('MLB-ORD-1001').length).toBeGreaterThan(0);
      expect(screen.getAllByText('SHP-ORD-2002').length).toBeGreaterThan(0);
    });

    // Status badges
    expect(screen.getAllByText('Pago').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Enviado').length).toBeGreaterThan(0);
  });

  it('2. Exibe o status da sincronização a partir do checkpoint oficial', async () => {
    renderComponent();

    expect(await screen.findByText(/Sincronização: Concluída/)).toBeDefined();
    expect(screen.getByText(/120 pedidos sincronizados/)).toBeDefined();
  });

  it('3. Dispara busca e filtros combináveis para a API com parâmetros corretos', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('MLB-ORD-1001').length).toBeGreaterThan(0);
    });

    const searchInput = screen.getByPlaceholderText(/ID do pedido, comprador ou rastreio/);
    fireEvent.change(searchInput, { target: { value: 'MLB-ORD' } });

    const channelSelect = screen.getByLabelText('Canal');
    fireEvent.change(channelSelect, { target: { value: 'MERCADO_LIVRE' } });

    const statusSelect = screen.getByLabelText('Status');
    fireEvent.change(statusSelect, { target: { value: 'PAID' } });

    const filterButton = screen.getByRole('button', { name: 'Filtrar' });
    fireEvent.click(filterButton);

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('marketplace=MERCADO_LIVRE')
      );
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('status=PAID')
      );
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('search=MLB-ORD')
      );
    });
  });

  it('4. Botão "Limpar" redefine todos os filtros e restabelece a consulta padrão', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('MLB-ORD-1001').length).toBeGreaterThan(0);
    });

    const searchInput = screen.getByPlaceholderText(/ID do pedido, comprador ou rastreio/) as HTMLInputElement;
    fireEvent.change(searchInput, { target: { value: 'Teste' } });

    const clearButton = screen.getByRole('button', { name: 'Limpar' });
    fireEvent.click(clearButton);

    expect(searchInput.value).toBe('');

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        expect.not.stringContaining('search=')
      );
    });
  });

  it('5. Exibe loading skeleton acessível com role="status" durante requisições', () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(
        () =>
          new Promise((resolve) => {
            setTimeout(
              () =>
                resolve({
                  ok: true,
                  status: 200,
                  json: async () => mockOrdersData,
                }),
              500
            );
          })
      )
    );

    renderComponent();
    expect(screen.getByRole('status')).toBeDefined();
    expect(screen.getByText(/Carregando pedidos de venda/)).toBeDefined();
  });

  it('6. Exibe EmptyState com role="region" quando nenhum pedido for encontrado', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (url.includes('/api/orders/checkpoints')) {
          return Promise.resolve({ ok: true, status: 200, json: async () => [] });
        }
        if (url.includes('/api/catalog/seller-accounts')) {
          return Promise.resolve({ ok: true, status: 200, json: async () => [] });
        }
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({
            content: [],
            page: 0,
            size: 10,
            totalElements: 0,
            totalPages: 0,
            last: true,
          }),
        });
      })
    );

    renderComponent();
    expect(await screen.findByText('Nenhum pedido encontrado')).toBeDefined();
    expect(screen.getByRole('region')).toBeDefined();
  });

  it('7. Trata erros 401/403/500 de forma sanitizada sem vazar dados sensíveis', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (url.includes('/api/orders/checkpoints') || url.includes('/api/catalog/seller-accounts')) {
          return Promise.resolve({ ok: true, status: 200, json: async () => [] });
        }
        return Promise.resolve({
          ok: false,
          status: 403,
          json: async () => ({ error: 'FORBIDDEN', message: 'Acesso negado' }),
        });
      })
    );

    renderComponent();
    expect(await screen.findByRole('alert')).toBeDefined();
    expect(screen.getByText(/Acesso negado: você não possui permissão/)).toBeDefined();
  });

  it('8. Sinaliza pedidos incompletos com badges visíveis de alerta', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('MLB-ORD-1001').length).toBeGreaterThan(0);
    });

    // Pedido 2 está incompleto (sem rastreio e sem comprador)
    expect(screen.getAllByText('Incompleto').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Sem rastreio').length).toBeGreaterThan(0);
  });

  it('9. Abre drawer de detalhes ao clicar em "Ver Detalhes" e fecha com Escape', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('MLB-ORD-1001').length).toBeGreaterThan(0);
    });

    const detailButtons = screen.getAllByRole('button', { name: 'Ver Detalhes' });
    fireEvent.click(detailButtons[0]);

    // Drawer aberto
    expect(await screen.findByRole('dialog')).toBeDefined();
    expect(screen.getByText(/Pedido MLB-ORD-1001/)).toBeDefined();
    expect(screen.getByText('Headset Gamer 7.1 Surround')).toBeDefined();
    expect(screen.getByText('SKU-HEADSET-01')).toBeDefined();
    expect(screen.getAllByText('João Silva').length).toBeGreaterThan(0);

    // Fecha com Escape
    fireEvent.keyDown(window, { key: 'Escape' });
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).toBeNull();
    });
  });

  it('10. Suporta navegação de paginação server-side com touch targets adequados', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (url.includes('/api/orders/checkpoints') || url.includes('/api/catalog/seller-accounts')) {
          return Promise.resolve({ ok: true, status: 200, json: async () => [] });
        }
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({
            content: mockOrdersData.content,
            page: 0,
            size: 10,
            totalElements: 25,
            totalPages: 3,
            last: false,
          }),
        });
      })
    );

    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('MLB-ORD-1001').length).toBeGreaterThan(0);
    });

    const nextButton = screen.getByRole('button', { name: 'Próxima' });
    expect(nextButton).toBeDefined();
    expect(nextButton.className).toContain('min-h-[44px]');

    fireEvent.click(nextButton);

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('page=1')
      );
    });
  });
});
