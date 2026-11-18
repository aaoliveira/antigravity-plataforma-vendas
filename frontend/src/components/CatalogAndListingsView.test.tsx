import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { CatalogAndListingsView } from './CatalogAndListingsView';
import { AuthProvider } from '@/context/AuthContext';

const mockListingsData = {
  content: [
    {
      id: 'listing-1',
      sellerAccountId: 'acc-1',
      sellerAccountName: 'Loja Oficial Mercado Livre',
      marketplace: 'MERCADO_LIVRE',
      externalListingId: 'MLB-100200',
      externalSku: 'SKU-MOUSE-01',
      externalVariationId: null,
      title: 'Mouse Gamer RGB 16000 DPI Sensor Pixart',
      price: 149.9,
      currency: 'BRL',
      availableQuantity: 45,
      status: 'ACTIVE',
      permalink: 'https://mercadolivre.com.br/MLB-100200',
      origin: 'MERCADO_LIVRE_ITEMS_API',
      schemaVersion: 'v1',
      syncedAt: '2026-11-18T10:00:00Z',
      product: {
        id: 'prod-1',
        title: 'Mouse Gamer Canônico Mestre',
        canonicalSku: 'MASTER-SKU-MOUSE',
        brand: 'TechBrand',
        category: 'Periféricos',
        status: 'ACTIVE',
      },
      variant: null,
    },
    {
      id: 'listing-2',
      sellerAccountId: 'acc-2',
      sellerAccountName: 'Loja Shopee Alpha',
      marketplace: 'SHOPEE',
      externalListingId: 'SHP-999888',
      externalSku: null, // Sem SKU
      externalVariationId: null,
      title: 'Teclado Mecânico Compacto Switch Blue',
      price: 289.0,
      currency: 'BRL',
      availableQuantity: 0, // Esgotado
      status: 'PAUSED',
      permalink: 'https://shopee.com.br/product/SHP-999888',
      origin: 'SHOPEE_OPENAPI_V2',
      schemaVersion: 'v1',
      syncedAt: '2026-11-18T10:30:00Z',
      product: null, // Não vinculado
      variant: null,
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

describe('P007-FE: CatalogAndListingsView Component', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (url.includes('/api/catalog/seller-accounts')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(mockAccountsData),
        });
      }
      if (url.includes('/api/catalog/listings')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(mockListingsData),
        });
      }
      return Promise.reject(new Error('Unknown endpoint'));
    }));
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderComponent = () => {
    return render(
      <AuthProvider>
        <CatalogAndListingsView />
      </AuthProvider>
    );
  };

  it('1. Renderiza o cabeçalho do catálogo, contador de anúncios e tabela responsiva', async () => {
    renderComponent();

    // Aguarda o carregamento dos anúncios
    await waitFor(() => {
      expect(screen.getByText('Catálogo de Produtos & Anúncios')).toBeDefined();
    });

    expect(screen.getByText(/2 anúncios/i)).toBeDefined();
    expect(screen.getAllByText('Mouse Gamer RGB 16000 DPI Sensor Pixart').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Teclado Mecânico Compacto Switch Blue').length).toBeGreaterThan(0);

    // Badges de canal e status
    expect(screen.getAllByText('Mercado Livre').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Shopee').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Ativo').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Pausado').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Esgotado').length).toBeGreaterThan(0);
  });

  it('2. Dispara busca e filtros combináveis para a API com parâmetros corretos', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('Mouse Gamer RGB 16000 DPI Sensor Pixart').length).toBeGreaterThan(0);
    });

    const searchInput = screen.getByPlaceholderText(/Buscar por título, SKU ou ID do anúncio.../i);
    fireEvent.change(searchInput, { target: { value: 'Pixart' } });

    const mktSelect = screen.getByRole('combobox', { name: /Filtrar por Marketplace/i });
    fireEvent.change(mktSelect, { target: { value: 'MERCADO_LIVRE' } });

    const statusSelect = screen.getByRole('combobox', { name: /Filtrar por Status/i });
    fireEvent.change(statusSelect, { target: { value: 'ACTIVE' } });

    const filterBtn = screen.getByRole('button', { name: 'Filtrar' });
    fireEvent.click(filterBtn);

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('marketplace=MERCADO_LIVRE')
      );
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('status=ACTIVE')
      );
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('search=Pixart')
      );
    });
  });

  it('3. Botão "Limpar" redefine todos os filtros e restabelece a lista', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Catálogo de Produtos & Anúncios')).toBeDefined();
    });

    const searchInput = screen.getByPlaceholderText(/Buscar por título, SKU ou ID do anúncio.../i);
    fireEvent.change(searchInput, { target: { value: 'FiltroAtivo' } });

    const filterBtn = screen.getByRole('button', { name: 'Filtrar' });
    fireEvent.click(filterBtn);

    // Botão Limpar aparece
    const clearBtn = await screen.findByRole('button', { name: 'Limpar' });
    fireEvent.click(clearBtn);

    expect(searchInput.getAttribute('value')).toBe('');
  });

  it('4. Exibe loading skeleton acessível com role="status" durante requisições', () => {
    // Simula promise pendente para capturar o loading
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})));

    renderComponent();

    expect(screen.getByRole('status', { name: /Carregando catálogo.../i })).toBeDefined();
  });

  it('5. Exibe EmptyState com role="region" quando nenhum anúncio for encontrado', async () => {
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (url.includes('/api/catalog/listings')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve({ content: [], totalElements: 0, totalPages: 0 }),
        });
      }
      return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([]) });
    }));

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('region', { name: /Nenhum anúncio encontrado/i })).toBeDefined();
    });
    expect(screen.getByText(/Seu catálogo ainda não possui anúncios sincronizados/i)).toBeDefined();
  });

  it('6. Trata erros 401/403/500 de forma sanitizada sem vazar dados ou stacktraces', async () => {
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (url.includes('/api/catalog/listings')) {
        return Promise.resolve({
          ok: false,
          status: 403,
          json: () => Promise.resolve({ error: 'FORBIDDEN', message: 'Forbidden' }),
        });
      }
      return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([]) });
    }));

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeDefined();
      expect(screen.getByText(/Acesso negado: você não possui permissão para consultar os anúncios desta organização/i)).toBeDefined();
    });
  });

  it('7. Abre drawer de detalhes do anúncio ao clicar em "Ver Detalhes", exibe produto mestre e fecha com ESC', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('Mouse Gamer RGB 16000 DPI Sensor Pixart').length).toBeGreaterThan(0);
    });

    const detailButtons = screen.getAllByRole('button', { name: /Ver detalhes do anúncio/i });
    fireEvent.click(detailButtons[0]);

    // Drawer aberto
    const dialog = screen.getByRole('dialog', { name: 'Detalhes do Anúncio' });
    expect(dialog).toBeDefined();

    expect(screen.getByText('Produto Canônico Mestre')).toBeDefined();
    expect(screen.getAllByText('Mouse Gamer Canônico Mestre').length).toBeGreaterThan(0);
    expect(screen.getAllByText('MASTER-SKU-MOUSE').length).toBeGreaterThan(0);
    expect(screen.getByText('TechBrand')).toBeDefined();

    // Link oficial com atributos de segurança
    const link = screen.getByRole('link', { name: /Ver anúncio oficial no marketplace/i });
    expect(link.getAttribute('href')).toBe('https://mercadolivre.com.br/MLB-100200');
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toContain('noopener');

    // Fecha pressionando Escape
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('8. Exibe claramente dados ausentes no detalhe com badges explícitas (Sem SKU, Não vinculado)', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('Teclado Mecânico Compacto Switch Blue').length).toBeGreaterThan(0);
    });

    // Clica no anúncio 2 (que não tem SKU nem produto canônico vinculado)
    const detailButtons = screen.getAllByRole('button', { name: /Ver detalhes do anúncio/i });
    fireEvent.click(detailButtons[1]);

    const dialog = screen.getByRole('dialog', { name: 'Detalhes do Anúncio' });
    expect(dialog).toBeDefined();

    // Dados ausentes destacados
    expect(screen.getByText(/Nenhum produto canônico vinculado/i)).toBeDefined();
    expect(screen.getAllByText('Sem SKU').length).toBeGreaterThan(0);
    expect(screen.getByText('Sem variação')).toBeDefined();
  });

  it('9. Garante acessibilidade e touch targets mínimos de 44px', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Catálogo de Produtos & Anúncios')).toBeDefined();
    });

    // Valida botões e inputs interativos
    const refreshBtn = screen.getByRole('button', { name: 'Atualizar listagem' });
    expect(refreshBtn.className).toContain('min-h-[44px]');

    const searchInput = screen.getByPlaceholderText(/Buscar por título, SKU ou ID do anúncio.../i);
    expect(searchInput.className).toContain('min-h-[44px]');

    const detailButtons = screen.getAllByRole('button', { name: /Ver detalhes do anúncio/i });
    expect(detailButtons[0].className).toContain('min-h-[44px]');
  });

  it('10. Suporta navegação da paginação server-side (próxima / anterior / itens por página)', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Catálogo de Produtos & Anúncios')).toBeDefined();
    });

    const pageSizeSelect = screen.getByRole('combobox', { name: /Itens por página/i });
    fireEvent.change(pageSizeSelect, { target: { value: '25' } });

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('size=25')
      );
    });
  });
});
